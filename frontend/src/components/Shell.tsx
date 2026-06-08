"use client";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { getToken, setToken } from "@/lib/api";

const NAV = [
  { href: "/", label: "Dashboard" },
  { href: "/leads", label: "Leads" },
  { href: "/audit", label: "Audit log" },
  { href: "/settings", label: "Settings" },
];

export default function Shell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!getToken() && pathname !== "/login") router.replace("/login");
  }, [pathname, router]);

  if (pathname === "/login") return <>{children}</>;

  return (
    <div className="min-h-screen flex">
      <aside className="w-56 bg-slate-900 text-slate-100 p-4 space-y-2">
        <div className="text-xl font-semibold mb-6">Codezilla CRM</div>
        {NAV.map((n) => (
          <Link
            key={n.href}
            href={n.href}
            className={`block px-3 py-2 rounded ${
              pathname === n.href ? "bg-slate-700" : "hover:bg-slate-800"
            }`}
          >
            {n.label}
          </Link>
        ))}
        <button
          onClick={() => {
            setToken(null);
            router.replace("/login");
          }}
          className="block w-full text-left px-3 py-2 rounded hover:bg-slate-800 mt-8 text-sm text-slate-300"
        >
          Log out
        </button>
      </aside>
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}
