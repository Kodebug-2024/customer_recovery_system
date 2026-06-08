"use client";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Stats } from "@/lib/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { TrendingUp, Inbox, Trophy, X as XIcon, Activity } from "lucide-react";

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Stats>("/api/stats")
      .then(setStats)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-destructive">{error}</p>;
  if (!stats) return <p className="text-muted-foreground">Loading…</p>;

  const cards = [
    { label: "Total leads", value: stats.total, icon: Inbox },
    { label: "Active", value: stats.active, icon: Activity },
    { label: "Won", value: stats.won, icon: Trophy },
    { label: "Lost", value: stats.lost, icon: XIcon },
    {
      label: "Conversion",
      value: `${(stats.conversionRate * 100).toFixed(1)}%`,
      icon: TrendingUp,
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground text-sm">
          Overview of your lead pipeline.
        </p>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {cards.map((c) => {
          const Icon = c.icon;
          return (
            <Card key={c.label}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {c.label}
                </CardTitle>
                <Icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold">{c.value}</div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
