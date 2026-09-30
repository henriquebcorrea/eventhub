import Link from "next/link";
import { ArrowUpRight, MapPin, Ticket } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { EventView } from "@/lib/types";
import { formatShortDate } from "@/lib/utils";

export function EventCard({ event }: { event: EventView }) {
  const fill = Math.min(100, Math.round((event.confirmedCount / event.capacity) * 100));
  return (
    <article className="group surface overflow-hidden transition duration-300 hover:-translate-y-1 hover:shadow-xl">
      <Link href={`/eventos/${event.slug}`} className="focus-ring block rounded-[1.25rem]">
        <div className="event-image relative aspect-[16/10] overflow-hidden">
          {event.coverUrl && <img src={event.coverUrl} alt="" className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]" />}
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950/65 via-transparent to-transparent" />
          <Badge className="absolute left-4 top-4 bg-white/95 text-slate-800">Entrada gratuita</Badge>
          <time className="absolute bottom-4 left-4 rounded-xl bg-white px-3 py-2 text-sm font-black text-slate-900" dateTime={event.startsAt}>{formatShortDate(event.startsAt)}</time>
        </div>
        <div className="p-5">
          <div className="flex items-start justify-between gap-4">
            <h2 className="text-xl font-black leading-tight tracking-[-.03em] text-slate-950">{event.title}</h2>
            <ArrowUpRight className="mt-1 size-5 shrink-0 text-slate-400 transition group-hover:text-[var(--primary)]" />
          </div>
          <p className="mt-3 flex items-center gap-2 text-sm text-slate-600"><MapPin className="size-4 text-[var(--primary)]" /> {event.venue} · {event.city}, {event.state}</p>
          <div className="mt-5">
            <div className="mb-2 flex justify-between text-xs font-semibold text-slate-500"><span className="flex items-center gap-1"><Ticket className="size-3.5" /> {event.available} vagas</span><span>{fill}% preenchido</span></div>
            <div className="h-1.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-[var(--accent)]" style={{ width: `${fill}%` }} /></div>
          </div>
        </div>
      </Link>
    </article>
  );
}

