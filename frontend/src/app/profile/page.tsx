"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { UserView } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ShieldCheck } from "lucide-react";

export default function ProfilePage() {
  const [me, setMe] = useState<UserView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [tfaEnabled, setTfaEnabled] = useState<boolean | null>(null);
  const [enroll, setEnroll] = useState<{
    qrDataUrl: string;
    secret: string;
  } | null>(null);
  const [tfaCode, setTfaCode] = useState("");
  const [tfaError, setTfaError] = useState<string | null>(null);
  const [tfaNotice, setTfaNotice] = useState<string | null>(null);

  useEffect(() => {
    api<UserView>("/api/users/me")
      .then(setMe)
      .catch((e) => setError((e as Error).message));
    api<{ enabled: boolean }>("/api/2fa/status")
      .then((s) => setTfaEnabled(s.enabled))
      .catch(() => setTfaEnabled(null));
  }, []);

  async function onChange(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setNotice(null);
    if (next !== confirm) return setError("New passwords do not match");
    if (next.length < 8)
      return setError("New password must be at least 8 characters");
    try {
      await api("/api/users/me/password", {
        method: "POST",
        body: JSON.stringify({ currentPassword: current, newPassword: next }),
      });
      setNotice("Password updated.");
      setCurrent("");
      setNext("");
      setConfirm("");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function startEnroll() {
    setTfaError(null);
    setTfaNotice(null);
    setTfaCode("");
    try {
      const r = await api<{ qrDataUrl: string; secret: string }>(
        "/api/2fa/enroll",
        { method: "POST" },
      );
      setEnroll(r);
    } catch (e) {
      setTfaError((e as Error).message);
    }
  }

  async function confirmEnroll(e: React.FormEvent) {
    e.preventDefault();
    setTfaError(null);
    try {
      await api("/api/2fa/confirm", {
        method: "POST",
        body: JSON.stringify({ code: tfaCode }),
      });
      setEnroll(null);
      setTfaCode("");
      setTfaEnabled(true);
      setTfaNotice("Two-factor authentication is now on.");
    } catch (e) {
      setTfaError((e as Error).message);
    }
  }

  async function disable() {
    setTfaError(null);
    const code = prompt(
      "Enter a current 6-digit code from your authenticator to confirm:",
    );
    if (!code) return;
    try {
      await api("/api/2fa/disable", {
        method: "POST",
        body: JSON.stringify({ code }),
      });
      setTfaEnabled(false);
      setTfaNotice("Two-factor authentication disabled.");
    } catch (e) {
      setTfaError((e as Error).message);
    }
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">My profile</h1>
        <p className="text-muted-foreground text-sm">
          Your account details, password, and 2-factor authentication.
        </p>
      </div>
      {me && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Account</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <div className="text-xs text-muted-foreground">Email</div>
              <div>{me.email}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Role</div>
              <Badge variant="secondary">{me.role}</Badge>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Status</div>
              <Badge variant={me.enabled ? "success" : "secondary"}>
                {me.enabled ? "Active" : "Disabled"}
              </Badge>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Last login</div>
              <div>
                {me.lastLoginAt
                  ? new Date(me.lastLoginAt).toLocaleString()
                  : "—"}
              </div>
            </div>
          </CardContent>
        </Card>
      )}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <ShieldCheck className="h-5 w-5" /> Two-factor authentication
            {tfaEnabled !== null && (
              <Badge variant={tfaEnabled ? "success" : "secondary"}>
                {tfaEnabled ? "Enabled" : "Disabled"}
              </Badge>
            )}
          </CardTitle>
          <CardDescription>
            Use an authenticator app (Google Authenticator, 1Password, Authy,
            etc.).
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {tfaError && <p className="text-sm text-destructive">{tfaError}</p>}
          {tfaNotice && <p className="text-sm text-green-700">{tfaNotice}</p>}
          {tfaEnabled === false && !enroll && (
            <Button onClick={startEnroll}>Enable 2FA</Button>
          )}
          {tfaEnabled === true && (
            <Button variant="outline" onClick={disable}>
              Disable 2FA
            </Button>
          )}
          {enroll && (
            <form onSubmit={confirmEnroll} className="space-y-3">
              <p className="text-sm">
                Scan the QR, then enter the 6-digit code to confirm.
              </p>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={enroll.qrDataUrl}
                alt="TOTP QR"
                className="border rounded p-2 bg-white"
                width={220}
                height={220}
              />
              <p className="text-xs text-muted-foreground">
                Can&apos;t scan? Manual secret:{" "}
                <code className="font-mono">{enroll.secret}</code>
              </p>
              <div className="space-y-2 max-w-xs">
                <Label htmlFor="tfa-code">6-digit code</Label>
                <Input
                  id="tfa-code"
                  value={tfaCode}
                  onChange={(e) => setTfaCode(e.target.value)}
                  inputMode="numeric"
                  pattern="\d{6}"
                  maxLength={6}
                  required
                />
              </div>
              <div className="flex gap-2">
                <Button type="submit">Confirm &amp; enable</Button>
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setEnroll(null)}
                >
                  Cancel
                </Button>
              </div>
            </form>
          )}
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Change password</CardTitle>
          <CardDescription>Minimum 8 characters.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onChange} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="cur">Current password</Label>
              <Input
                id="cur"
                type="password"
                value={current}
                onChange={(e) => setCurrent(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="new">New password</Label>
              <Input
                id="new"
                type="password"
                value={next}
                onChange={(e) => setNext(e.target.value)}
                minLength={8}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="cnf">Confirm new password</Label>
              <Input
                id="cnf"
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            {notice && <p className="text-sm text-green-700">{notice}</p>}
            <Button type="submit">Update password</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
