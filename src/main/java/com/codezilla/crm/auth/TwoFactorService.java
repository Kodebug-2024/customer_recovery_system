package com.codezilla.crm.auth;

import com.codezilla.crm.security.SecretCipher;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * TOTP (RFC 6238) two-factor auth. Secrets are encrypted at rest using
 * SecretCipher (AES-GCM). Standard 6-digit, 30-second window; tolerates one
 * step of clock drift.
 */
@Service
public class TwoFactorService {

    private static final String ISSUER = "Codezilla CRM";

    private final SecretGenerator secrets = new DefaultSecretGenerator();
    private final QrGenerator qr = new ZxingPngQrGenerator();
    private final TimeProvider time = new SystemTimeProvider();
    private final CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), time);

    private final UserRepository users;
    private final SecretCipher cipher;

    public TwoFactorService(UserRepository users, SecretCipher cipher) {
        this.users = users;
        this.cipher = cipher;
    }

    public record EnrollData(String secret, String qrDataUrl) {}

    /** Generate a fresh secret and a `data:` PNG QR. Does NOT enable 2FA yet. */
    @Transactional
    public EnrollData beginEnrollment(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        String secret = secrets.generate();
        u.setTotpSecretEnc(cipher.encrypt(secret));
        u.setTotpEnabled(false);
        users.save(u);

        QrData qrData = new QrData.Builder()
                .label(u.getEmail())
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] png = qr.generate(qrData);
            String dataUrl = "data:" + qr.getImageMimeType() + ";base64," + Utils.getDataUriForImage(png, qr.getImageMimeType()).split(",", 2)[1];
            return new EnrollData(secret, dataUrl);
        } catch (Exception e) {
            throw new IllegalStateException("QR generation failed", e);
        }
    }

    /** Verify the first code from the user's authenticator and switch 2FA ON. */
    @Transactional
    public boolean confirmEnrollment(UUID userId, String code) {
        User u = users.findById(userId).orElseThrow();
        String secret = u.getTotpSecretEnc() == null ? null : cipher.decrypt(u.getTotpSecretEnc());
        if (secret == null) return false;
        if (!verifier.isValidCode(secret, code)) return false;
        u.setTotpEnabled(true);
        users.save(u);
        return true;
    }

    @Transactional
    public void disable(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        u.setTotpEnabled(false);
        u.setTotpSecretEnc(null);
        users.save(u);
    }

    /** Used during login if the user has 2FA enabled. */
    public boolean verifyLoginCode(User user, String code) {
        if (!user.isTotpEnabled() || user.getTotpSecretEnc() == null) return true;
        if (code == null || code.isBlank()) return false;
        String secret = cipher.decrypt(user.getTotpSecretEnc());
        return secret != null && verifier.isValidCode(secret, code);
    }
}
