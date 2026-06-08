"use client";
import { useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { api, getToken, apiUrl } from "@/lib/api";
import type { Lead, LeadStatus, MessageItem, UserView } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import { Send, X, Plus, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";

const STATUSES: LeadStatus[] = ["NEW", "CONTACTED", "QUALIFIED", "WON", "LOST"];
const UNASSIGNED = "__none__";

interface Note {
  id: string; leadId: string; authorUserId: string | null;
  body: string; createdAt: string; updatedAt: string;
}
interface Tag { id: string; name: string; color: string | null; createdAt: string; }
interface Appointment {
  id: string; leadId: string; startsAt: string; durationMinutes: number;
  status: string; notes: string | null;
}

function statusVariant(s: LeadStatus): BadgeProps["variant"] {
  switch (s) {
    case "NEW": return "info";
    case "CONTACTED": return "secondary";
    case "QUALIFIED": return "warning";
    case "WON": return "success";
    case "LOST": return "destructive";
    default: return "outline";
  }
}

export default function LeadDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [lead, setLead] = useState<Lead | null>(null);
  const [users, setUsers] = useState<UserView[]>([]);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [notes, setNotes] = useState<Note[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [reply, setReply] = useState("");
  const [noteBody, setNoteBody] = useState("");
  const [tagInput, setTagInput] = useState("");
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  async function load() {
    setError(null);
    try {
      const [l, m, u, n, t, a] = await Promise.all([
        api<Lead>(`/api/leads/${id}`),
        api<MessageItem[]>(`/api/leads/${id}/messages`),
        api<UserView[]>(`/api/users`).catch(() => [] as UserView[]),
        api<Note[]>(`/api/leads/${id}/notes`).catch(() => [] as Note[]),
        api<Tag[]>(`/api/leads/${id}/tags`).catch(() => [] as Tag[]),
        api<Appointment[]>(`/api/leads/${id}/appointments`).catch(() => [] as Appointment[]),
      ]);
      setLead(l); setMessages(m); setUsers(u); setNotes(n); setTags(t); setAppointments(a);
    } catch (e) { setError((e as Error).message); }
  }

  useEffect(() => { if (id) load(); }, [id]);

  // Live conversation via Server-Sent Events.
  useEffect(() => {
    if (!id) return;
    const token = getToken();
    if (!token) return;
    const url = apiUrl(`/api/leads/${id}/messages/stream?access_token=${encodeURIComponent(token)}`);
    const es = new EventSource(url);
    es.addEventListener("message", (ev) => {
      try {
        const msg = JSON.parse((ev as MessageEvent).data) as MessageItem;
        setMessages((prev) => prev.some((m) => m.id === msg.id) ? prev : [...prev, msg]);
      } catch { /* ignore */ }
    });
    es.onerror = () => { /* let browser auto-reconnect */ };
    return () => es.close();
  }, [id]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  async function changeStatus(status: LeadStatus) {
    setLead(await api<Lead>(`/api/leads/${id}/status`, {
      method: "PATCH", body: JSON.stringify({ status }),
    }));
  }
  async function changeAssignee(value: string) {
    const userId = value === UNASSIGNED ? null : value;
    setLead(await api<Lead>(`/api/leads/${id}/assign`, {
      method: "PATCH", body: JSON.stringify({ userId }),
    }));
  }
  async function send() {
    if (!reply.trim()) return;
    await api(`/api/leads/${id}/messages`, {
      method: "POST", body: JSON.stringify({ content: reply, channel: "whatsapp" }),
    });
    setReply("");
  }
  async function addNote() {
    if (!noteBody.trim()) return;
    await api(`/api/leads/${id}/notes`, { method: "POST", body: JSON.stringify({ body: noteBody }) });
    setNoteBody(""); load();
  }
  async function deleteNote(noteId: string) {
    await api(`/api/leads/${id}/notes/${noteId}`, { method: "DELETE" });
    load();
  }
  async function addTag(e: React.FormEvent) {
    e.preventDefault();
    if (!tagInput.trim()) return;
    await api(`/api/leads/${id}/tags`, { method: "POST", body: JSON.stringify({ name: tagInput.trim() }) });
    setTagInput(""); load();
  }
  async function removeTag(tagId: string) {
    await api(`/api/leads/${id}/tags/${tagId}`, { method: "DELETE" });
    load();
  }

  if (error) return <p className="text-destructive">{error}</p>;
  if (!lead) return <p className="text-muted-foreground">Loading…</p>;

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">{lead.name || "Unnamed lead"}</h1>
          <p className="text-muted-foreground text-sm">
            {lead.source} · created {new Date(lead.createdAt).toLocaleString()}
          </p>
        </div>
        <Badge variant={statusVariant(lead.status)}>{lead.status}</Badge>
      </div>

      <Card>
        <CardContent className="pt-6 grid grid-cols-2 gap-4 text-sm">
          <div><div className="text-xs text-muted-foreground">Phone</div><div>{lead.phone || "—"}</div></div>
          <div><div className="text-xs text-muted-foreground">Email</div><div>{lead.email || "—"}</div></div>
          <div>
            <div className="text-xs text-muted-foreground mb-1">Status</div>
            <Select value={lead.status} onValueChange={(v) => changeStatus(v as LeadStatus)}>
              <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
              <SelectContent>
                {STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div>
            <div className="text-xs text-muted-foreground mb-1">Assigned to</div>
            <Select value={lead.assignedToUserId ?? UNASSIGNED} onValueChange={changeAssignee}>
              <SelectTrigger className="w-56"><SelectValue placeholder="Unassigned" /></SelectTrigger>
              <SelectContent>
                <SelectItem value={UNASSIGNED}>Unassigned</SelectItem>
                {users.filter((u) => u.enabled).map((u) => (
                  <SelectItem key={u.id} value={u.id}>{u.name || u.email}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="col-span-2">
            <div className="text-xs text-muted-foreground mb-1">Tags</div>
            <div className="flex flex-wrap items-center gap-2">
              {tags.map((t) => (
                <Badge key={t.id} variant="secondary" className="cursor-pointer">
                  {t.name}
                  <button
                    onClick={() => removeTag(t.id)}
                    className="ml-1 hover:text-destructive"
                    aria-label={`Remove ${t.name}`}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
              <form onSubmit={addTag} className="flex items-center gap-1">
                <Input
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  placeholder="add tag…"
                  className="h-7 w-32 text-xs"
                />
                <Button type="submit" variant="ghost" size="sm" className="h-7">
                  <Plus className="h-3 w-3" />
                </Button>
              </form>
            </div>
          </div>
        </CardContent>
      </Card>

      {appointments.length > 0 && (
        <Card>
          <CardHeader><CardTitle className="text-lg">Appointments</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            {appointments.map((a) => (
              <div key={a.id} className="flex justify-between border rounded p-3">
                <div>
                  <div className="font-medium">{new Date(a.startsAt).toLocaleString()}</div>
                  <div className="text-xs text-muted-foreground">{a.durationMinutes} min</div>
                </div>
                <Badge variant="outline">{a.status}</Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle className="text-lg">Conversation</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="space-y-2 max-h-96 overflow-y-auto pr-2">
            {messages.map((m) => (
              <div
                key={m.id}
                className={cn(
                  "p-3 rounded-lg max-w-md",
                  m.direction === "INBOUND" ? "bg-muted" : "bg-primary text-primary-foreground ml-auto",
                )}
              >
                <div className="text-xs opacity-70 mb-1">
                  {m.direction} · {m.channel} · {new Date(m.createdAt).toLocaleString()}
                </div>
                <div className="whitespace-pre-wrap text-sm">{m.content}</div>
              </div>
            ))}
            {messages.length === 0 && <p className="text-sm text-muted-foreground">No messages yet.</p>}
            <div ref={messagesEndRef} />
          </div>
          <div className="flex gap-2 pt-2 border-t">
            <Input
              value={reply}
              onChange={(e) => setReply(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && send()}
              placeholder="Type a reply…"
              className="flex-1"
            />
            <Button onClick={send}><Send className="h-4 w-4 mr-2" /> Send</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-lg">Internal notes</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="space-y-2">
            {notes.map((n) => (
              <div key={n.id} className="border rounded p-3 text-sm">
                <div className="flex justify-between items-start gap-2">
                  <div className="text-xs text-muted-foreground">
                    {new Date(n.createdAt).toLocaleString()}
                  </div>
                  <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => deleteNote(n.id)}>
                    <Trash2 className="h-3 w-3 text-destructive" />
                  </Button>
                </div>
                <div className="whitespace-pre-wrap">{n.body}</div>
              </div>
            ))}
            {notes.length === 0 && <p className="text-sm text-muted-foreground">No notes yet.</p>}
          </div>
          <div className="flex gap-2 pt-2 border-t">
            <Textarea
              value={noteBody}
              onChange={(e) => setNoteBody(e.target.value)}
              placeholder="Add an internal note (only your team sees this)…"
              className="flex-1 min-h-[60px]"
            />
            <Button onClick={addNote} className="self-end">Add note</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
