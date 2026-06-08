"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Stats } from "@/lib/types";

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Stats>("/api/stats")
      .then(setStats)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;
  if (!stats) return <p>Loading…</p>;

  const cards = [
    { label: "Total leads", value: stats.total },
    { label: "Active", value: stats.active },
    { label: "Won", value: stats.won },
    { label: "Lost", value: stats.lost },
    {
      label: "Conversion",
      value: `${(stats.conversionRate * 100).toFixed(1)}%`,
    },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Dashboard</h1>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {cards.map((c) => (
          <div key={c.label} className="bg-white rounded-lg shadow p-4">
            <div className="text-sm text-slate-500">{c.label}</div>
            <div className="text-3xl font-semibold mt-1">{c.value}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
