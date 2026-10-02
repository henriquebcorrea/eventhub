import { notFound } from "next/navigation";
import { EventForm } from "@/components/event-form";
import { OrganizerHeading } from "@/components/organizer-heading";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";

export const metadata = { title: "Editar evento" };

export default async function EditEventPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const events = await authenticatedGet<EventView[]>("events/organizer/mine", `/organizador/eventos/${id}/editar`);
  const event = events.find((item) => item.id === id);
  if (!event) notFound();
  return <main className="container-shell py-8 sm:py-12"><div className="mx-auto max-w-4xl"><OrganizerHeading eyebrow="Backstage / Organização" title="Editar evento" description="Atualize os detalhes e salve as alterações." /><section className="mt-5 border border-[var(--border)] bg-[var(--surface)] p-5 sm:p-8"><EventForm event={event} /></section></div></main>;
}
