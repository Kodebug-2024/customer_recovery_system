"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { UserRole, UserView } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { UserPlus, KeyRound, Power } from "lucide-react";

const ROLES: UserRole[] = ["OWNER", "ADMIN", "AGENT", "VIEWER"];

export default function UsersPage() {
  const [users, setUsers] = useState<UserView[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [open, setOpen] = useState(false);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<UserRole>("AGENT");

  async function load() {
    setLoading(true);
    setError(null);
    try {
      setUsers(await api<UserView[]>("/api/users"));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    load();
  }, []);

  function flash(msg: string) {
    setNotice(msg);
    setTimeout(() => setNotice(null), 4000);
  }

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api("/api/users", {
        method: "POST",
        body: JSON.stringify({ name: name || null, email, password, role }),
      });
      flash(`Created ${email}`);
      setOpen(false);
      setName("");
      setEmail("");
      setPassword("");
      setRole("AGENT");
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onRoleChange(id: string, newRole: UserRole) {
    try {
      await api(`/api/users/${id}/role`, {
        method: "PATCH",
        body: JSON.stringify({ role: newRole }),
      });
      flash("Role updated");
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onToggleEnabled(u: UserView) {
    try {
      await api(`/api/users/${u.id}/enabled`, {
        method: "PATCH",
        body: JSON.stringify({ enabled: !u.enabled }),
      });
      flash(u.enabled ? "User disabled" : "User enabled");
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onResetPassword(u: UserView) {
    const pw = prompt(`Set new password for ${u.email} (min 8 chars):`);
    if (!pw) return;
    try {
      await api(`/api/users/${u.id}/reset-password`, {
        method: "POST",
        body: JSON.stringify({ password: pw }),
      });
      flash(`Password reset for ${u.email}`);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Users</h1>
          <p className="text-muted-foreground text-sm">Manage team access.</p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>
              <UserPlus className="h-4 w-4 mr-2" /> Invite user
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Invite a new user</DialogTitle>
              <DialogDescription>
                They will be able to sign in immediately with the password you
                set.
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={onCreate} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="u-name">Name (optional)</Label>
                <Input
                  id="u-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="u-email">Email</Label>
                <Input
                  id="u-email"
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="u-pw">Initial password (min 8)</Label>
                <Input
                  id="u-pw"
                  type="password"
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="u-role">Role</Label>
                <Select
                  value={role}
                  onValueChange={(v) => setRole(v as UserRole)}
                >
                  <SelectTrigger id="u-role">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLES.map((r) => (
                      <SelectItem key={r} value={r}>
                        {r}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <DialogFooter>
                <Button type="submit">Create user</Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {notice && (
        <div className="text-sm rounded-md border border-green-200 bg-green-50 text-green-800 px-3 py-2">
          {notice}
        </div>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}

      <Card className="p-0">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Email</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Last login</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((u) => (
              <TableRow key={u.id}>
                <TableCell className="font-medium">{u.email}</TableCell>
                <TableCell>{u.name || "—"}</TableCell>
                <TableCell>
                  <Select
                    value={u.role}
                    onValueChange={(v) => onRoleChange(u.id, v as UserRole)}
                  >
                    <SelectTrigger className="h-8 w-28">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ROLES.map((r) => (
                        <SelectItem key={r} value={r}>
                          {r}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </TableCell>
                <TableCell>
                  <Badge variant={u.enabled ? "success" : "secondary"}>
                    {u.enabled ? "Active" : "Disabled"}
                  </Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {u.lastLoginAt
                    ? new Date(u.lastLoginAt).toLocaleString()
                    : "—"}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onResetPassword(u)}
                    >
                      <KeyRound className="h-4 w-4 mr-1" /> Reset pw
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onToggleEnabled(u)}
                    >
                      <Power className="h-4 w-4 mr-1" />
                      {u.enabled ? "Disable" : "Enable"}
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {!loading && users.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center text-muted-foreground py-8"
                >
                  No users
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
}
