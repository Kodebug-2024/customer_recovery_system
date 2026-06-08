"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import type { Settings } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  CheckCircle2,
  Circle,
  MessageSquare,
  Sparkles,
  Send,
} from "lucide-react";
import { cn } from "@/lib/utils";

type Step = 1 | 2 | 3;

export default function OnboardingPage() {
  const router = useRouter();
  const [s, setS] = useState<Settings | null>(null);
  const [step, setStep] = useState<Step>(1);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Step 1 — WhatsApp credentials.
  const [waPhone, setWaPhone] = useState("");
  const [waToken, setWaToken] = useState("");

  // Step 2 — auto-reply template.
  const [template, setTemplate] = useState(
    "Hi {{name}}, thanks for reaching out! We'll get back to you within a few minutes.",
  );

  // Step 3 — sample lead.
  const [testName, setTestName] = useState("Test Lead");
  const [testMessage, setTestMessage] = useState("Hello, I'm interested.");

  useEffect(() => {
    api<Settings>("/api/settings")
      .then((data) => {
        setS(data);
        if (data.autoReplyTemplate) setTemplate(data.autoReplyTemplate);
      })
      .catch((e) => setError((e as Error).message));
  }, []);

  async function saveStep1() {
    setBusy(true);
    setError(null);
    try {
      const body: Record<string, unknown> = {};
      if (waPhone) body.whatsappPhoneNumberId = waPhone;
      if (waToken) body.whatsappAccessToken = waToken;
      if (Object.keys(body).length > 0)
        await api("/api/settings", {
          method: "PUT",
          body: JSON.stringify(body),
        });
      setStep(2);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function saveStep2() {
    setBusy(true);
    setError(null);
    try {
      await api("/api/settings", {
        method: "PUT",
        body: JSON.stringify({ autoReplyTemplate: template }),
      });
      setStep(3);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function finish(createSample: boolean) {
    setBusy(true);
    setError(null);
    try {
      if (createSample) {
        await api("/api/leads", {
          method: "POST",
          body: JSON.stringify({
            name: testName,
            source: "manual",
            message: testMessage,
          }),
        });
      }
      await api("/api/settings/complete-onboarding", { method: "POST" });
      router.replace("/");
    } catch (e) {
      setError((e as Error).message);
      setBusy(false);
    }
  }

  if (error && !s) return <p className="text-destructive p-8">{error}</p>;

  return (
    <div className="min-h-screen bg-muted/30 p-4">
      <div className="max-w-2xl mx-auto py-8 space-y-6">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">
            Welcome to Codezilla CRM
          </h1>
          <p className="text-muted-foreground text-sm">
            Three quick steps to start capturing leads.
          </p>
        </div>

        <Steps current={step} />

        {error && <p className="text-sm text-destructive">{error}</p>}

        {step === 1 && (
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <MessageSquare className="h-5 w-5 text-primary" />
                <CardTitle>Connect WhatsApp</CardTitle>
              </div>
              <CardDescription>
                Add your Meta Cloud API credentials. You can skip and add them
                later in Settings.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="ph">Phone Number ID</Label>
                <Input
                  id="ph"
                  value={waPhone}
                  onChange={(e) => setWaPhone(e.target.value)}
                  placeholder={
                    s?.whatsappPhoneNumberId || "e.g. 1238096609383182"
                  }
                />
                {s?.whatsappPhoneNumberId && (
                  <p className="text-xs text-muted-foreground">
                    Current: {s.whatsappPhoneNumberId}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="tk">Access Token</Label>
                <Input
                  id="tk"
                  type="password"
                  value={waToken}
                  onChange={(e) => setWaToken(e.target.value)}
                  placeholder="EAAOyZ…"
                  autoComplete="new-password"
                />
                {s?.whatsappAccessTokenConfigured && (
                  <p className="text-xs text-muted-foreground">
                    Already configured. Leave blank to keep.
                  </p>
                )}
              </div>
            </CardContent>
            <CardFooter className="justify-between">
              <Button variant="ghost" onClick={() => setStep(2)}>
                Skip for now
              </Button>
              <Button onClick={saveStep1} disabled={busy}>
                Save &amp; continue
              </Button>
            </CardFooter>
          </Card>
        )}

        {step === 2 && (
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <Sparkles className="h-5 w-5 text-primary" />
                <CardTitle>Set your auto-reply</CardTitle>
              </div>
              <CardDescription>
                Sent automatically to new leads. Use <code>{"{{name}}"}</code>{" "}
                to insert their name.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Textarea
                value={template}
                onChange={(e) => setTemplate(e.target.value)}
                className="h-32"
              />
            </CardContent>
            <CardFooter className="justify-between">
              <Button variant="ghost" onClick={() => setStep(1)}>
                Back
              </Button>
              <Button onClick={saveStep2} disabled={busy}>
                Save &amp; continue
              </Button>
            </CardFooter>
          </Card>
        )}

        {step === 3 && (
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <Send className="h-5 w-5 text-primary" />
                <CardTitle>Try it out</CardTitle>
              </div>
              <CardDescription>
                Create a sample lead so you can see how the dashboard looks with
                data. You can delete it later.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="tn">Name</Label>
                <Input
                  id="tn"
                  value={testName}
                  onChange={(e) => setTestName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tm">Message</Label>
                <Input
                  id="tm"
                  value={testMessage}
                  onChange={(e) => setTestMessage(e.target.value)}
                />
              </div>
            </CardContent>
            <CardFooter className="justify-between">
              <Button
                variant="ghost"
                onClick={() => finish(false)}
                disabled={busy}
              >
                Skip &amp; finish
              </Button>
              <Button onClick={() => finish(true)} disabled={busy}>
                Create lead &amp; finish
              </Button>
            </CardFooter>
          </Card>
        )}

        <p className="text-xs text-muted-foreground text-center">
          You can revisit setup anytime from{" "}
          <Link href="/settings" className="underline">
            Settings
          </Link>
          .
        </p>
      </div>
    </div>
  );
}

function Steps({ current }: { current: Step }) {
  const items = [
    { n: 1, label: "WhatsApp" },
    { n: 2, label: "Auto-reply" },
    { n: 3, label: "Try it" },
  ];
  return (
    <div className="flex items-center gap-2">
      {items.map((it, i) => {
        const Icon = it.n < current ? CheckCircle2 : Circle;
        const active = it.n === current;
        const done = it.n < current;
        return (
          <div key={it.n} className="flex items-center gap-2 flex-1">
            <div
              className={cn(
                "flex items-center gap-2 rounded-md px-3 py-2 flex-1",
                active && "bg-primary text-primary-foreground",
                done && "text-muted-foreground",
                !active && !done && "border bg-card",
              )}
            >
              <Icon className="h-4 w-4" />
              <span className="text-sm font-medium">{it.label}</span>
            </div>
            {i < items.length - 1 && <div className="h-px bg-border flex-1" />}
          </div>
        );
      })}
    </div>
  );
}
