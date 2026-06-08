"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Settings } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

function Status({ on }: { on: boolean }) {
  return (
    <Badge variant={on ? "success" : "secondary"}>
      {on ? "configured" : "not set"}
    </Badge>
  );
}

export default function SettingsPage() {
  const [s, setS] = useState<Settings | null>(null);
  const [secret, setSecret] = useState("");

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

  if (error) return <p className="text-destructive">{error}</p>;
  if (!s) return <p className="text-muted-foreground">Loading…</p>;

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Settings</h1>
        <p className="text-muted-foreground text-sm">
          Workspace and integration configuration.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Business</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="biz-name">Business name</Label>
            <Input
              id="biz-name"
              value={s.name}
              onChange={(e) => setS({ ...s, name: e.target.value })}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="biz-ind">Industry</Label>
            <Input
              id="biz-ind"
              value={s.industry || ""}
              onChange={(e) => setS({ ...s, industry: e.target.value })}
            />
          </div>
          <div className="flex items-center gap-2">
            <Checkbox
              id="ai"
              checked={s.aiEnabled}
              onCheckedChange={(c) => setS({ ...s, aiEnabled: !!c })}
            />
            <Label htmlFor="ai" className="cursor-pointer">
              Enable AI replies (uses OpenAI when configured)
            </Label>
          </div>
          <div className="space-y-2">
            <Label htmlFor="tmpl">Auto-reply template</Label>
            <Textarea
              id="tmpl"
              value={s.autoReplyTemplate || ""}
              onChange={(e) =>
                setS({ ...s, autoReplyTemplate: e.target.value })
              }
              placeholder="Hi {{name}}, thanks for contacting us…"
              className="h-28"
            />
            <p className="text-xs text-muted-foreground">
              Use <code className="text-foreground">{"{{name}}"}</code> to
              insert the lead&apos;s name.
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            WhatsApp <Status on={s.whatsappAccessTokenConfigured} />
          </CardTitle>
          <CardDescription>
            Per-tenant Meta Cloud API credentials. Stored encrypted
            (AES-GCM-256). Leave blank to keep existing values.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="wa-phone">
              Phone Number ID{" "}
              {s.whatsappPhoneNumberId && (
                <span className="text-xs text-muted-foreground font-normal">
                  (current: {s.whatsappPhoneNumberId})
                </span>
              )}
            </Label>
            <Input
              id="wa-phone"
              value={waPhone}
              onChange={(e) => setWaPhone(e.target.value)}
              placeholder={s.whatsappPhoneNumberId || "e.g. 1238096609383182"}
            />
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="wa-tok" className="flex items-center gap-2">
                Access Token <Status on={s.whatsappAccessTokenConfigured} />
              </Label>
              {s.whatsappAccessTokenConfigured && (
                <Button
                  variant="link"
                  size="sm"
                  onClick={() => clearCredential("whatsappAccessToken")}
                  className="text-destructive h-auto p-0"
                >
                  Clear
                </Button>
              )}
            </div>
            <Input
              id="wa-tok"
              type="password"
              autoComplete="new-password"
              value={waToken}
              onChange={(e) => setWaToken(e.target.value)}
              placeholder="EAAOyZ…"
            />
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="wa-vfy" className="flex items-center gap-2">
                Webhook Verify Token{" "}
                <Status on={s.whatsappVerifyTokenConfigured} />
              </Label>
              {s.whatsappVerifyTokenConfigured && (
                <Button
                  variant="link"
                  size="sm"
                  onClick={() => clearCredential("whatsappVerifyToken")}
                  className="text-destructive h-auto p-0"
                >
                  Clear
                </Button>
              )}
            </div>
            <Input
              id="wa-vfy"
              type="password"
              autoComplete="new-password"
              value={waVerify}
              onChange={(e) => setWaVerify(e.target.value)}
              placeholder="Any string; must match Meta dashboard"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="wa-sec" className="flex items-center gap-2">
              Webhook signing secret <Status on={s.webhookSecretConfigured} />
            </Label>
            <Input
              id="wa-sec"
              type="password"
              autoComplete="new-password"
              value={secret}
              onChange={(e) => setSecret(e.target.value)}
              placeholder="Used to validate X-Hub-Signature-256 (optional)"
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            Telegram <Status on={s.telegramBotTokenConfigured} />
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="tg-tok" className="flex items-center gap-2">
                Bot Token <Status on={s.telegramBotTokenConfigured} />
              </Label>
              {s.telegramBotTokenConfigured && (
                <Button
                  variant="link"
                  size="sm"
                  onClick={() => clearCredential("telegramBotToken")}
                  className="text-destructive h-auto p-0"
                >
                  Clear
                </Button>
              )}
            </div>
            <Input
              id="tg-tok"
              type="password"
              autoComplete="new-password"
              value={tgToken}
              onChange={(e) => setTgToken(e.target.value)}
              placeholder="From @BotFather"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="tg-chat">
              Notification Chat ID{" "}
              {s.telegramChatId && (
                <span className="text-xs text-muted-foreground font-normal">
                  (current: {s.telegramChatId})
                </span>
              )}
            </Label>
            <Input
              id="tg-chat"
              value={tgChat}
              onChange={(e) => setTgChat(e.target.value)}
              placeholder={s.telegramChatId || "e.g. 123456789"}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            OpenAI <Status on={s.openaiApiKeyConfigured} />
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="oai" className="flex items-center gap-2">
                API Key <Status on={s.openaiApiKeyConfigured} />
              </Label>
              {s.openaiApiKeyConfigured && (
                <Button
                  variant="link"
                  size="sm"
                  onClick={() => clearCredential("openaiApiKey")}
                  className="text-destructive h-auto p-0"
                >
                  Clear
                </Button>
              )}
            </div>
            <Input
              id="oai"
              type="password"
              autoComplete="new-password"
              value={openAi}
              onChange={(e) => setOpenAi(e.target.value)}
              placeholder="sk-…"
            />
          </div>
        </CardContent>
      </Card>

      <div className="flex items-center gap-3">
        <Button onClick={save}>Save changes</Button>
        {saved && <span className="text-green-700 text-sm">Saved.</span>}
      </div>
    </div>
  );
}
