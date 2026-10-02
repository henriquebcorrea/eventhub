import Link from "next/link";
import { ArrowUpRight, MapPin, Ticket } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { EventView } from "@/lib/types";
import { formatShortDate } from "@/lib/utils";

export function EventCard({ event, featured = false }: { event: EventView; featured?: boolean }) {
  const fill = Math.min(100, Math.round((event.confirmedCount / event.capacity) * 100));
  const isDemo = event.description.toLowerCase().includes("evento fictício para demonstração");
  return <article className={`group lift-card overflow-hidden border border-[var(--border)] bg-[var(--surface)] ${featured ? "lg:col-span-2" : ""}`}>
    <Link href={`/eventos/${event.slug}`} className={`focus-ring block h-full ${featured ? "lg:grid lg:grid-cols-[1.12fr_.88fr]" : ""}`}>
      <div className={`event-image relative overflow-hidden ${featured ? "aspect-[16/10] lg:aspect-auto lg:min-h-[370px]" : "aspect-[16/10]"}`}>
        {event.coverUrl && <img src={event.coverUrl} alt="" loading={featured ? "eager" : "lazy"} className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.035]" />}
        <div className="absolute inset-0 bg-gradient-to-t from-[var(--navy)]/75 via-transparent to-transparent" />
        <div className="absolute left-4 top-4 flex flex-wrap gap-2"><Badge className="bg-[var(--accent)] text-[var(--navy)]">Entrada gratuita</Badge>{isDemo && <Badge className="bg-[var(--paper)] text-[var(--navy)]">Evento fictício</Badge>}</div>
        <time className="absolute bottom-4 left-4 border border-white/70 bg-[var(--navy)] px-3 py-2 font-mono text-xs font-bold uppercase tracking-[.08em] text-white" dateTime={event.startsAt}>{formatShortDate(event.startsAt)}</time>
      </div>
      <div className={`flex flex-col p-5 sm:p-6 ${featured ? "lg:justify-between lg:p-8" : ""}`}>
        <div>{featured && <p className="eyebrow mb-5 text-[var(--primary)]">Em destaque / 01</p>}<div className="flex items-start justify-between gap-3"><h3 className={`font-extrabold leading-[1.05] tracking-[-.055em] text-[var(--navy)] ${featured ? "text-3xl sm:text-4xl" : "text-2xl"}`}>{event.title}</h3><span className="grid size-9 shrink-0 place-items-center border border-[var(--navy)] transition group-hover:bg-[var(--accent)]"><ArrowUpRight className="size-5" /></span></div><p className="mt-4 flex items-start gap-2 text-sm font-medium text-slate-600"><MapPin className="mt-0.5 size-4 shrink-0 text-[var(--primary)]" />{event.venue} · {event.city}, {event.state}</p></div>
        <div className="mt-7 border-t border-[var(--border)] pt-4"><div className="mb-2 flex justify-between gap-2 text-xs font-bold"><span className="flex items-center gap-1.5"><Ticket className="size-4" /> {event.available} vagas</span><span className="text-slate-500">{fill}% preenchido</span></div><div className="h-1 overflow-hidden bg-[var(--surface-muted)]"><div className="h-full bg-[var(--primary)]" style={{ width: `${fill}%` }} /></div></div>
      </div>
    </Link>
  </article>;
}
