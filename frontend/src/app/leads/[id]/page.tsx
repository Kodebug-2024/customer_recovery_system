"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import type { Lead, LeadStatus, MessageItem } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import { Send } from "lucide-react";
import { cn } from "@/lib/utils";

const STATUSES: LeadStatus[] = ["NEW", "CONTACTED", "QUALIFIED", "WON", "LOST"];

function statusVariant(s: LeadStatus): BadgeProps["variant"] {
  switch (s) {
    case "NEW":
      return "info";
    case "CONTACTED":
      return "secondary";
    case "QUALIFIED":
      return "warning";
    case "WON":
      return "success";
    case "LOST":
      return "destructive";
    default:
      return "outline";
  }
}

export default function LeadDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [lead, setLead] = useState<Lead | null>(null);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [reply, setReply] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      const [l, m] = await Promise.all([
        api<Lead>(`/api/leads/${id}`),
        api<MessageItem[]>(`/api/leads/${id}/messages`),
      ]);
      setLead(l);
      setMessages(m);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  useEffect(() => {
    if (id) load();
  }, [id]);

  async function changeStatus(status: LeadStatus) {
    const updated = await api<Lead>(`/api/leads/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
    setLead(updated);
  }

  async function send() {
    if (!reply.trim()) return;
    await api(`/api/leads/${id}/messages`, {
      method: "POST",
      body: JSON.stringify({ content: reply, channel: "whatsapp" }),
    });
    setReply("");
    load();
  }

  if (error) return <p className="text-destructive">{error}</p>;
  if (!lead) return <p className="text-muted-foreground">Loading…</p>;

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">
            {lead.name || "Unnamed lead"}
          </h1>
          <p className="text-muted-foreground text-sm">
            {lead.source} · created {new Date(lead.createdAt).toLocaleString()}
          </p>
        </div>
        <Badge variant={statusVariant(lead.status)}>{lead.status}</Badge>
      </div>

      <Card>
        <CardContent className="pt-6 grid grid-cols-2 gap-4 text-sm">
          <div>
            <div className="text-xs text-muted-foreground">Phone</div>
            <div>{lead.phone || "—"}</div>
          </div>
          <div>
            <div className="text-xs text-muted-foreground">Email</div>
            <div>{lead.email || "—"}</div>
          </div>
          <div className="col-span-2">
            <div className="text-xs text-muted-foreground mb-1">Status</div>
            <Select
              value={lead.status}
              onValueChange={(v) => changeStatus(v as LeadStatus)}
            >
              <SelectTrigger className="w-44">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Conversation</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="space-y-2 max-h-96 overflow-y-auto pr-2">
            {messages.map((m) => (
              <div
                key={m.id}
                className={cn(
                  "p-3 rounded-lg max-w-md",
                  m.direction === "INBOUND"
                    ? "bg-muted"
                    : "bg-primary text-primary-foreground ml-auto",
                )}
              >
                <div className="text-xs opacity-70 mb-1">
                  {m.direction} · {m.channel} ·{" "}
                  {new Date(m.createdAt).toLocaleString()}
                </div>
                <div className="whitespace-pre-wrap text-sm">{m.content}</div>
              </div>
            ))}
            {messages.length === 0 && (
              <p className="text-sm text-muted-foreground">No messages yet.</p>
            )}
          </div>
          <div className="flex gap-2 pt-2 border-t">
            <Input
              value={reply}
              onChange={(e) => setReply(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && send()}
              placeholder="Type a reply…"
              className="flex-1"
            />
            <Button onClick={send}>
              <Send className="h-4 w-4 mr-2" /> Send
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
