"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,
} from "@/components/ui/card";
import { CalendarCheck2, CheckCircle2 } from "lucide-react";

interface PublicTenantInfo { name: string; industry: string | null; blurb: string | null; }

/**
 * PUBLIC booking page. No auth required. The slug maps to a tenant; submitting
 * creates a Lead + Appointment in that tenant.
 */
export default function BookPage() {
  const { slug } = useParams<{ slug: string }>();
  const [info, setInfo] = useState<PublicTenantInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const apiBase = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
    fetch(`${apiBase}/book/${slug}`)
      .then(async (r) => {
        if (!r.ok) throw new Error(r.status === 404 ? "Booking page not found" : `HTTP ${r.status}`);
        setInfo(await r.json());
      })
      .catch((e) => setError(e.message));
  }, [slug]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true); setError(null);
    try {
      const startsAt = new Date(`${date}T${time}:00`).toISOString();
      const apiBase = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
      const r = await fetch(`${apiBase}/book/${slug}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, phone: phone || null, email: email || null, message, startsAt }),
      });
      if (!r.ok) {
        const t = await r.text();
        throw new Error(t || `Booking failed (${r.status})`);
      }
      setDone(true);
    } catch (e) { setError((e as Error).message); }
    finally { setBusy(false); }
  }

  if (error && !info) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>Not available</CardTitle>
            <CardDescription>{error}</CardDescription>
          </CardHeader>
        </Card>
      </div>
    );
  }
  if (!info) return <p className="p-8 text-muted-foreground">Loading…</p>;

  if (done) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
        <Card className="w-full max-w-md text-center">
          <CardHeader>
            <CheckCircle2 className="h-12 w-12 text-green-600 mx-auto" />
            <CardTitle>Request received</CardTitle>
            <CardDescription>
              {info.name} will get back to you to confirm shortly. Thanks!
            </CardDescription>
          </CardHeader>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <div className="flex items-center gap-2">
            <CalendarCheck2 className="h-5 w-5 text-primary" />
            <CardTitle>Book with {info.name}</CardTitle>
          </div>
          <CardDescription>
            {info.blurb || "Pick a time and we'll be in touch to confirm."}
          </CardDescription>
        </CardHeader>
        <form onSubmit={submit}>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2 col-span-2">
                <Label htmlFor="bn">Your name</Label>
                <Input id="bn" value={name} onChange={(e) => setName(e.target.value)} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="bp">Phone</Label>
                <Input id="bp" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="be">Email</Label>
                <Input id="be" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="bd">Date</Label>
                <Input id="bd" type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="bt">Time</Label>
                <Input id="bt" type="time" value={time} onChange={(e) => setTime(e.target.value)} required />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="bm">Anything we should know? (optional)</Label>
              <Textarea id="bm" value={message} onChange={(e) => setMessage(e.target.value)} className="min-h-[80px]" />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
          </CardContent>
          <CardFooter>
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? "Submitting…" : "Request booking"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
