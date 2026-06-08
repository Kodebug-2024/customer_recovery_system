"use client";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { getToken, setToken } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  LayoutDashboard,
  Users as UsersIcon,
  Settings as SettingsIcon,
  ScrollText,
  Inbox,
  UserCircle,
  LogOut,
  CreditCard,
  BookOpen,
} from "lucide-react";

const NAV = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/leads", label: "Leads", icon: Inbox },
  { href: "/audit", label: "Audit log", icon: ScrollText },
  { href: "/users", label: "Users", icon: UsersIcon },
  { href: "/knowledge", label: "Knowledge", icon: BookOpen },
  { href: "/billing", label: "Billing", icon: CreditCard },
  { href: "/settings", label: "Settings", icon: SettingsIcon },
  { href: "/profile", label: "Profile", icon: UserCircle },
];

const PUBLIC = new Set(["/login", "/signup", "/verify-email"]);
const FULLSCREEN = new Set(["/onboarding"]);

function isPublic(pathname: string): boolean {
  if (PUBLIC.has(pathname)) return true;
  // Public booking pages: /book/{slug}
  if (pathname.startsWith("/book/")) return true;
  return false;
}

export default function Shell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!getToken() && !isPublic(pathname)) router.replace("/login");
  }, [pathname, router]);

  if (isPublic(pathname)) return <>{children}</>;
  if (FULLSCREEN.has(pathname)) return <>{children}</>;

  return (
    <div className="min-h-screen flex bg-muted/30">
      <aside className="w-60 bg-card border-r flex flex-col">
        <div className="px-6 py-5 border-b">
          <div className="font-semibold text-lg">Codezilla CRM</div>
          <div className="text-xs text-muted-foreground">Customer Recovery</div>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {NAV.map((n) => {
            const Icon = n.icon;
            const active = pathname === n.href;
            return (
              <Link
                key={n.href}
                href={n.href}
                className={cn(
                  "flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors",
                  active
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                )}
              >
                <Icon className="h-4 w-4" />
                {n.label}
              </Link>
            );
          })}
        </nav>
        <div className="p-3 border-t">
          <Button
            variant="ghost"
            className="w-full justify-start text-muted-foreground"
            onClick={() => {
              setToken(null);
              router.replace("/login");
            }}
          >
            <LogOut className="h-4 w-4 mr-2" />
            Log out
          </Button>
        </div>
      </aside>
      <main className="flex-1 p-8 overflow-x-auto">{children}</main>
    </div>
  );
}
