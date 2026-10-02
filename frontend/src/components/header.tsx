import Link from "next/link";
import { cookies } from "next/headers";
import { ArrowUpRight, Menu, Ticket } from "lucide-react";
import { BrandMark } from "@/components/brand-mark";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { LogoutButton } from "@/components/logout-button";

export async function Header() {
  const signedIn = Boolean((await cookies()).get("eh_access"));
  return <header className="relative z-40 border-b border-white/15 bg-[var(--navy)] text-[var(--paper)]">
    <div className="container-shell flex min-h-18 items-center justify-between gap-4 py-3">
      <Link href="/" aria-label="EventHub — início" className="focus-ring flex items-center gap-2 rounded-sm font-display text-xl font-extrabold tracking-[-.065em] sm:text-2xl">
        <BrandMark className="size-10 text-[var(--accent)]" /><span>Event<span className="text-[var(--accent)]">Hub</span><span className="text-[var(--primary)]">.</span></span>
      </Link>
      <nav aria-label="Navegação principal" className="hidden items-center gap-8 lg:flex">
        <Link className="text-sm font-bold text-white/70 hover:text-[var(--accent)]" href="/#eventos">Explorar agenda</Link>
        <Link className="text-sm font-bold text-white/70 hover:text-[var(--accent)]" href="/organizador/eventos">Para organizadores</Link>
        {signedIn && <Link className="text-sm font-bold text-white/70 hover:text-[var(--accent)]" href="/meus-ingressos">Meus ingressos</Link>}
      </nav>
      <div className="flex items-center gap-2">
        {signedIn ? <><Link href="/meus-ingressos" className={cn(buttonVariants({ variant: "secondary", size: "sm" }), "hidden sm:inline-flex")}><Ticket className="size-4" /> Minha conta</Link><span className="hidden sm:inline-flex"><LogoutButton /></span></> : <><Link href="/entrar" className="focus-ring hidden px-4 py-2 text-sm font-bold text-white/80 hover:text-white sm:inline-flex">Entrar</Link><Link href="/cadastro" className={cn(buttonVariants({ size: "sm" }), "max-[360px]:hidden")}><span className="sm:hidden">Criar</span><span className="hidden sm:inline">Criar conta</span><ArrowUpRight className="size-4" /></Link></>}
        <details className="relative lg:hidden"><summary aria-label="Abrir menu" className="focus-ring grid size-11 cursor-pointer list-none place-items-center border border-white/20"><Menu className="size-5" /></summary><nav aria-label="Navegação mobile" className="absolute right-0 top-13 z-50 grid w-[min(19rem,calc(100vw-1.25rem))] gap-1 border border-white/20 bg-[var(--navy)] p-3 shadow-2xl"><Link className="px-3 py-3 text-sm font-bold hover:bg-white/10" href="/#eventos">Explorar agenda</Link><Link className="px-3 py-3 text-sm font-bold hover:bg-white/10" href="/organizador/eventos">Para organizadores</Link>{signedIn && <><Link className="px-3 py-3 text-sm font-bold hover:bg-white/10" href="/meus-ingressos">Meus ingressos</Link><LogoutButton /></>}{!signedIn && <><Link className="px-3 py-3 text-sm font-bold hover:bg-white/10 sm:hidden" href="/entrar">Entrar</Link><Link className="px-3 py-3 text-sm font-bold hover:bg-white/10 max-[360px]:block min-[361px]:hidden" href="/cadastro">Criar conta</Link></>}</nav></details>
      </div>
    </div>
  </header>;
}

