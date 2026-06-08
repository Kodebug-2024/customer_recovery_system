"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { UserRole, UserView } from "@/lib/types";

const ROLES: UserRole[] = ["OWNER", "ADMIN", "AGENT", "VIEWER"];

export default function UsersPage() {
  const [users, setUsers] = useState<UserView[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);

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
      setShowCreate(false);
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
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Users</h1>
        <button
          onClick={() => setShowCreate((s) => !s)}
          className="px-4 py-2 bg-slate-900 text-white rounded text-sm"
        >
          {showCreate ? "Cancel" : "+ Invite user"}
        </button>
      </div>

      {notice && (
        <p className="text-green-700 bg-green-50 border border-green-200 rounded px-3 py-2 text-sm">
          {notice}
        </p>
      )}
      {error && <p className="text-red-600 text-sm">{error}</p>}

      {showCreate && (
        <form
          onSubmit={onCreate}
          className="bg-white shadow rounded-lg p-4 grid grid-cols-2 gap-3"
        >
          <input
            className="border rounded px-3 py-2"
            placeholder="Name (optional)"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <input
            className="border rounded px-3 py-2"
            placeholder="Email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            className="border rounded px-3 py-2"
            placeholder="Initial password (min 8)"
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <select
            className="border rounded px-3 py-2"
            value={role}
            onChange={(e) => setRole(e.target.value as UserRole)}
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
          <div className="col-span-2 flex justify-end">
            <button
              type="submit"
              className="px-4 py-2 bg-slate-900 text-white rounded text-sm"
            >
              Create user
            </button>
          </div>
        </form>
      )}

      {loading && <p>Loading…</p>}

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-100 text-slate-600">
            <tr>
              <th className="text-left p-3">Email</th>
              <th className="text-left p-3">Name</th>
              <th className="text-left p-3">Role</th>
              <th className="text-left p-3">Status</th>
              <th className="text-left p-3">Last login</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className="border-t">
                <td className="p-3">{u.email}</td>
                <td className="p-3">{u.name || "—"}</td>
                <td className="p-3">
                  <select
                    value={u.role}
                    onChange={(e) =>
                      onRoleChange(u.id, e.target.value as UserRole)
                    }
                    className="border rounded px-2 py-1 text-xs"
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>
                </td>
                <td className="p-3">
                  <span
                    className={`px-2 py-1 rounded text-xs ${u.enabled ? "bg-green-100 text-green-800" : "bg-slate-200 text-slate-600"}`}
                  >
                    {u.enabled ? "Active" : "Disabled"}
                  </span>
                </td>
                <td className="p-3 text-slate-600">
                  {u.lastLoginAt
                    ? new Date(u.lastLoginAt).toLocaleString()
                    : "—"}
                </td>
                <td className="p-3 text-right space-x-3 whitespace-nowrap">
                  <button
                    onClick={() => onResetPassword(u)}
                    className="text-blue-600 hover:underline"
                  >
                    Reset password
                  </button>
                  <button
                    onClick={() => onToggleEnabled(u)}
                    className={
                      u.enabled
                        ? "text-red-600 hover:underline"
                        : "text-green-700 hover:underline"
                    }
                  >
                    {u.enabled ? "Disable" : "Enable"}
                  </button>
                </td>
              </tr>
            ))}
            {!loading && users.length === 0 && (
              <tr>
                <td colSpan={6} className="p-6 text-center text-slate-500">
                  No users
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
