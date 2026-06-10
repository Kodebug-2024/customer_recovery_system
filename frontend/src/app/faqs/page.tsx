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
import { Plus, Pencil, Trash2 } from "lucide-react";

interface Faq {
  id: string;
  pattern: string;
  reply: string;
  priority: number;
  hitCount: number;
  updatedAt: string;
}

interface ProviderStatus {
  name: string;
  available: boolean;
}

interface AiStatus {
  faqCount: number;
  providers: ProviderStatus[];
}

export default function FaqsPage() {
  const [faqs, setFaqs] = useState<Faq[]>([]);
  const [status, setStatus] = useState<AiStatus | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Faq | null>(null);
  const [pattern, setPattern] = useState("");
  const [reply, setReply] = useState("");
  const [priority, setPriority] = useState(0);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const [list, st] = await Promise.all([
        api<Faq[]>("/api/faqs"),
        api<AiStatus>("/api/ai/status"),
      ]);
      setFaqs(list);
      setStatus(st);
    } catch (e) {
      setError((e as Error).message);
    }
  }
  useEffect(() => {
    load();
  }, []);

  function openCreate() {
    setEditing(null);
    setPattern("");
    setReply("");
    setPriority(0);
    setError(null);
    setOpen(true);
  }
  function openEdit(f: Faq) {
    setEditing(f);
    setPattern(f.pattern);
    setReply(f.reply);
    setPriority(f.priority);
    setError(null);
    setOpen(true);
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      if (editing) {
        await api(`/api/faqs/${editing.id}`, {
          method: "PUT",
          body: JSON.stringify({ pattern, reply, priority }),
        });
      } else {
        await api(`/api/faqs`, {
          method: "POST",
          body: JSON.stringify({ pattern, reply, priority }),
        });
      }
      setOpen(false);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function remove(id: string) {
    if (!confirm("Delete this FAQ?")) return;
    await api(`/api/faqs/${id}`, { method: "DELETE" });
    await load();
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">FAQ Engine</h1>
        <p className="text-sm text-muted-foreground">
          When a lead message contains a pattern, the matching reply is sent
          instantly — no LLM call, no cost. Highest priority wins; ties are
          broken by longer (more specific) patterns.
        </p>
      </div>

      {status && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">AI gateway status</CardTitle>
            <CardDescription>
              Order: FAQ → {status.providers.map((p) => p.name).join(" → ")}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            <Badge variant="secondary">
              FAQ: {status.faqCount} pattern{status.faqCount === 1 ? "" : "s"}
            </Badge>
            {status.providers.map((p) => (
              <Badge key={p.name} variant={p.available ? "default" : "outline"}>
                {p.name}: {p.available ? "available" : "unavailable"}
              </Badge>
            ))}
          </CardContent>
        </Card>
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-lg font-medium">Your FAQs ({faqs.length})</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button onClick={openCreate}>
              <Plus className="mr-2 h-4 w-4" /> New FAQ
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{editing ? "Edit FAQ" : "New FAQ"}</DialogTitle>
              <DialogDescription>
                Match is case-insensitive substring. Example pattern{" "}
                <code>shipping</code> hits "do you do shipping?", "shipping
                cost?", etc.
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={save} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="pat">Pattern</Label>
                <Input
                  id="pat"
                  value={pattern}
                  onChange={(e) => setPattern(e.target.value)}
                  placeholder="shipping cost"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="rep">Reply</Label>
                <Textarea
                  id="rep"
                  value={reply}
                  onChange={(e) => setReply(e.target.value)}
                  rows={4}
                  placeholder="Shipping to Singapore is $5 flat for orders under 2kg."
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="pri">Priority (higher wins)</Label>
                <Input
                  id="pri"
                  type="number"
                  value={priority}
                  onChange={(e) =>
                    setPriority(parseInt(e.target.value || "0", 10))
                  }
                />
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <DialogFooter>
                <Button type="submit">{editing ? "Save" : "Create"}</Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Pattern</TableHead>
                <TableHead>Reply</TableHead>
                <TableHead className="w-20">Priority</TableHead>
                <TableHead className="w-20">Hits</TableHead>
                <TableHead className="w-28"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {faqs.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="text-center text-muted-foreground py-8"
                  >
                    No FAQs yet. Create one to start saving on LLM costs.
                  </TableCell>
                </TableRow>
              ) : (
                faqs.map((f) => (
                  <TableRow key={f.id}>
                    <TableCell className="font-mono text-xs">
                      {f.pattern}
                    </TableCell>
                    <TableCell className="text-sm max-w-md truncate">
                      {f.reply}
                    </TableCell>
                    <TableCell>{f.priority}</TableCell>
                    <TableCell>{f.hitCount}</TableCell>
                    <TableCell className="text-right space-x-1">
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => openEdit(f)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => remove(f.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
