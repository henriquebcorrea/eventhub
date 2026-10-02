import Link from "next/link";
import { CalendarDays, MapPin, QrCode } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { WaitlistCard } from "@/components/waitlist-card";
import { authenticatedGet } from "@/lib/server-api";
import type { TicketView, WaitlistView } from "@/lib/types";
import { formatDate } from "@/lib/utils";
export const metadata = { title: "Meus ingressos" };
export default async function MyTicketsPage() {
  const [tickets, waitlist] = await Promise.all([authenticatedGet<TicketView[]>("tickets/mine"), authenticatedGet<WaitlistView[]>("waitlist/mine")]);
  return <main className="container-shell py-12"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Sua agenda</p><div className="mt-1 flex items-end justify-between"><h1 className="text-4xl font-black tracking-[-.045em]">Meus ingressos</h1><Link className="text-sm font-bold text-[var(--primary)]" href="/">Encontrar eventos</Link></div>
    {waitlist.length > 0 && <section className="mt-8"><h2 className="text-2xl font-black">Lista de espera</h2><div className="mt-4 grid gap-5 lg:grid-cols-2">{waitlist.map((request) => <WaitlistCard key={request.id} request={request} />)}</div></section>}
    {tickets.length ? <div className="mt-8 grid gap-5 lg:grid-cols-2">{tickets.map((ticket) => <Link key={ticket.id} href={`/ingressos/${ticket.id}`} className="focus-ring surface group grid overflow-hidden sm:grid-cols-[160px_1fr]"><div className="event-image min-h-40">{ticket.coverUrl && <img src={ticket.coverUrl} alt="" className="h-full w-full object-cover" />}</div><div className="p-5"><div className="flex items-start justify-between gap-3"><Badge className={ticket.status === "ACTIVE" && ticket.eventStatus === "PUBLISHED" ? "" : "bg-red-50 text-red-700"}>{ticket.status === "ACTIVE" && ticket.eventStatus === "PUBLISHED" ? "Confirmado" : "Cancelado"}</Badge><QrCode className="size-5 text-slate-400 transition group-hover:text-[var(--primary)]" /></div><h2 className="mt-3 text-xl font-black">{ticket.eventTitle}</h2><p className="mt-1 text-sm font-semibold text-slate-700">{ticket.attendeeName ?? "Participante"} · {ticket.ticketTypeName ?? "Ingresso geral"}</p><p className="mt-2 flex items-center gap-2 text-sm text-slate-500"><CalendarDays className="size-4" />{formatDate(ticket.startsAt)}</p><p className="mt-1 flex items-center gap-2 text-sm text-slate-500"><MapPin className="size-4" />{ticket.venue} · {ticket.city}</p></div></Link>)}</div> : !waitlist.length && <div className="surface mt-8 grid min-h-64 place-items-center text-center"><div><QrCode className="mx-auto size-10 text-slate-300" /><h2 className="mt-3 text-xl font-bold">Você ainda não tem ingressos</h2><p className="mt-1 text-slate-500">Explore a agenda e escolha sua próxima experiência.</p></div></div>}
  </main>;
}

