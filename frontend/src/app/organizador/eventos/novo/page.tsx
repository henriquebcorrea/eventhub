import { EventForm } from "@/components/event-form";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";
export const metadata = { title: "Criar evento" };
export default async function NewEventPage() { await authenticatedGet<EventView[]>("events/organizer/mine", "/organizador/eventos/novo"); return <main className="container-shell py-10"><div className="mx-auto max-w-3xl"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Novo evento</p><h1 className="mt-1 text-4xl font-black tracking-[-.045em]">Transforme a ideia em uma experiência</h1><p className="mt-2 text-slate-500">Preencha as informações essenciais. Você poderá revisar tudo antes de publicar.</p><section className="surface mt-8 p-6 shadow-soft sm:p-8"><EventForm /></section></div></main>; }

