"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";

const KEY = "crm.cookieConsent.v1";

/**
 * GDPR/PDPA-style consent banner. We only set 1 first-party cookie (the JWT)
 * and use localStorage for UI prefs, so the choices are coarse: "accept all"
 * vs "essential only". Selection is stored locally; analytics SDKs (if added
 * later) should read window.__crmConsent before initializing.
 */
export default function CookieBanner() {
  const [choice, setChoice] = useState<string | null>("loading");

  useEffect(() => {
    setChoice(typeof window === "undefined" ? null : localStorage.getItem(KEY));
  }, []);

  function decide(value: "all" | "essential") {
    localStorage.setItem(KEY, value);
    (window as any).__crmConsent = value;
    setChoice(value);
  }

  if (choice === "loading" || choice) return null;

  return (
    <div className="fixed inset-x-0 bottom-0 z-50 border-t bg-background/95 p-4 shadow-lg backdrop-blur">
      <div className="mx-auto flex max-w-4xl flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-muted-foreground">
          We use a small number of cookies to keep you logged in and to measure
          errors. See our{" "}
          <Link href="/legal/privacy" className="underline">
            Privacy Policy
          </Link>{" "}
          and{" "}
          <Link href="/legal/terms" className="underline">
            Terms
          </Link>
          .
        </p>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => decide("essential")}
          >
            Essential only
          </Button>
          <Button size="sm" onClick={() => decide("all")}>
            Accept all
          </Button>
        </div>
      </div>
    </div>
  );
}
