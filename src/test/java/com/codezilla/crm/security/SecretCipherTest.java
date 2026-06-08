package com.codezilla.crm.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

    private final SecretCipher cipher = new SecretCipher(
            "", "test-secret-test-secret-test-secret-test-secret");

    @Test
    void roundTrip() {
        String plain = "EAAOYoyZAjVkQBR_super_secret_token_value_!@#";
        byte[] enc = cipher.encrypt(plain);
        assertThat(enc).isNotNull();
        assertThat(new String(enc)).doesNotContain(plain);
        assertThat(cipher.decrypt(enc)).isEqualTo(plain);
    }

    @Test
    void nullSafe() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt(new byte[0])).isNull();
    }

    @Test
    void tamperedCiphertextRejected() {
        byte[] enc = cipher.encrypt("secret");
        enc[enc.length - 1] ^= 0x01;
        assertThatThrownBy(() -> cipher.decrypt(enc)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void differentIvsProduceDifferentCiphertext() {
        byte[] a = cipher.encrypt("same");
        byte[] b = cipher.encrypt("same");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejectsBadKeyLength() {
        assertThatThrownBy(() -> new SecretCipher("Zm9v", "fallback"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
