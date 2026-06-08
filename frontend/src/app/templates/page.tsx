"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Plus, Pencil, Trash2 } from "lucide-react";

interface Template {
  id: string;
  name: string;
  channel: string;
  event: string;
  subject: string | null;
  body: string;
  isDefault: boolean;
  updatedAt: string;
}

const CHANNELS = ["whatsapp", "telegram", "email", "any"];
const EVENTS = ["auto_reply", "new_lead_notify", "follow_up", "custom"];
const VARS = ["name", "phone", "email", "source", "business"];

export default function TemplatesPage() {
  const [items, setItems] = useState<Template[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Template | null>(null);

  async function load() {
    try {
      setItems(await api<Template[]>("/api/templates"));
    } catch (e) {
      setError((e as Error).message);
    }
  }
  useEffect(() => {
    load();
  }, []);

  function openCreate() {
    setEditing({
      id: "",
      name: "",
      channel: "whatsapp",
      event: "auto_reply",
      subject: null,
      body: "Hi {{name}}, thanks for contacting {{business}}!",
      isDefault: false,
      updatedAt: "",
    });
    setOpen(true);
  }

  async function save() {
    if (!editing) return;
    const body = {
      name: editing.name,
      channel: editing.channel,
      event: editing.event,
      subject: editing.subject,
      body: editing.body,
      isDefault: editing.isDefault,
    };
    try {
      if (editing.id) {
        await api(`/api/templates/${editing.id}`, {
          method: "PUT",
          body: JSON.stringify(body),
        });
      } else {
        await api(`/api/templates`, {
          method: "POST",
          body: JSON.stringify(body),
        });
      }
      setOpen(false);
      setEditing(null);
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function remove(id: string) {
    if (!confirm("Delete this template?")) return;
    try {
      await api(`/api/templates/${id}`, { method: "DELETE" });
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">
            Message templates
          </h1>
          <p className="text-muted-foreground text-sm">
            Reusable bodies for auto-replies and notifications. Use{" "}
            <code>{"{{name}}"}</code> placeholders.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4 mr-2" /> New template
        </Button>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="grid gap-3">
        {items.map((t) => (
          <Card key={t.id}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <CardTitle className="text-base flex items-center gap-2">
                    {t.name}
                    {t.isDefault && <Badge variant="success">default</Badge>}
                  </CardTitle>
                  <CardDescription className="text-xs">
                    {t.channel} · {t.event} · updated{" "}
                    {new Date(t.updatedAt).toLocaleString()}
                  </CardDescription>
                </div>
                <div className="flex gap-1">
                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={() => {
                      setEditing(t);
                      setOpen(true);
                    }}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={() => remove(t.id)}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {t.subject && (
                <p className="text-xs mb-2">
                  <span className="text-muted-foreground">Subject:</span>{" "}
                  {t.subject}
                </p>
              )}
              <p className="text-sm whitespace-pre-wrap line-clamp-4">
                {t.body}
              </p>
            </CardContent>
          </Card>
        ))}
        {items.length === 0 && (
          <Card>
            <CardContent className="py-8 text-center text-muted-foreground text-sm">
              No templates yet. Add one for auto-replies, new-lead alerts, or
              follow-ups.
            </CardContent>
          </Card>
        )}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-xl">
          <DialogHeader>
            <DialogTitle>
              {editing?.id ? "Edit template" : "New template"}
            </DialogTitle>
            <DialogDescription>
              Variables:{" "}
              {VARS.map((v) => (
                <code key={v} className="mx-1">{`{{${v}}}`}</code>
              ))}
            </DialogDescription>
          </DialogHeader>
          {editing && (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="tn">Name</Label>
                <Input
                  id="tn"
                  value={editing.name}
                  onChange={(e) =>
                    setEditing({ ...editing, name: e.target.value })
                  }
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="tc">Channel</Label>
                  <Select
                    value={editing.channel}
                    onValueChange={(v) =>
                      setEditing({ ...editing, channel: v })
                    }
                  >
                    <SelectTrigger id="tc">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {CHANNELS.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="te">Event</Label>
                  <Select
                    value={editing.event}
                    onValueChange={(v) => setEditing({ ...editing, event: v })}
                  >
                    <SelectTrigger id="te">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {EVENTS.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              {editing.channel === "email" && (
                <div className="space-y-2">
                  <Label htmlFor="ts">Subject</Label>
                  <Input
                    id="ts"
                    value={editing.subject ?? ""}
                    onChange={(e) =>
                      setEditing({ ...editing, subject: e.target.value })
                    }
                  />
                </div>
              )}
              <div className="space-y-2">
                <Label htmlFor="tb">Body</Label>
                <Textarea
                  id="tb"
                  value={editing.body}
                  className="min-h-[160px] font-mono text-sm"
                  onChange={(e) =>
                    setEditing({ ...editing, body: e.target.value })
                  }
                />
              </div>
              <div className="flex items-center gap-2">
                <Checkbox
                  id="td"
                  checked={editing.isDefault}
                  onCheckedChange={(c) =>
                    setEditing({ ...editing, isDefault: !!c })
                  }
                />
                <Label htmlFor="td" className="cursor-pointer">
                  Use as default for this channel + event
                </Label>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
