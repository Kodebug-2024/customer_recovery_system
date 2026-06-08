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

export default function ProfilePage() {
  const [me, setMe] = useState<UserView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");

  useEffect(() => {
    api<UserView>("/api/users/me")
      .then(setMe)
      .catch((e) => setError((e as Error).message));
  }, []);

  async function onChange(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setNotice(null);
    if (next !== confirm) {
      setError("New passwords do not match");
      return;
    }
    if (next.length < 8) {
      setError("New password must be at least 8 characters");
      return;
    }
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

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">My profile</h1>
        <p className="text-muted-foreground text-sm">
          Your account details and password.
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
