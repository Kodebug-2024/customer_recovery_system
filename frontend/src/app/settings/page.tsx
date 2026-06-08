"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Settings } from "@/lib/types";

export default function SettingsPage() {
  const [s, setS] = useState<Settings | null>(null);
  const [secret, setSecret] = useState("");
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Settings>("/api/settings")
      .then(setS)
      .catch((e) => setError(e.message));
  }, []);

  async function save() {
    if (!s) return;
    setSaved(false);
    try {
      const updated = await api<Settings>("/api/settings", {
        method: "PUT",
        body: JSON.stringify({
          name: s.name,
          industry: s.industry,
          aiEnabled: s.aiEnabled,
          autoReplyTemplate: s.autoReplyTemplate,
          webhookSecret: secret || undefined,
        }),
      });
      setS(updated);
      setSecret("");
      setSaved(true);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;
  if (!s) return <p>Loading…</p>;

  return (
    <div className="space-y-4 max-w-2xl">
      <h1 className="text-2xl font-semibold">Settings</h1>
      <div className="bg-white rounded-lg shadow p-6 space-y-4">
        <div>
          <label className="block text-sm mb-1">Business name</label>
          <input
            value={s.name}
            onChange={(e) => setS({ ...s, name: e.target.value })}
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-sm mb-1">Industry</label>
          <input
            value={s.industry || ""}
            onChange={(e) => setS({ ...s, industry: e.target.value })}
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <div className="flex items-center gap-2">
          <input
            id="ai"
            type="checkbox"
            checked={s.aiEnabled}
            onChange={(e) => setS({ ...s, aiEnabled: e.target.checked })}
          />
          <label htmlFor="ai" className="text-sm">
            Enable AI replies (uses OpenAI when configured)
          </label>
        </div>
        <div>
          <label className="block text-sm mb-1">Auto-reply template</label>
          <textarea
            value={s.autoReplyTemplate || ""}
            onChange={(e) => setS({ ...s, autoReplyTemplate: e.target.value })}
            className="w-full border rounded px-3 py-2 h-28"
            placeholder="Hi {{name}}, thanks for contacting us…"
          />
          <p className="text-xs text-slate-500 mt-1">
            Use <code>{"{{name}}"}</code> to insert the lead&apos;s name.
          </p>
        </div>
        <div>
          <label className="block text-sm mb-1">
            WhatsApp webhook secret{" "}
            {s.webhookSecretConfigured && (
              <span className="text-green-600 text-xs">(configured)</span>
            )}
          </label>
          <input
            value={secret}
            onChange={(e) => setSecret(e.target.value)}
            placeholder="Leave blank to keep existing"
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <button
          onClick={save}
          className="px-4 py-2 bg-slate-900 text-white rounded"
        >
          Save
        </button>
        {saved && <span className="ml-3 text-green-600 text-sm">Saved.</span>}
      </div>
    </div>
  );
}
