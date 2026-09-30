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
  const fill = Math.min(100, Math.round(event.confirmedCount * 100 / event.capacity));
  return (
    <main>
      <section className="bg-[var(--navy)] py-8 text-white sm:py-12">
        <div className="container-shell grid gap-8 lg:grid-cols-[1.15fr_.85fr] lg:items-center">
          <div className="event-image relative aspect-[16/9] overflow-hidden rounded-3xl shadow-2xl">
            {event.coverUrl && <img src={event.coverUrl} alt="" className="h-full w-full object-cover" />}
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/55 to-transparent" />
          </div>
          <div>
            <Badge className="bg-teal-300 text-teal-950">Entrada gratuita</Badge>
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
        <aside className="surface sticky top-24 p-6 shadow-soft"><div className="flex items-center justify-between"><span className="text-sm font-semibold text-slate-500">Ingresso geral</span><span className="text-lg font-black text-teal-700">Gratuito</span></div><div className="my-5 h-px bg-slate-200" /><div className="mb-2 flex justify-between text-sm"><span className="flex items-center gap-2 font-semibold"><Ticket className="size-4 text-[var(--primary)]" /> Disponibilidade</span><span className="text-slate-500">{event.available} de {event.capacity}</span></div><div className="mb-6 h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-[var(--accent)]" style={{ width: `${fill}%` }} /></div><RegisterButton eventId={event.id} soldOut={event.available === 0} /><p className="mt-4 text-center text-xs text-slate-500">A confirmação e o ingresso aparecem imediatamente na sua conta.</p></aside>
      </div>
    </main>
  );
}

