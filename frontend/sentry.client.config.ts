import * as Sentry from "@sentry/nextjs";

const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN;

if (dsn) {
  Sentry.init({
    dsn,
    environment: process.env.NEXT_PUBLIC_SENTRY_ENVIRONMENT || "dev",
    tracesSampleRate: 0.1,
    // Strip URLs that may contain tokens/IDs from breadcrumbs.
    beforeBreadcrumb(b) {
      if (b.data && typeof b.data.url === "string") {
        b.data.url = b.data.url.replace(/access_token=[^&]+/g, "access_token=***");
      }
      return b;
    },
  });
}
