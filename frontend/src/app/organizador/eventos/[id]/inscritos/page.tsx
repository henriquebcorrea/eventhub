import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { OrganizerHeading } from "@/components/organizer-heading";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView, PageView } from "@/lib/types";
import { formatDate } from "@/lib/utils";

interface Attendee { registrationId: string; ticketId: string; ticketTypeName: string; name: string; email: string; status: "CONFIRMED" | "CANCELLED"; registeredAt: string; }
export const metadata = { title: "Inscritos" };

export default async function AttendeesPage({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ page?: string }> }) {
  const { id } = await params;
  const { page: rawPage = "0" } = await searchParams;
  const page = Math.min(100000, Math.max(0, Number.parseInt(rawPage, 10) || 0));
  const returnTo = `/organizador/eventos/${id}/inscritos?page=${page}`;
  const events = await authenticatedGet<EventView[]>("events/organizer/mine", returnTo);
  const event = events.find((item) => item.id === id);
  if (!event) notFound();
  const attendees = await authenticatedGet<PageView<Attendee>>(`organizer/events/${id}/attendees?page=${page}&size=25`, returnTo);
  return <main className="container-shell py-8 sm:py-12"><Link href={`/organizador/eventos/${id}`} className="focus-ring mb-5 inline-flex min-h-11 items-center gap-2 text-sm font-bold text-slate-600 hover:text-[var(--primary)]"><ArrowLeft className="size-4" /> Voltar ao painel</Link><OrganizerHeading eyebrow="Backstage / Pessoas" title="Inscritos" description={`${event.title} · ${attendees.totalElements} ingressos emitidos`} />
    <div className="mt-5 hidden overflow-x-auto border border-[var(--border)] bg-[var(--surface)] md:block"><table className="w-full min-w-[720px] text-left text-sm"><thead className="bg-[var(--navy)] text-[var(--paper)]"><tr><th className="px-5 py-4">Nome no ingresso</th><th className="px-5 py-4">Tipo</th><th className="px-5 py-4">E-mail do titular</th><th className="px-5 py-4">Emissão</th><th className="px-5 py-4">Situação</th></tr></thead><tbody className="divide-y divide-[var(--border)]">{attendees.content.map((person) => <tr key={person.ticketId ?? person.registrationId}><td className="px-5 py-4 font-bold">{person.name}</td><td className="px-5 py-4">{person.ticketTypeName ?? "Ingresso geral"}</td><td className="px-5 py-4">{person.email}</td><td className="px-5 py-4">{formatDate(person.registeredAt)}</td><td className="px-5 py-4"><Badge className={person.status === "CANCELLED" ? "bg-red-100 text-red-800" : ""}>{person.status === "CONFIRMED" ? "Confirmado" : "Cancelado"}</Badge></td></tr>)}</tbody></table></div>
    <div className="mt-5 grid gap-3 md:hidden">{attendees.content.map((person) => <article key={person.ticketId ?? person.registrationId} className="border border-[var(--border)] bg-[var(--surface)] p-4"><div className="flex items-start justify-between gap-2"><h2 className="min-w-0 font-display text-lg font-extrabold">{person.name}</h2><Badge className={person.status === "CANCELLED" ? "bg-red-100 text-red-800" : ""}>{person.status === "CONFIRMED" ? "Confirmado" : "Cancelado"}</Badge></div><p className="mt-2 text-sm font-bold">{person.ticketTypeName ?? "Ingresso geral"}</p><p className="break-all text-sm text-slate-600">{person.email}</p><p className="mt-3 border-t border-[var(--border)] pt-3 text-xs text-slate-600">Emitido em {formatDate(person.registeredAt)}</p></article>)}</div>
    {!attendees.content.length && <p className="border border-[var(--border)] bg-[var(--surface)] p-8 text-center text-slate-600">Nenhum ingresso nesta página.</p>}
    <nav aria-label="Páginas de inscritos" className="mt-7 flex flex-wrap items-center justify-center gap-4 text-sm font-bold">{page > 0 && <Link href={`/organizador/eventos/${id}/inscritos?page=${page - 1}`} className="border border-[var(--navy)] px-4 py-2">Anterior</Link>}<span>Página {page + 1} de {Math.max(1, attendees.totalPages)}</span>{page + 1 < attendees.totalPages && <Link href={`/organizador/eventos/${id}/inscritos?page=${page + 1}`} className="border border-[var(--navy)] px-4 py-2">Próxima</Link>}</nav>
  </main>;
}
