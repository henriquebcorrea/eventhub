import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { CalendarDays, Clock3, MapPin, ShieldCheck, Ticket } from "lucide-react";
import { RegisterButton } from "@/components/register-button";
import { Badge } from "@/components/ui/badge";
import { getEvent } from "@/lib/api";
import { formatDate } from "@/lib/utils";

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const event = await getEvent((await params).slug);
  return event ? { title: event.title, description: event.description.slice(0, 155) } : { title: "Evento não encontrado" };
}

export default async function EventDetail({ params }: { params: Promise<{ slug: string }> }) {
  const event = await getEvent((await params).slug);
  if (!event) notFound();
  const types = event.ticketTypes?.length ? event.ticketTypes : [{ id: event.ticketTypeId, name: "Ingresso geral", capacity: event.capacity, confirmedCount: event.confirmedCount, available: event.available, waitingCount: 0 }];
  const isDemo = event.description.toLowerCase().includes("evento fictício para demonstração");
  return (
    <main>
      <section className="bg-[var(--navy)] py-8 text-white sm:py-12">
        <div className="container-shell grid gap-8 lg:grid-cols-[1.15fr_.85fr] lg:items-center">
          <div className="event-image relative aspect-[16/9] overflow-hidden rounded-3xl shadow-2xl">
            {event.coverUrl && <img src={event.coverUrl} alt="" className="h-full w-full object-cover" />}
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/55 to-transparent" />
          </div>
          <div>
            <Badge className="bg-teal-300 text-teal-950">Entrada gratuita</Badge>{isDemo && <p className="mt-4 rounded-xl border border-amber-300 bg-amber-100 p-3 text-sm font-bold text-amber-950">Evento fictício para demonstração. O ingresso não dá acesso a um evento real.</p>}
            <h1 className="mt-4 text-balance text-4xl font-black leading-tight tracking-[-.05em] sm:text-5xl">{event.title}</h1>
            <div className="mt-6 grid gap-4 text-slate-200">
              <p className="flex items-start gap-3"><CalendarDays className="mt-0.5 size-5 text-[var(--primary)]" /><span><strong className="block text-white">Data e horário</strong>{formatDate(event.startsAt)}</span></p>
              <p className="flex items-start gap-3"><MapPin className="mt-0.5 size-5 text-[var(--primary)]" /><span><strong className="block text-white">Local</strong>{event.venue}<br />{event.address} · {event.city}, {event.state}</span></p>
            </div>
          </div>
        </div>
      </section>
      <div className="container-shell grid gap-8 py-10 lg:grid-cols-[1fr_370px] lg:items-start">
        <article className="surface p-6 sm:p-8"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Sobre o evento</p><h2 className="mt-2 text-2xl font-black tracking-[-.03em]">O que você vai encontrar</h2><p className="mt-5 whitespace-pre-line text-lg leading-8 text-slate-600">{event.description}</p><div className="mt-8 grid gap-4 border-t border-slate-200 pt-6 sm:grid-cols-2"><div className="flex gap-3"><Clock3 className="size-5 text-[var(--accent)]" /><div><strong className="block">Entrada ágil</strong><span className="text-sm text-slate-500">Apresente o QR Code no celular.</span></div></div><div className="flex gap-3"><ShieldCheck className="size-5 text-[var(--accent)]" /><div><strong className="block">Ingresso seguro</strong><span className="text-sm text-slate-500">Validação única na entrada.</span></div></div></div></article>
        <aside className="surface sticky top-24 p-6 shadow-soft"><div className="flex items-center justify-between"><span className="text-sm font-semibold text-slate-500">Escolha seus ingressos</span><span className="text-lg font-black text-teal-700">Gratuitos</span></div><div className="my-5 h-px bg-slate-200" /><div className="mb-4 grid gap-2">{types.map((type) => <div key={type.id} className="flex justify-between gap-2 text-sm"><span className="flex items-center gap-2 font-semibold"><Ticket className="size-4 text-[var(--primary)]" />{type.name}</span><span className="text-slate-500">{type.available} de {type.capacity}</span></div>)}</div><RegisterButton eventId={event.id} types={types} /><p className="mt-4 text-center text-xs text-slate-500">A confirmação e os QR Codes aparecem imediatamente na sua conta. A fila não garante ingresso até a promoção.</p></aside>
      </div>
    </main>
  );
}

