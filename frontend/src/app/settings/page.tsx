"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Settings } from "@/lib/types";

export default function SettingsPage() {
  const [s, setS] = useState<Settings | null>(null);
  const [secret, setSecret] = useState("");

  // Credentials are write-only from the UI side. Empty string = leave unchanged.
  const [waPhone, setWaPhone] = useState("");
  const [waToken, setWaToken] = useState("");
  const [waVerify, setWaVerify] = useState("");
  const [tgToken, setTgToken] = useState("");
  const [tgChat, setTgChat] = useState("");
  const [openAi, setOpenAi] = useState("");

  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function reload() {
    const data = await api<Settings>("/api/settings");
    setS(data);
    setWaPhone("");
    setTgChat("");
  }

  useEffect(() => {
    reload().catch((e) => setError((e as Error).message));
  }, []);

  async function save() {
    if (!s) return;
    setSaved(false);
    setError(null);
    try {
      // Send each credential field only if user typed something. Empty = unchanged.
      // To clear a credential, type a single space then trim — easier UX: explicit "Clear" button below.
      const body: Record<string, unknown> = {
        name: s.name,
        industry: s.industry,
        aiEnabled: s.aiEnabled,
        autoReplyTemplate: s.autoReplyTemplate,
      };
      if (secret) body.webhookSecret = secret;
      if (waPhone) body.whatsappPhoneNumberId = waPhone;
      if (waToken) body.whatsappAccessToken = waToken;
      if (waVerify) body.whatsappVerifyToken = waVerify;
      if (tgToken) body.telegramBotToken = tgToken;
      if (tgChat) body.telegramChatId = tgChat;
      if (openAi) body.openaiApiKey = openAi;

      const updated = await api<Settings>("/api/settings", {
        method: "PUT",
        body: JSON.stringify(body),
      });
      setS(updated);
      setSecret("");
      setWaToken("");
      setWaVerify("");
      setTgToken("");
      setOpenAi("");
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function clearCredential(field: string) {
    if (
      !confirm(
        `Clear ${field}? Subsequent requests will fall back to the server default (if any).`,
      )
    )
      return;
    try {
      await api<Settings>("/api/settings", {
        method: "PUT",
        body: JSON.stringify({ [field]: "" }),
      });
      reload();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;
  if (!s) return <p>Loading…</p>;

  function Status({ on }: { on: boolean }) {
    return (
      <span
        className={`text-xs px-2 py-0.5 rounded ${on ? "bg-green-100 text-green-800" : "bg-slate-200 text-slate-600"}`}
      >
        {on ? "configured" : "not set"}
      </span>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <h1 className="text-2xl font-semibold">Settings</h1>

      <section className="bg-white rounded-lg shadow p-6 space-y-4">
        <h2 className="font-semibold">Business</h2>
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
      </section>

      <section className="bg-white rounded-lg shadow p-6 space-y-4">
        <h2 className="font-semibold flex items-center gap-2">
          WhatsApp <Status on={s.whatsappAccessTokenConfigured} />
        </h2>
        <p className="text-xs text-slate-500">
          Per-tenant Meta Cloud API credentials. Stored encrypted (AES-GCM-256).
          Leave blank to keep existing values.
        </p>

        <div>
          <label className="block text-sm mb-1">
            Phone Number ID{" "}
            {s.whatsappPhoneNumberId && (
              <span className="text-xs text-slate-500">
                (current: {s.whatsappPhoneNumberId})
              </span>
            )}
          </label>
          <input
            value={waPhone}
            onChange={(e) => setWaPhone(e.target.value)}
            placeholder={s.whatsappPhoneNumberId || "e.g. 1238096609383182"}
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-sm mb-1 flex items-center gap-2">
            Access Token <Status on={s.whatsappAccessTokenConfigured} />
            {s.whatsappAccessTokenConfigured && (
              <button
                onClick={() => clearCredential("whatsappAccessToken")}
                className="ml-auto text-xs text-red-600 hover:underline"
              >
                Clear
              </button>
            )}
          </label>
          <input
            type="password"
            value={waToken}
            onChange={(e) => setWaToken(e.target.value)}
            placeholder="EAAOyZ…"
            className="w-full border rounded px-3 py-2"
            autoComplete="new-password"
          />
        </div>
        <div>
          <label className="block text-sm mb-1 flex items-center gap-2">
            Webhook Verify Token <Status on={s.whatsappVerifyTokenConfigured} />
            {s.whatsappVerifyTokenConfigured && (
              <button
                onClick={() => clearCredential("whatsappVerifyToken")}
                className="ml-auto text-xs text-red-600 hover:underline"
              >
                Clear
              </button>
            )}
          </label>
          <input
            type="password"
            value={waVerify}
            onChange={(e) => setWaVerify(e.target.value)}
            placeholder="Any string; must match what you put in Meta dashboard"
            className="w-full border rounded px-3 py-2"
            autoComplete="new-password"
          />
        </div>
        <div>
          <label className="block text-sm mb-1 flex items-center gap-2">
            Webhook signing secret <Status on={s.webhookSecretConfigured} />
          </label>
          <input
            type="password"
            value={secret}
            onChange={(e) => setSecret(e.target.value)}
            placeholder="Used to validate X-Hub-Signature-256 (optional)"
            className="w-full border rounded px-3 py-2"
            autoComplete="new-password"
          />
        </div>
      </section>

      <section className="bg-white rounded-lg shadow p-6 space-y-4">
        <h2 className="font-semibold flex items-center gap-2">
          Telegram <Status on={s.telegramBotTokenConfigured} />
        </h2>
        <div>
          <label className="block text-sm mb-1 flex items-center gap-2">
            Bot Token <Status on={s.telegramBotTokenConfigured} />
            {s.telegramBotTokenConfigured && (
              <button
                onClick={() => clearCredential("telegramBotToken")}
                className="ml-auto text-xs text-red-600 hover:underline"
              >
                Clear
              </button>
            )}
          </label>
          <input
            type="password"
            value={tgToken}
            onChange={(e) => setTgToken(e.target.value)}
            placeholder="From @BotFather"
            className="w-full border rounded px-3 py-2"
            autoComplete="new-password"
          />
        </div>
        <div>
          <label className="block text-sm mb-1">
            Notification Chat ID{" "}
            {s.telegramChatId && (
              <span className="text-xs text-slate-500">
                (current: {s.telegramChatId})
              </span>
            )}
          </label>
          <input
            value={tgChat}
            onChange={(e) => setTgChat(e.target.value)}
            placeholder={s.telegramChatId || "e.g. 123456789"}
            className="w-full border rounded px-3 py-2"
          />
        </div>
      </section>

      <section className="bg-white rounded-lg shadow p-6 space-y-4">
        <h2 className="font-semibold flex items-center gap-2">
          OpenAI <Status on={s.openaiApiKeyConfigured} />
        </h2>
        <div>
          <label className="block text-sm mb-1 flex items-center gap-2">
            API Key <Status on={s.openaiApiKeyConfigured} />
            {s.openaiApiKeyConfigured && (
              <button
                onClick={() => clearCredential("openaiApiKey")}
                className="ml-auto text-xs text-red-600 hover:underline"
              >
                Clear
              </button>
            )}
          </label>
          <input
            type="password"
            value={openAi}
            onChange={(e) => setOpenAi(e.target.value)}
            placeholder="sk-…"
            className="w-full border rounded px-3 py-2"
            autoComplete="new-password"
          />
        </div>
      </section>

      <div className="flex items-center gap-3">
        <button
          onClick={save}
          className="px-4 py-2 bg-slate-900 text-white rounded"
        >
          Save changes
        </button>
        {saved && <span className="text-green-600 text-sm">Saved.</span>}
      </div>
    </div>
  );
}
