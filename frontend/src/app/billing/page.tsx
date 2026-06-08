"use client";
import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Check, Sparkles } from "lucide-react";

interface BillingView {
  plan: "FREE" | "PRO";
  status: string;
  cancelAtPeriodEnd: boolean;
  currentPeriodEnd: string | null;
  live: boolean;
  leadsUsedThisMonth: number;
  leadLimitThisMonth: number;
  aiAllowed: boolean;
}

interface PlansMap {
  free: { name: string; priceMonthly: number; limits: Record<string, unknown> };
  pro: { name: string; priceMonthly: number; limits: Record<string, unknown> };
}

export default function BillingPageOuter() {
  return (
    <Suspense fallback={<p className="text-muted-foreground">Loading…</p>}>
      <BillingPage />
    </Suspense>
  );
}

function BillingPage() {
  const params = useSearchParams();
  const [b, setB] = useState<BillingView | null>(null);
  const [plans, setPlans] = useState<PlansMap | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      const [bRes, pRes] = await Promise.all([
        api<BillingView>("/api/billing"),
        api<PlansMap>("/api/billing/plans"),
      ]);
      setB(bRes);
      setPlans(pRes);
    } catch (e) {
      setError((e as Error).message);
    }
  }
  useEffect(() => {
    load();
    const status = params.get("status");
    if (status === "success") setNotice("Subscription updated.");
    if (status === "cancel") setNotice("Checkout cancelled.");
  }, [params]);

  async function upgrade() {
    setBusy(true);
    setError(null);
    try {
      const r = await api<{ url: string }>("/api/billing/checkout", {
        method: "POST",
        body: JSON.stringify({ plan: "PRO" }),
      });
      window.location.href = r.url;
    } catch (e) {
      setError((e as Error).message);
      setBusy(false);
    }
  }

  async function manage() {
    setBusy(true);
    setError(null);
    try {
      const r = await api<{ url: string }>("/api/billing/portal", {
        method: "POST",
      });
      window.location.href = r.url;
    } catch (e) {
      setError((e as Error).message);
      setBusy(false);
    }
  }

  if (error) return <p className="text-destructive">{error}</p>;
  if (!b || !plans) return <p className="text-muted-foreground">Loading…</p>;

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Billing</h1>
        <p className="text-muted-foreground text-sm">
          Manage your subscription.{" "}
          {!b.live && (
            <Badge variant="warning" className="ml-2">
              Stub mode — Stripe not configured
            </Badge>
          )}
        </p>
      </div>

      {notice && (
        <div className="text-sm rounded-md border border-green-200 bg-green-50 text-green-800 px-3 py-2">
          {notice}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            Current plan
            <Badge variant={b.plan === "PRO" ? "success" : "secondary"}>
              {b.plan}
            </Badge>
            <Badge variant="outline">{b.status}</Badge>
          </CardTitle>
          {b.currentPeriodEnd && (
            <CardDescription>
              {b.cancelAtPeriodEnd ? "Cancels" : "Renews"} on{" "}
              {new Date(b.currentPeriodEnd).toLocaleDateString()}
            </CardDescription>
          )}
        </CardHeader>
        <CardContent>
          <UsageBar
            used={b.leadsUsedThisMonth}
            limit={b.leadLimitThisMonth}
            label="Leads this month"
          />
        </CardContent>
        {b.plan === "PRO" && (
          <CardFooter>
            <Button variant="outline" onClick={manage} disabled={busy}>
              Manage subscription
            </Button>
          </CardFooter>
        )}
      </Card>

      <div className="grid md:grid-cols-2 gap-6">
        <PlanCard
          name={plans.free.name}
          price={plans.free.priceMonthly}
          current={b.plan === "FREE"}
          features={[
            `${plans.free.limits.leadsPerMonth} leads / month`,
            "WhatsApp + web form ingestion",
            "Auto-reply",
            "CSV import / export",
          ]}
        />
        <PlanCard
          name={plans.pro.name}
          price={plans.pro.priceMonthly}
          current={b.plan === "PRO"}
          highlight
          features={[
            `${plans.pro.limits.leadsPerMonth} leads / month`,
            "AI replies (OpenAI)",
            "Google Sheets sync",
            "Priority support",
          ]}
          action={
            b.plan === "PRO" ? null : (
              <Button onClick={upgrade} disabled={busy} className="w-full">
                <Sparkles className="h-4 w-4 mr-2" />
                Upgrade to Pro
              </Button>
            )
          }
        />
      </div>
    </div>
  );
}

function UsageBar(props: { used: number; limit: number; label: string }) {
  const pct = Math.min(
    100,
    Math.round((props.used / Math.max(1, props.limit)) * 100),
  );
  const tone =
    pct >= 100 ? "bg-destructive" : pct >= 80 ? "bg-amber-500" : "bg-primary";
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-sm">
        <span className="text-muted-foreground">{props.label}</span>
        <span className="font-medium">
          {props.used.toLocaleString()} / {props.limit.toLocaleString()}
        </span>
      </div>
      <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
        <div className={`h-full ${tone}`} style={{ width: `${pct}%` }} />
      </div>
      {pct >= 100 && (
        <p className="text-xs text-destructive">
          Limit reached. Upgrade to keep capturing leads from API &amp; manual
          entry.
        </p>
      )}
    </div>
  );
}

function PlanCard(props: {
  name: string;
  price: number;
  current: boolean;
  highlight?: boolean;
  features: string[];
  action?: React.ReactNode;
}) {
  return (
    <Card className={props.highlight ? "border-primary" : ""}>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          {props.name}
          {props.current && <Badge variant="success">Current</Badge>}
        </CardTitle>
        <CardDescription>
          <span className="text-3xl font-bold text-foreground">
            ${props.price}
          </span>
          <span className="text-sm"> / month</span>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="space-y-2 text-sm">
          {props.features.map((f) => (
            <li key={f} className="flex items-start gap-2">
              <Check className="h-4 w-4 text-green-600 mt-0.5 shrink-0" />
              {f}
            </li>
          ))}
        </ul>
      </CardContent>
      {props.action && <CardFooter>{props.action}</CardFooter>}
    </Card>
  );
}
