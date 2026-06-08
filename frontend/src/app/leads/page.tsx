"use client";
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { api, apiDownload, apiUpload } from "@/lib/api";
import type { Lead, LeadStatus, PageResp } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Download, Upload, Sheet, RefreshCw, Trash2, Eye } from "lucide-react";

const STATUSES: (LeadStatus | "ALL")[] = [
  "ALL",
  "NEW",
  "CONTACTED",
  "QUALIFIED",
  "WON",
  "LOST",
];

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

export default function LeadsPage() {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [status, setStatus] = useState<LeadStatus | "ALL">("ALL");
  const [mine, setMine] = useState(false);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const qs = new URLSearchParams({ size: "50", sort: "createdAt,desc" });
      if (status !== "ALL") qs.set("status", status);
      if (mine) qs.set("mine", "true");
      const data = await api<PageResp<Lead>>(`/api/leads?${qs}`);
      setLeads(data.content);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [status, mine]);

  const filtered = leads.filter((l) => {
    if (!search) return true;
    const s = search.toLowerCase();
    return (
      (l.name && l.name.toLowerCase().includes(s)) ||
      (l.phone && l.phone.toLowerCase().includes(s)) ||
      (l.email && l.email.toLowerCase().includes(s)) ||
      (l.message && l.message.toLowerCase().includes(s))
    );
  });

  async function onDelete(id: string) {
    if (!confirm("Delete this lead? This cannot be undone from the UI."))
      return;
    try {
      await api(`/api/leads/${id}`, { method: "DELETE" });
      setNotice("Lead deleted.");
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onExport() {
    try {
      await apiDownload("/api/leads/export", "leads.csv");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function onImport(file: File) {
    setError(null);
    setNotice(null);
    try {
      const r = await apiUpload<{ created: number; skipped: number }>(
        "/api/leads/import",
        file,
      );
      setNotice(`Imported ${r.created} leads (${r.skipped} skipped).`);
      load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      if (fileRef.current) fileRef.current.value = "";
    }
  }

  async function onSyncSheet() {
    setError(null);
    setNotice(null);
    try {
      const r = await api<{ enabled: boolean; ok: boolean; rows: number }>(
        "/api/leads/sync-sheet",
        { method: "POST" },
      );
      if (!r.enabled)
        setNotice(
          `Google Sheets is in stub mode. ${r.rows} rows would be exported. Set SHEETS_MODE=real to enable.`,
        );
      else if (r.ok) setNotice(`Appended ${r.rows} rows to Google Sheet.`);
      else setError("Sheets export failed — check server logs.");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Leads</h1>
          <p className="text-muted-foreground text-sm">
            Inbound leads from all channels.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={onExport}>
            <Download className="h-4 w-4 mr-2" /> Export
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => fileRef.current?.click()}
          >
            <Upload className="h-4 w-4 mr-2" /> Import
          </Button>
          <Button variant="outline" size="sm" onClick={onSyncSheet}>
            <Sheet className="h-4 w-4 mr-2" /> Sync sheet
          </Button>
          <input
            ref={fileRef}
            type="file"
            accept=".csv,text/csv"
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) onImport(f);
            }}
          />
        </div>
      </div>

      <div className="flex gap-3">
        <Select
          value={status}
          onValueChange={(v) => setStatus(v as LeadStatus | "ALL")}
        >
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {s === "ALL" ? "All statuses" : s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant={mine ? "default" : "outline"}
          size="default"
          onClick={() => setMine((m) => !m)}
        >
          Assigned to me
        </Button>
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search name / phone / email / message"
          className="flex-1"
        />
        <Button onClick={load} variant="secondary">
          <RefreshCw className="h-4 w-4 mr-2" /> Refresh
        </Button>
      </div>

      {notice && (
        <div className="text-sm rounded-md border border-green-200 bg-green-50 text-green-800 px-3 py-2">
          {notice}
        </div>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}

      <Card className="p-0">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Phone</TableHead>
              <TableHead>Source</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((l) => (
              <TableRow key={l.id}>
                <TableCell className="font-medium">{l.name || "—"}</TableCell>
                <TableCell>{l.phone || "—"}</TableCell>
                <TableCell>{l.source}</TableCell>
                <TableCell>
                  <Badge variant={statusVariant(l.status)}>{l.status}</Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {new Date(l.createdAt).toLocaleString()}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" asChild>
                      <Link href={`/leads/${l.id}`}>
                        <Eye className="h-4 w-4" />
                      </Link>
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => onDelete(l.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {!loading && filtered.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center text-muted-foreground py-8"
                >
                  No leads
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
}
