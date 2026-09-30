import Link from "next/link";
import { CalendarPlus, ChevronRight, MapPin, Users } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";
import { formatShortDate } from "@/lib/utils";
export const metadata = { title: "Painel do organizador" };
const statusLabel = { DRAFT: "Rascunho", PUBLISHED: "Publicado", CANCELLED: "Cancelado", COMPLETED: "Concluído" };
export default async function OrganizerEventsPage() {
  const events = await authenticatedGet<EventView[]>("events/organizer/mine");
  return <main className="container-shell py-10"><div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Área do organizador</p><h1 className="mt-1 text-4xl font-black tracking-[-.045em]">Seus eventos</h1><p className="mt-2 text-slate-500">Acompanhe inscrições e deixe a entrada fluir.</p></div><Link href="/organizador/eventos/novo" className={buttonVariants({ variant: "primary", size: "lg" })}><CalendarPlus className="size-5" /> Criar evento</Link></div>
    <div className="mt-8 grid gap-4">{events.map((event) => <Link key={event.id} href={`/organizador/eventos/${event.id}`} className="focus-ring surface group grid gap-4 p-4 sm:grid-cols-[120px_1fr_auto] sm:items-center"><div className="event-image aspect-[4/3] overflow-hidden rounded-xl">{event.coverUrl && <img src={event.coverUrl} alt="" className="h-full w-full object-cover" />}</div><div><div className="flex flex-wrap items-center gap-2"><Badge className={event.status === "DRAFT" ? "bg-amber-50 text-amber-700" : ""}>{statusLabel[event.status]}</Badge><span className="text-xs font-semibold text-slate-400">{formatShortDate(event.startsAt)}</span></div><h2 className="mt-2 text-xl font-black">{event.title}</h2><p className="mt-1 flex items-center gap-1.5 text-sm text-slate-500"><MapPin className="size-4" />{event.city}, {event.state}</p><p className="mt-2 flex items-center gap-1.5 text-sm font-semibold"><Users className="size-4 text-[var(--accent)]" />{event.confirmedCount} de {event.capacity} inscritos</p></div><ChevronRight className="hidden size-6 text-slate-300 transition group-hover:translate-x-1 group-hover:text-[var(--primary)] sm:block" /></Link>)}</div>
  </main>;
}

