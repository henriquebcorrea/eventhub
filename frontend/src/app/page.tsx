import Link from "next/link";
import { ArrowDownRight, ArrowRight, CalendarDays, MapPin, Search } from "lucide-react";
import { EventCard } from "@/components/event-card";
import { getEvents } from "@/lib/api";
import type { EventView } from "@/lib/types";

const demoSlugs = new Set(["festival-aurora", "summit-criativo", "jazz-no-jardim", "feira-de-sabores", "corrida-orla-viva", "cinema-ao-ar-livre"]);
const featuredCovers = ["festival-aurora", "summit-criativo", "jazz-no-jardim"];
const isShowcase = (event: EventView) => demoSlugs.has(event.slug);

export default async function Home({ searchParams }: { searchParams: Promise<{ query?: string; city?: string; from?: string; page?: string }> }) {
  const { query = "", city = "", from = "", page: rawPage = "0" } = await searchParams;
  const page = Math.min(100000, Math.max(0, Number.parseInt(rawPage, 10) || 0));
  const events = await getEvents(query, city, from, page);
  const unfiltered = !query && !city && !from && page === 0;
  const displayed = unfiltered ? [...events.content].sort((a, b) => Number(isShowcase(b)) - Number(isShowcase(a))) : events.content;
  const pageUrl = (target: number) => `/?${new URLSearchParams({ ...(query ? { query } : {}), ...(city ? { city } : {}), ...(from ? { from } : {}), page: String(target) })}#eventos`;

  return <main>
    <section className="relative overflow-hidden bg-[var(--navy)] text-[var(--paper)]">
      <div aria-hidden="true" className="poster-grid absolute inset-0 opacity-35" />
      <div className="container-shell relative grid gap-10 pb-14 pt-12 lg:min-h-[620px] lg:grid-cols-[1.05fr_.95fr] lg:items-center lg:gap-16 lg:py-20">
        <div className="relative z-10 min-w-0">
          <p className="eyebrow flex items-center gap-3 text-[var(--accent)]"><span className="inline-block size-2 bg-[var(--primary)]" /> A agenda que tira você de casa</p>
          <h1 className="display-tight mt-7 max-w-[11ch] text-[clamp(2.45rem,11vw,7.5rem)] font-extrabold">A noite <span className="text-[var(--accent)]">acontece</span> lá fora<span className="text-[var(--primary)]">.</span></h1>
          <p className="mt-7 max-w-lg text-lg leading-relaxed text-white/70 sm:text-xl">Shows, ideias e encontros que merecem mais do que uma curtida. Descubra o próximo e esteja lá.</p>
          <div className="mt-8 flex flex-wrap items-center gap-4"><a href="#eventos" className="focus-ring inline-flex min-h-13 items-center gap-3 bg-[var(--accent)] px-6 font-extrabold text-[var(--navy)] hover:bg-[#dcffa0]">Explorar eventos <ArrowDownRight className="size-5" /></a><Link href="/organizador/eventos" className="focus-ring inline-flex min-h-13 items-center gap-2 border-b border-white/60 px-1 font-bold text-white hover:border-[var(--accent)] hover:text-[var(--accent)]">Quero organizar <ArrowRight className="size-4" /></Link></div>
          <div className="mt-12 flex items-center gap-3 border-t border-white/20 pt-5 text-xs font-bold uppercase tracking-[.15em] text-white/50"><span className="text-[var(--accent)]">01 / 03</span><span className="h-px w-12 bg-white/30" /><span>Descubra · garanta · viva</span></div>
        </div>
        <div className="relative mx-auto hidden h-[500px] w-full max-w-[540px] sm:block" aria-hidden="true">
          <div className="absolute left-[5%] top-[7%] h-[72%] w-[64%] -rotate-6 overflow-hidden border-[8px] border-[var(--paper)] bg-slate-900 shadow-[20px_28px_0_#ff5a3d]"><img src={`/demo-covers/${featuredCovers[0]}.png`} alt="" className="h-full w-full object-cover" /></div>
          <div className="absolute right-[2%] top-[1%] h-[47%] w-[42%] rotate-8 overflow-hidden border-[7px] border-[var(--paper)] bg-slate-900 shadow-2xl"><img src={`/demo-covers/${featuredCovers[1]}.png`} alt="" className="h-full w-full object-cover" /></div>
          <div className="absolute bottom-[2%] right-[5%] h-[42%] w-[48%] rotate-3 overflow-hidden border-[7px] border-[var(--paper)] bg-slate-900 shadow-2xl"><img src={`/demo-covers/${featuredCovers[2]}.png`} alt="" className="h-full w-full object-cover" /></div>
          <div className="ticket-cut absolute bottom-[8%] left-[-1%] rotate-[-9deg] bg-[var(--accent)] px-6 py-4 text-[var(--navy)] shadow-xl"><span className="eyebrow block">O próximo capítulo</span><strong className="font-display text-2xl font-extrabold leading-none">é ao vivo.</strong></div>
          <span className="eyebrow absolute right-[-2%] top-[50%] hidden rotate-90 text-white/55 xl:block">EventHub / Em cartaz</span>
        </div>
        <div className="relative mx-auto grid w-full max-w-md grid-cols-2 gap-2 sm:hidden" aria-hidden="true"><img src="/demo-covers/festival-aurora.png" alt="" className="aspect-[4/3] w-full rotate-[-2deg] object-cover" /><img src="/demo-covers/summit-criativo.png" alt="" className="aspect-[4/3] w-full rotate-[2deg] object-cover" /></div>
      </div>
    </section>

    <section className="relative z-10 border-b border-[var(--border)] bg-[var(--paper)] py-6 sm:py-8" aria-label="Buscar eventos">
      <form action="/" method="get" className="container-shell grid gap-3 md:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)_minmax(0,.8fr)_auto] md:items-end">
        <label className="grid gap-1.5 text-xs font-extrabold uppercase tracking-[.12em]">O que você quer viver?<span className="relative"><Search aria-hidden="true" className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-500" /><input name="query" defaultValue={query} placeholder="Evento ou tema" className="focus-ring h-13 w-full rounded-sm border border-[var(--border)] bg-white pl-12 pr-4 text-base font-medium normal-case tracking-normal" /></span></label>
        <label className="grid gap-1.5 text-xs font-extrabold uppercase tracking-[.12em]">Onde?<span className="relative"><MapPin aria-hidden="true" className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-500" /><input name="city" defaultValue={city} placeholder="Cidade" className="focus-ring h-13 w-full rounded-sm border border-[var(--border)] bg-white pl-12 pr-4 text-base font-medium normal-case tracking-normal" /></span></label>
        <label className="grid gap-1.5 text-xs font-extrabold uppercase tracking-[.12em]">A partir de<input name="from" type="date" defaultValue={from} className="focus-ring h-13 w-full min-w-0 rounded-sm border border-[var(--border)] bg-white px-3 text-base font-medium normal-case tracking-normal" /></label>
        <button className="focus-ring flex min-h-13 items-center justify-center gap-2 rounded-sm bg-[var(--primary)] px-7 font-extrabold text-white hover:bg-[var(--primary-strong)]"><Search className="size-5" /> Buscar</button>
      </form>
    </section>

    <section id="eventos" className="container-shell py-14 sm:py-20">
      <div className="mb-9 flex flex-col gap-3 border-b border-[var(--navy)] pb-6 sm:flex-row sm:items-end sm:justify-between"><div><p className="eyebrow text-[var(--primary)]">Programação / Em cartaz</p><h2 className="display-tight mt-2 text-[clamp(2.5rem,5vw,4.5rem)] font-extrabold">Encontre seu lugar.</h2></div><p className="text-sm font-bold text-slate-600">{events.totalElements} {events.totalElements === 1 ? "evento encontrado" : "eventos encontrados"}</p></div>
      {displayed.length ? <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">{displayed.map((event, index) => <EventCard event={event} featured={unfiltered && index === 0 && isShowcase(event)} key={event.id} />)}</div> : <div className="surface grid min-h-64 place-items-center p-8 text-center"><div><CalendarDays className="mx-auto size-10 text-[var(--primary)]" /><h3 className="mt-3 text-2xl font-extrabold">Nada em cartaz para essa busca</h3><p className="mt-1 text-slate-600">Tente outro tema, cidade ou data.</p></div></div>}
      {events.totalPages > 1 && <nav aria-label="Páginas de eventos" className="mt-10 flex flex-wrap items-center justify-center gap-4 text-sm font-extrabold">{page > 0 && <Link className="border border-[var(--navy)] px-5 py-3 hover:bg-[var(--navy)] hover:text-white" href={pageUrl(page - 1)}>Anterior</Link>}<span>Página {page + 1} de {events.totalPages}</span>{page + 1 < events.totalPages && <Link className="border border-[var(--navy)] px-5 py-3 hover:bg-[var(--navy)] hover:text-white" href={pageUrl(page + 1)}>Próxima</Link>}</nav>}
    </section>

    <section className="bg-[var(--navy)] py-16 text-[var(--paper)] sm:py-20"><div className="container-shell"><p className="eyebrow text-[var(--accent)]">Simples assim</p><h2 className="display-tight mt-3 max-w-3xl text-4xl font-extrabold sm:text-6xl">Menos tela. <span className="text-[var(--primary)]">Mais história.</span></h2><div className="mt-10 grid border-t border-white/30 md:grid-cols-3">{[["01", "Descubra", "Encontre um evento que combina com o seu momento."], ["02", "Garanta", "Reserve seus ingressos gratuitos em poucos passos."], ["03", "Esteja lá", "Mostre o QR Code na entrada e aproveite."]].map(([number, title, detail]) => <div className="border-b border-white/30 py-6 md:border-b-0 md:border-r md:px-6 md:first:pl-0 md:last:border-r-0" key={number}><span className="eyebrow text-[var(--accent)]">{number} / 03</span><h3 className="mt-5 text-3xl font-extrabold">{title}</h3><p className="mt-2 max-w-xs text-white/60">{detail}</p></div>)}</div><Link href="/cadastro" className="focus-ring mt-10 inline-flex min-h-12 items-center gap-3 border-b border-[var(--accent)] font-extrabold text-[var(--accent)]">Começar agora <ArrowRight className="size-5" /></Link></div></section>
  </main>;
}
