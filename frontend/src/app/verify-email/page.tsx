"use client";
import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CheckCircle2, XCircle, Loader2 } from "lucide-react";

type State = "checking" | "ok" | "bad";

export default function VerifyEmailPageOuter() {
  return (
    <Suspense fallback={null}>
      <VerifyEmailPage />
    </Suspense>
  );
}

function VerifyEmailPage() {
  const params = useSearchParams();
  const token = params.get("token");
  const [state, setState] = useState<State>("checking");
  const [msg, setMsg] = useState("");

  useEffect(() => {
    if (!token) {
      setState("bad");
      setMsg("Missing token.");
      return;
    }
    const api = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
    fetch(`${api}/auth/verify-email?token=${encodeURIComponent(token)}`, {
      method: "POST",
    })
      .then(async (r) => {
        const body = await r.json().catch(() => ({}));
        if (r.ok && body.ok) {
          setState("ok");
          setMsg(body.message || "Email verified");
        } else {
          setState("bad");
          setMsg(body.message || "Verification failed");
        }
      })
      .catch((e) => {
        setState("bad");
        setMsg(e.message);
      });
  }, [token]);

  const Icon =
    state === "checking" ? Loader2 : state === "ok" ? CheckCircle2 : XCircle;
  const iconClass =
    state === "checking"
      ? "text-muted-foreground animate-spin"
      : state === "ok"
        ? "text-green-600"
        : "text-destructive";

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-sm text-center">
        <CardHeader>
          <div className="mx-auto mb-2">
            <Icon className={`h-12 w-12 ${iconClass}`} />
          </div>
          <CardTitle>
            {state === "checking" && "Verifying…"}
            {state === "ok" && "Email verified"}
            {state === "bad" && "Could not verify"}
          </CardTitle>
          <CardDescription>{msg}</CardDescription>
        </CardHeader>
        <CardContent />
        <CardFooter className="justify-center">
          <Button asChild>
            <Link href="/login">Go to sign in</Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
