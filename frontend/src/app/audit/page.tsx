"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AuditEvent, PageResp } from "@/lib/types";

export default function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await api<PageResp<AuditEvent>>(
        `/api/audit?page=${page}&size=25`,
      );
      setEvents(data.content);
      setTotalPages(data.totalPages);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [page]);

  function actionColor(a: string) {
    if (a === "DELETE") return "bg-red-100 text-red-800";
    if (a === "CREATE") return "bg-green-100 text-green-800";
    if (a === "STATUS_CHANGE") return "bg-blue-100 text-blue-800";
    if (a === "IMPORT") return "bg-purple-100 text-purple-800";
    return "bg-slate-200 text-slate-800";
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Audit log</h1>
      {error && <p className="text-red-600 text-sm">{error}</p>}
      {loading && <p>Loading…</p>}

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-100 text-slate-600">
            <tr>
              <th className="text-left p-3">When</th>
              <th className="text-left p-3">Actor</th>
              <th className="text-left p-3">Entity</th>
              <th className="text-left p-3">Action</th>
              <th className="text-left p-3">Details</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <tr key={e.id} className="border-t">
                <td className="p-3 whitespace-nowrap">
                  {new Date(e.createdAt).toLocaleString()}
                </td>
                <td className="p-3">{e.actor || "—"}</td>
                <td className="p-3 font-mono text-xs">
                  {e.entityType}/{e.entityId.slice(0, 8)}
                </td>
                <td className="p-3">
                  <span
                    className={`px-2 py-1 rounded text-xs ${actionColor(e.action)}`}
                  >
                    {e.action}
                  </span>
                </td>
                <td className="p-3 text-slate-600">{e.details || "—"}</td>
              </tr>
            ))}
            {!loading && events.length === 0 && (
              <tr>
                <td colSpan={5} className="p-6 text-center text-slate-500">
                  No audit events yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-between items-center">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            className="px-3 py-1 border rounded disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-slate-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 border rounded disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
