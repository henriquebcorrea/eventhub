import { EventForm } from "@/components/event-form";
import { OrganizerHeading } from "@/components/organizer-heading";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";
export const metadata = { title: "Criar evento" };
export default async function NewEventPage() { await authenticatedGet<EventView[]>("events/organizer/mine", "/organizador/eventos/novo"); return <main className="container-shell py-8 sm:py-12"><div className="mx-auto max-w-4xl"><OrganizerHeading eyebrow="Backstage / Novo evento" title="Transforme a ideia em experiência" description="Preencha as informações essenciais. Você poderá revisar tudo antes de publicar." /><section className="mt-5 border border-[var(--border)] bg-[var(--surface)] p-5 sm:p-8"><EventForm /></section></div></main>; }

