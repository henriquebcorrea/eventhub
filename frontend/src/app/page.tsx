import Link from "next/link";
import { CalendarSearch, MapPin, Search, ShieldCheck, Sparkles } from "lucide-react";
import { EventCard } from "@/components/event-card";
import { getEvents } from "@/lib/api";

export default async function Home({ searchParams }: { searchParams: Promise<{ query?: string; city?: string; from?: string; page?: string }> }) {
  const { query = "", city = "", from = "", page: rawPage = "0" } = await searchParams;
  const page = Math.min(100000, Math.max(0, Number.parseInt(rawPage, 10) || 0));
  const events = await getEvents(query, city, from, page);
  const pageUrl = (target: number) => `/?${new URLSearchParams({ ...(query ? { query } : {}), ...(city ? { city } : {}), ...(from ? { from } : {}), page: String(target) })}#eventos`;
  return (
    <main>
      <section className="relative overflow-hidden bg-[var(--navy)] text-white">
        <div className="absolute inset-0 opacity-30 [background-image:radial-gradient(circle_at_15%_30%,#17b6a6_0,transparent_26%),radial-gradient(circle_at_85%_10%,#ef5946_0,transparent_24%)]" />
        <div className="container-shell relative grid gap-10 py-16 lg:grid-cols-[1.05fr_.95fr] lg:items-end lg:py-20">
          <div>
            <span className="inline-flex items-center gap-2 text-sm font-bold uppercase tracking-[.15em] text-teal-300"><Sparkles className="size-4" /> Sua próxima experiência começa aqui</span>
            <h1 className="mt-4 max-w-3xl text-balance text-5xl font-black leading-[1.02] tracking-[-.055em] sm:text-6xl">Encontre um evento que vale sair de casa.</h1>
            <p className="mt-5 max-w-xl text-lg text-slate-300">Shows, encontros e experiências selecionadas. Inscrição simples, ingresso no celular e entrada sem fila.</p>
          </div>
          <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-md">
            <form className="grid gap-2 sm:grid-cols-[1fr_1fr_auto]" action="/" method="get">
              <label className="relative"><span className="sr-only">Buscar evento</span><Search className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-400" /><input name="query" defaultValue={query} placeholder="Evento ou tema" className="focus-ring h-13 w-full rounded-xl bg-white pl-12 pr-4 text-slate-950" /></label>
              <label className="relative"><span className="sr-only">Cidade</span><MapPin className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-400" /><input name="city" defaultValue={city} placeholder="Cidade" className="focus-ring h-13 w-full rounded-xl bg-white pl-12 pr-4 text-slate-950" /></label>
              <button className="focus-ring flex h-13 items-center justify-center gap-2 rounded-xl bg-[var(--primary)] px-6 font-bold hover:bg-[var(--primary-strong)]"><CalendarSearch className="size-5" /> Buscar</button>
            </form>
            <form action="/" method="get" className="mt-2 flex items-center gap-2 px-1 text-sm text-slate-200">
              <input type="hidden" name="query" value={query} /><input type="hidden" name="city" value={city} />
              <label htmlFor="from">A partir de</label><input id="from" name="from" type="date" defaultValue={from} className="focus-ring rounded-lg bg-white px-2 py-1 text-slate-900" /><button className="font-bold text-teal-200 hover:underline">Filtrar</button>
            </form>
            <p className="mt-3 flex items-center gap-2 px-2 text-xs text-slate-300"><ShieldCheck className="size-4 text-teal-300" /> Ingressos protegidos e check-in validado em tempo real</p>
          </div>
        </div>
      </section>

      <section id="eventos" className="container-shell py-14">
        <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Agenda selecionada</p><h2 className="mt-1 text-3xl font-black tracking-[-.04em] sm:text-4xl">Eventos para você</h2></div>
          <p className="text-sm text-slate-500">{events.totalElements} {events.totalElements === 1 ? "evento encontrado" : "eventos encontrados"}</p>
        </div>
        {events.content.length ? <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">{events.content.map((event) => <EventCard event={event} key={event.id} />)}</div> : <div className="surface grid min-h-64 place-items-center p-8 text-center"><div><CalendarSearch className="mx-auto size-10 text-slate-300" /><h3 className="mt-3 text-xl font-bold">Nenhum evento encontrado</h3><p className="mt-1 text-slate-500">Tente buscar outro tema ou cidade.</p></div></div>}
        {events.totalPages > 1 && <nav aria-label="Páginas de eventos" className="mt-8 flex items-center justify-center gap-4 text-sm font-bold">
          {page > 0 && <Link className="rounded-xl border border-slate-200 px-4 py-2 hover:bg-slate-50" href={pageUrl(page - 1)}>Anterior</Link>}
          <span>Página {page + 1} de {events.totalPages}</span>
          {page + 1 < events.totalPages && <Link className="rounded-xl border border-slate-200 px-4 py-2 hover:bg-slate-50" href={pageUrl(page + 1)}>Próxima</Link>}
        </nav>}
      </section>
    </main>
  );
}
