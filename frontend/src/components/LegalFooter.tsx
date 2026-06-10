import Link from "next/link";

/**
 * Footer used by public pages (login/signup/legal/booking). Dashboard
 * already has its own chrome via Shell.tsx.
 */
export default function LegalFooter() {
  return (
    <footer className="mt-12 border-t py-6 text-center text-xs text-muted-foreground">
      <div className="space-x-4">
        <Link href="/legal/terms" className="hover:underline">Terms</Link>
        <Link href="/legal/privacy" className="hover:underline">Privacy</Link>
        <a href="mailto:support@codezilla.example" className="hover:underline">Contact</a>
      </div>
      <p className="mt-2">© {new Date().getFullYear()} Codezilla. All rights reserved.</p>
    </footer>
  );
}
