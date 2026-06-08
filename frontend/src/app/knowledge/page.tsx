"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";
import { Plus, RefreshCw, Trash2 } from "lucide-react";

interface Doc {
  id: string; title: string; content: string; indexed: boolean;
  createdAt: string; updatedAt: string;
}

export default function KnowledgePage() {
  const [docs, setDocs] = useState<Doc[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  async function load() {
    try { setDocs(await api<Doc[]>("/api/knowledge")); }
    catch (e) { setError((e as Error).message); }
  }
  useEffect(() => { load(); }, []);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api("/api/knowledge", { method: "POST", body: JSON.stringify({ title, content }) });
      setOpen(false); setTitle(""); setContent(""); load();
    } catch (e) { setError((e as Error).message); }
  }

  async function reindex(id: string) {
    try { await api(`/api/knowledge/${id}/reindex`, { method: "POST" }); load(); }
    catch (e) { setError((e as Error).message); }
  }

  async function remove(id: string) {
    if (!confirm("Delete this document?")) return;
    try { await api(`/api/knowledge/${id}`, { method: "DELETE" }); load(); }
    catch (e) { setError((e as Error).message); }
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Knowledge base</h1>
          <p className="text-muted-foreground text-sm">
            Upload product info, FAQs, pricing, etc. AI replies will reference these.
          </p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button><Plus className="h-4 w-4 mr-2" /> Add document</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Add knowledge document</DialogTitle>
              <DialogDescription>
                Plain text or markdown. The full document is embedded for retrieval; keep
                it focused (one topic per doc works best).
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={create} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="kt">Title</Label>
                <Input id="kt" value={title} onChange={(e) => setTitle(e.target.value)} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="kc">Content</Label>
                <Textarea
                  id="kc"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  className="min-h-[200px]"
                  required
                />
              </div>
              <DialogFooter>
                <Button type="submit">Save &amp; index</Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="grid gap-3">
        {docs.map((d) => (
          <Card key={d.id}>
            <CardHeader className="pb-2">
              <div className="flex justify-between items-start gap-2">
                <CardTitle className="text-base">{d.title}</CardTitle>
                <div className="flex items-center gap-2">
                  <Badge variant={d.indexed ? "success" : "warning"}>
                    {d.indexed ? "indexed" : "not indexed"}
                  </Badge>
                  <Button size="icon" variant="ghost" onClick={() => reindex(d.id)} title="Re-index">
                    <RefreshCw className="h-4 w-4" />
                  </Button>
                  <Button size="icon" variant="ghost" onClick={() => remove(d.id)}>
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
              </div>
              <CardDescription className="text-xs">
                {new Date(d.updatedAt).toLocaleString()}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm whitespace-pre-wrap line-clamp-3">{d.content}</p>
            </CardContent>
          </Card>
        ))}
        {docs.length === 0 && (
          <Card><CardContent className="py-8 text-center text-muted-foreground text-sm">
            No knowledge documents yet. Add one to give the AI context.
          </CardContent></Card>
        )}
      </div>
    </div>
  );
}
