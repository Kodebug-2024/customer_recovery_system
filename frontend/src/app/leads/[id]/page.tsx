"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import type { Lead, LeadStatus, MessageItem } from "@/lib/types";

const STATUSES: LeadStatus[] = ["NEW", "CONTACTED", "QUALIFIED", "WON", "LOST"];

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

  if (error) return <p className="text-red-600">{error}</p>;
  if (!lead) return <p>Loading…</p>;

  return (
    <div className="space-y-6 max-w-4xl">
      <h1 className="text-2xl font-semibold">{lead.name || "Unnamed lead"}</h1>

      <div className="bg-white rounded-lg shadow p-4 grid grid-cols-2 gap-3 text-sm">
        <div>
          <span className="text-slate-500">Phone:</span> {lead.phone || "—"}
        </div>
        <div>
          <span className="text-slate-500">Email:</span> {lead.email || "—"}
        </div>
        <div>
          <span className="text-slate-500">Source:</span> {lead.source}
        </div>
        <div>
          <span className="text-slate-500">Created:</span>{" "}
          {new Date(lead.createdAt).toLocaleString()}
        </div>
        <div className="col-span-2">
          <span className="text-slate-500">Status:</span>{" "}
          <select
            value={lead.status}
            onChange={(e) => changeStatus(e.target.value as LeadStatus)}
            className="border rounded px-2 py-1 ml-2"
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-4 space-y-3">
        <h2 className="font-medium">Conversation</h2>
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`p-3 rounded max-w-md ${
                m.direction === "INBOUND"
                  ? "bg-slate-100"
                  : "bg-blue-100 ml-auto"
              }`}
            >
              <div className="text-xs text-slate-500 mb-1">
                {m.direction} · {m.channel} ·{" "}
                {new Date(m.createdAt).toLocaleString()}
              </div>
              <div className="whitespace-pre-wrap">{m.content}</div>
            </div>
          ))}
          {messages.length === 0 && (
            <p className="text-slate-500 text-sm">No messages yet.</p>
          )}
        </div>
        <div className="flex gap-2 pt-2 border-t">
          <input
            value={reply}
            onChange={(e) => setReply(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && send()}
            placeholder="Type a reply…"
            className="flex-1 border rounded px-3 py-2"
          />
          <button
            onClick={send}
            className="px-4 py-2 bg-slate-900 text-white rounded"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}
