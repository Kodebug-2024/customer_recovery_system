"use client";
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { api, apiDownload, apiUpload } from "@/lib/api";
import type { Lead, LeadStatus, PageResp } from "@/lib/types";

const STATUSES: (LeadStatus | "")[] = [
  "",
  "NEW",
  "CONTACTED",
  "QUALIFIED",
  "WON",
  "LOST",
];

export default function LeadsPage() {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [status, setStatus] = useState<LeadStatus | "">("");
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
      if (status) qs.set("status", status);
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
  }, [status]);

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
      if (!r.enabled) {
        setNotice(
          `Google Sheets is in stub mode. ${r.rows} rows would be exported. Set SHEETS_MODE=real to enable.`,
        );
      } else if (r.ok) {
        setNotice(`Appended ${r.rows} rows to Google Sheet.`);
      } else {
        setError("Sheets export failed — check server logs.");
      }
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Leads</h1>
        <div className="flex gap-2">
          <button
            onClick={onExport}
            className="px-3 py-2 border rounded text-sm"
          >
            Export CSV
          </button>
          <button
            onClick={() => fileRef.current?.click()}
            className="px-3 py-2 border rounded text-sm"
          >
            Import CSV
          </button>
          <button
            onClick={onSyncSheet}
            className="px-3 py-2 border rounded text-sm"
          >
            Sync to Google Sheet
          </button>
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
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as LeadStatus | "")}
          className="border rounded px-3 py-2"
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s || "All statuses"}
            </option>
          ))}
        </select>
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search name / phone / email / message"
          className="border rounded px-3 py-2 flex-1"
        />
        <button
          onClick={load}
          className="px-4 py-2 bg-slate-900 text-white rounded"
        >
          Refresh
        </button>
      </div>

      {notice && (
        <p className="text-green-700 bg-green-50 border border-green-200 rounded px-3 py-2 text-sm">
          {notice}
        </p>
      )}
      {error && <p className="text-red-600 text-sm">{error}</p>}
      {loading && <p>Loading…</p>}

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-100 text-slate-600">
            <tr>
              <th className="text-left p-3">Name</th>
              <th className="text-left p-3">Phone</th>
              <th className="text-left p-3">Source</th>
              <th className="text-left p-3">Status</th>
              <th className="text-left p-3">Created</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((l) => (
              <tr key={l.id} className="border-t">
                <td className="p-3">{l.name || "—"}</td>
                <td className="p-3">{l.phone || "—"}</td>
                <td className="p-3">{l.source}</td>
                <td className="p-3">
                  <span className="px-2 py-1 rounded text-xs bg-slate-200">
                    {l.status}
                  </span>
                </td>
                <td className="p-3">
                  {new Date(l.createdAt).toLocaleString()}
                </td>
                <td className="p-3 text-right space-x-3">
                  <Link
                    href={`/leads/${l.id}`}
                    className="text-blue-600 hover:underline"
                  >
                    View
                  </Link>
                  <button
                    onClick={() => onDelete(l.id)}
                    className="text-red-600 hover:underline"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {!loading && filtered.length === 0 && (
              <tr>
                <td colSpan={6} className="p-6 text-center text-slate-500">
                  No leads
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
