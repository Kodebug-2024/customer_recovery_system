"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { UserView } from "@/lib/types";

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
    <div className="space-y-6 max-w-xl">
      <h1 className="text-2xl font-semibold">My profile</h1>

      {me && (
        <div className="bg-white shadow rounded-lg p-4 text-sm space-y-1">
          <div>
            <span className="text-slate-500">Email:</span> {me.email}
          </div>
          <div>
            <span className="text-slate-500">Role:</span> {me.role}
          </div>
          <div>
            <span className="text-slate-500">Status:</span>{" "}
            {me.enabled ? "Active" : "Disabled"}
          </div>
          <div>
            <span className="text-slate-500">Last login:</span>{" "}
            {me.lastLoginAt ? new Date(me.lastLoginAt).toLocaleString() : "—"}
          </div>
        </div>
      )}

      <form
        onSubmit={onChange}
        className="bg-white shadow rounded-lg p-4 space-y-3"
      >
        <h2 className="font-semibold">Change password</h2>
        <input
          className="border rounded px-3 py-2 w-full"
          type="password"
          placeholder="Current password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          required
        />
        <input
          className="border rounded px-3 py-2 w-full"
          type="password"
          placeholder="New password (min 8)"
          value={next}
          onChange={(e) => setNext(e.target.value)}
          required
          minLength={8}
        />
        <input
          className="border rounded px-3 py-2 w-full"
          type="password"
          placeholder="Confirm new password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          required
        />
        {error && <p className="text-red-600 text-sm">{error}</p>}
        {notice && <p className="text-green-700 text-sm">{notice}</p>}
        <button
          type="submit"
          className="px-4 py-2 bg-slate-900 text-white rounded text-sm"
        >
          Update password
        </button>
      </form>
    </div>
  );
}
