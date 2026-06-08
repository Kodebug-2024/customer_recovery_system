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

export default function SignupPage() {
  const router = useRouter();
  const [businessName, setBusinessName] = useState("");
  const [industry, setIndustry] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (password.length < 8)
        throw new Error("Password must be at least 8 characters");
      const api = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
      const res = await fetch(`${api}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ businessName, industry, name, email, password }),
      });
      if (res.status === 409)
        throw new Error("That email is already registered");
      if (!res.ok) {
        const t = await res.text();
        throw new Error(t || `Signup failed (${res.status})`);
      }
      const data = await res.json();
      setToken(data.token);
      router.replace("/onboarding");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Create your workspace</CardTitle>
          <CardDescription>
            Start capturing and replying to leads in minutes.
          </CardDescription>
        </CardHeader>
        <form onSubmit={submit}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="biz">Business name</Label>
              <Input
                id="biz"
                value={businessName}
                onChange={(e) => setBusinessName(e.target.value)}
                placeholder="Acme Pte Ltd"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="industry">Industry (optional)</Label>
              <Input
                id="industry"
                value={industry}
                onChange={(e) => setIndustry(e.target.value)}
                placeholder="Retail, F&B, Services…"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="name">Your name (optional)</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                autoComplete="name"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password (min 8)</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                minLength={8}
                required
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
          </CardContent>
          <CardFooter className="flex flex-col gap-3">
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? "Creating…" : "Create workspace"}
            </Button>
            <p className="text-xs text-muted-foreground text-center w-full">
              Already have an account?{" "}
              <Link href="/login" className="text-primary hover:underline">
                Sign in
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
