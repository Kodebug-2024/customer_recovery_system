import LegalFooter from "@/components/LegalFooter";

export const metadata = { title: "Privacy Policy · Codezilla CRM" };

export default function PrivacyPage() {
  return (
    <div className="mx-auto max-w-3xl px-6 py-12 prose prose-sm">
      <h1>Privacy Policy</h1>
      <p className="text-muted-foreground">
        Last updated: {new Date().toISOString().slice(0, 10)}
      </p>

      <p>
        <strong>[Company Legal Name]</strong> ("we", "us") operates the
        Codezilla CRM platform. This policy explains what we collect, why, and
        your rights.
      </p>

      <h2>1. Data we collect</h2>
      <ul>
        <li>
          <strong>Account data</strong>: name, work email, hashed password,
          role, last login, optional 2FA secret (encrypted).
        </li>
        <li>
          <strong>Customer data you upload</strong>: leads, messages, notes,
          tags, appointments. You control what goes in.
        </li>
        <li>
          <strong>Integration credentials</strong>:
          WhatsApp/Telegram/OpenAI/Stripe tokens (encrypted at rest with
          AES-GCM-256).
        </li>
        <li>
          <strong>Operational data</strong>: audit logs, IP address on login,
          request timing metrics, error reports (with request bodies and cookies
          stripped).
        </li>
      </ul>

      <h2>2. Why we collect it</h2>
      <ul>
        <li>To provide the Service (contract performance).</li>
        <li>To prevent abuse and secure accounts (legitimate interest).</li>
        <li>To bill you for paid plans (contract performance).</li>
        <li>
          To send transactional email — verification, password reset, billing
          receipts (contract performance).
        </li>
      </ul>
      <p>
        We do <strong>not</strong> sell your data. We do not use your customer
        data to train any AI model.
      </p>

      <h2>3. Sub-processors</h2>
      <ul>
        <li>
          <strong>AWS</strong> — hosting, S3 backup storage.
        </li>
        <li>
          <strong>Stripe</strong> — payment processing (we never see your card
          number).
        </li>
        <li>
          <strong>Meta (WhatsApp Cloud API)</strong> — only when you enable
          WhatsApp.
        </li>
        <li>
          <strong>OpenAI</strong> — only when you enable AI replies; lead
          messages are sent to generate the reply and are not retained by us
          beyond the message log.
        </li>
        <li>
          <strong>Sentry</strong> — error tracking. Request bodies and cookies
          are scrubbed before sending.
        </li>
        <li>
          <strong>SMTP provider</strong> (e.g. Amazon SES) — for transactional
          email.
        </li>
      </ul>

      <h2>4. Cookies and similar storage</h2>
      <p>We use:</p>
      <ul>
        <li>
          <strong>localStorage</strong> for your authentication token and UI
          preferences (essential — required for login).
        </li>
        <li>
          <strong>localStorage</strong> for your cookie-consent choice itself.
        </li>
      </ul>
      <p>We do not use third-party advertising cookies.</p>

      <h2>5. Data retention</h2>
      <ul>
        <li>Active account data is kept while your account exists.</li>
        <li>
          Leads soft-deleted from the UI are permanently purged after 30 days.
        </li>
        <li>Backups are retained for 30 days by default, then rotated.</li>
        <li>Audit logs are kept for 12 months for security and compliance.</li>
      </ul>

      <h2>6. Your rights (GDPR / PDPA)</h2>
      <p>You can:</p>
      <ul>
        <li>
          <strong>Access</strong> your data — export leads as CSV from{" "}
          <code>/leads</code>, or email us for a full account export.
        </li>
        <li>
          <strong>Correct</strong> inaccurate data via the dashboard.
        </li>
        <li>
          <strong>Delete</strong> your account; we will purge it within 30 days.
        </li>
        <li>
          <strong>Restrict</strong> or <strong>object to</strong> certain
          processing — email us.
        </li>
        <li>
          <strong>Data portability</strong> — CSV exports + the public REST API
          at <code>/v1</code>.
        </li>
      </ul>

      <h2>7. International transfers</h2>
      <p>
        Your data is processed in our primary region (
        <strong>[AWS region]</strong>). When a sub-processor is located
        elsewhere, transfers rely on Standard Contractual Clauses or equivalent
        safeguards.
      </p>

      <h2>8. Security</h2>
      <p>
        TLS in transit, AES-GCM-256 for secrets at rest, bcrypt for passwords,
        rate-limited login with account lockout, optional TOTP 2FA, multi-tenant
        isolation verified by integration tests, nightly off-host backups, error
        monitoring.
      </p>

      <h2>9. Children</h2>
      <p>The Service is not intended for individuals under 16.</p>

      <h2>10. Contact</h2>
      <p>
        Privacy questions:{" "}
        <a href="mailto:privacy@codezilla.example">privacy@codezilla.example</a>
        . Data Protection Officer: <strong>[DPO name and address]</strong>.
      </p>

      <LegalFooter />
    </div>
  );
}
