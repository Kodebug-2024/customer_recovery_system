"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AuditEvent, PageResp } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ChevronLeft, ChevronRight } from "lucide-react";

function actionVariant(a: string): BadgeProps["variant"] {
  switch (a) {
    case "DELETE":
    case "DISABLE":
      return "destructive";
    case "CREATE":
    case "ENABLE":
      return "success";
    case "STATUS_CHANGE":
    case "ROLE_CHANGE":
      return "info";
    case "PASSWORD_CHANGE":
    case "PASSWORD_RESET":
      return "warning";
    case "IMPORT":
      return "secondary";
    default:
      return "outline";
  }
}

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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Audit log</h1>
        <p className="text-muted-foreground text-sm">
          Who changed what, and when.
        </p>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Card className="p-0">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>When</TableHead>
              <TableHead>Actor</TableHead>
              <TableHead>Entity</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Details</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {events.map((e) => (
              <TableRow key={e.id}>
                <TableCell className="whitespace-nowrap text-muted-foreground">
                  {new Date(e.createdAt).toLocaleString()}
                </TableCell>
                <TableCell>{e.actor || "—"}</TableCell>
                <TableCell className="font-mono text-xs">
                  {e.entityType}/{e.entityId.slice(0, 8)}
                </TableCell>
                <TableCell>
                  <Badge variant={actionVariant(e.action)}>{e.action}</Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {e.details || "—"}
                </TableCell>
              </TableRow>
            ))}
            {!loading && events.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={5}
                  className="text-center text-muted-foreground py-8"
                >
                  No audit events yet
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft className="h-4 w-4 mr-1" /> Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next <ChevronRight className="h-4 w-4 ml-1" />
          </Button>
        </div>
      )}
    </div>
  );
}
