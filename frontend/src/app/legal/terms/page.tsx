import LegalFooter from "@/components/LegalFooter";

export const metadata = { title: "Terms of Service · Codezilla CRM" };

export default function TermsPage() {
  return (
    <div className="mx-auto max-w-3xl px-6 py-12 prose prose-sm">
      <h1>Terms of Service</h1>
      <p className="text-muted-foreground">Last updated: {new Date().toISOString().slice(0, 10)}</p>

      <p>
        These Terms of Service ("Terms") govern your use of the Codezilla CRM platform
        (the "Service"), operated by <strong>[Company Legal Name]</strong> ("we", "us").
        By creating an account you agree to these Terms.
      </p>

      <h2>1. The Service</h2>
      <p>
        The Service is a multi-tenant customer-relationship management application for small
        and medium-sized businesses. We provide it on an "as is" basis and do not guarantee
        uninterrupted availability.
      </p>

      <h2>2. Your account</h2>
      <ul>
        <li>You must provide accurate registration information and keep your credentials secret.</li>
        <li>You are responsible for everything that happens under your account, including the
            actions of users you invite to your workspace.</li>
        <li>We may suspend accounts that violate these Terms or applicable law.</li>
      </ul>

      <h2>3. Your data</h2>
      <p>
        You retain ownership of all customer data you enter into the Service. You grant us a
        limited licence to store, process and back up that data solely in order to operate
        the Service for you. We will not sell your data.
      </p>

      <h2>4. Acceptable use</h2>
      <ul>
        <li>No unlawful, fraudulent, harmful, or abusive content.</li>
        <li>No sending of unsolicited messages ("spam") through any integration we offer.</li>
        <li>No reverse-engineering, scraping at unreasonable rates, or attempts to bypass
            security controls.</li>
      </ul>

      <h2>5. Subscription and payment</h2>
      <p>
        Paid plans are billed in advance and renew automatically. You can cancel any time
        from the Billing page; cancellations take effect at the end of the current period.
        We do not offer refunds for partial periods.
      </p>

      <h2>6. Third-party services</h2>
      <p>
        The Service integrates with third parties (Meta WhatsApp Cloud API, Telegram, OpenAI,
        Stripe, AWS, Sentry). Your use of those integrations is also subject to their
        respective terms.
      </p>

      <h2>7. Liability</h2>
      <p>
        To the maximum extent permitted by law, our aggregate liability for any claim arising
        out of the Service is limited to the fees you paid in the 12 months preceding the claim.
        We are not liable for indirect, incidental, or consequential damages.
      </p>

      <h2>8. Termination</h2>
      <p>
        You can delete your account at any time. We can terminate accounts for material
        breach with reasonable notice. On termination we will delete your data within 30 days,
        subject to legal retention obligations.
      </p>

      <h2>9. Changes</h2>
      <p>
        We may update these Terms occasionally. Material changes will be notified by email
        and/or an in-app banner at least 14 days in advance.
      </p>

      <h2>10. Governing law</h2>
      <p>
        These Terms are governed by the laws of <strong>[Jurisdiction]</strong>. Disputes will
        be resolved in the courts of <strong>[City, Jurisdiction]</strong>.
      </p>

      <h2>11. Contact</h2>
      <p>Questions? Email <a href="mailto:legal@codezilla.example">legal@codezilla.example</a>.</p>

      <LegalFooter />
    </div>
  );
}
