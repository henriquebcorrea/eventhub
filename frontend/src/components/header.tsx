import Link from "next/link";
import { cookies } from "next/headers";
import { CalendarDays, Menu, Ticket, UserRound } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { LogoutButton } from "@/components/logout-button";

export async function Header() {
  const signedIn = Boolean((await cookies()).get("eh_access"));
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/90 backdrop-blur-xl">
      <div className="container-shell flex h-18 items-center justify-between gap-6">
        <Link href="/" className="focus-ring flex items-center gap-2 rounded-lg text-xl font-black tracking-[-.04em]">
          <span className="grid size-9 place-items-center rounded-xl bg-[var(--navy)] text-white"><CalendarDays className="size-5 text-[var(--primary)]" /></span>
          Event<span className="text-[var(--primary)]">Hub</span>
        </Link>
        <nav aria-label="Navegação principal" className="hidden items-center gap-7 md:flex">
          <Link className="text-sm font-semibold text-slate-600 hover:text-slate-950" href="/#eventos">Explorar eventos</Link>
          <Link className="text-sm font-semibold text-slate-600 hover:text-slate-950" href="/organizador/eventos">Organizar</Link>
          {signedIn && <Link className="text-sm font-semibold text-slate-600 hover:text-slate-950" href="/meus-ingressos">Meus ingressos</Link>}
        </nav>
        <div className="flex items-center gap-2">
          {signedIn ? (
            <><Link href="/meus-ingressos" className={cn(buttonVariants({ variant: "secondary", size: "sm" }), "hidden sm:inline-flex")}><Ticket className="size-4" /> Minha conta</Link><span className="hidden sm:inline-flex"><LogoutButton /></span></>
          ) : (
            <><Link href="/entrar" className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "hidden sm:inline-flex")}>Entrar</Link><Link href="/cadastro" className={buttonVariants({ size: "sm" })}><UserRound className="size-4" /> Criar conta</Link></>
          )}
          <details className="relative md:hidden"><summary aria-label="Abrir menu" className="focus-ring grid size-10 cursor-pointer list-none place-items-center rounded-xl"><Menu className="size-5" /></summary><nav aria-label="Navegação mobile" className="absolute right-0 top-12 z-50 grid w-52 gap-1 rounded-xl border border-slate-200 bg-white p-2 shadow-lg"><Link className="rounded-lg px-3 py-2 hover:bg-slate-50" href="/">Explorar eventos</Link><Link className="rounded-lg px-3 py-2 hover:bg-slate-50" href="/organizador/eventos">Organizar</Link>{signedIn && <><Link className="rounded-lg px-3 py-2 hover:bg-slate-50" href="/meus-ingressos">Meus ingressos</Link><LogoutButton /></>}</nav></details>
        </div>
      </div>
    </header>
  );
}

