"use client";
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { setToken } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@demo.local");
  const [password, setPassword] = useState("password123");
  const [totpCode, setTotpCode] = useState("");
  const [needTotp, setNeedTotp] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const api = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
      const res = await fetch(`${api}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          password,
          totpCode: needTotp ? totpCode : undefined,
        }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        if (res.status === 401 && body.twoFactorRequired) {
          setNeedTotp(true);
          setError(null);
          return;
        }
        throw new Error(body.error || "Invalid credentials");
      }
      setToken(body.token);
      router.replace("/");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Codezilla CRM</CardTitle>
          <CardDescription>Sign in to your dashboard</CardDescription>
        </CardHeader>
        <form onSubmit={submit}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />
            </div>
            {needTotp && (
              <div className="space-y-2">
                <Label htmlFor="totp">6-digit code</Label>
                <Input
                  id="totp"
                  value={totpCode}
                  onChange={(e) => setTotpCode(e.target.value)}
                  inputMode="numeric"
                  pattern="\d{6}"
                  maxLength={6}
                  autoComplete="one-time-code"
                  autoFocus
                  required
                />
                <p className="text-xs text-muted-foreground">
                  From your authenticator app.
                </p>
              </div>
            )}
            {error && <p className="text-sm text-destructive">{error}</p>}
          </CardContent>
          <CardFooter className="flex flex-col gap-3">
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? "Signing in…" : "Sign in"}
            </Button>
            <div className="flex justify-between w-full text-xs">
              <Link
                href="/forgot-password"
                className="text-muted-foreground hover:text-primary hover:underline"
              >
                Forgot password?
              </Link>
              <span className="text-muted-foreground">
                No account?{" "}
                <Link href="/signup" className="text-primary hover:underline">
                  Create one
                </Link>
              </span>
            </div>
          </CardFooter>
        </form>
      </Card>
      <p className="mt-6 text-center text-xs text-muted-foreground">
        <Link href="/legal/terms" className="hover:underline">
          Terms
        </Link>
        {" · "}
        <Link href="/legal/privacy" className="hover:underline">
          Privacy
        </Link>
      </p>
    </div>
  );
}
