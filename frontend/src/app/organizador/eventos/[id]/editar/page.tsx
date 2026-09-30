import { notFound } from "next/navigation";
import { EventForm } from "@/components/event-form";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";

export const metadata = { title: "Editar evento" };

export default async function EditEventPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const events = await authenticatedGet<EventView[]>("events/organizer/mine", `/organizador/eventos/${id}/editar`);
  const event = events.find((item) => item.id === id);
  if (!event) notFound();
  return <main className="container-shell py-10"><div className="mx-auto max-w-3xl"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Organização</p><h1 className="mt-1 text-4xl font-black tracking-[-.045em]">Editar evento</h1><p className="mt-2 text-slate-500">Atualize os detalhes e salve as alterações.</p><section className="surface mt-8 p-6 shadow-soft sm:p-8"><EventForm event={event} /></section></div></main>;
}
