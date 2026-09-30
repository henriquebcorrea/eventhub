import Link from "next/link";
import { notFound } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView, PageView } from "@/lib/types";
import { formatDate } from "@/lib/utils";

interface Attendee { registrationId: string; name: string; email: string; status: "CONFIRMED" | "CANCELLED"; registeredAt: string; }
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
  return <main className="container-shell py-10"><Link href={`/organizador/eventos/${id}`} className="text-sm font-bold text-[var(--primary)]">← Voltar ao painel</Link><h1 className="mt-5 text-4xl font-black tracking-[-.045em]">Inscritos</h1><p className="mt-2 text-slate-500">{event.title} · {attendees.totalElements} inscrições</p>
    <div className="surface mt-8 overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm"><thead className="bg-slate-50 text-slate-500"><tr><th className="px-5 py-4">Participante</th><th className="px-5 py-4">E-mail</th><th className="px-5 py-4">Inscrição</th><th className="px-5 py-4">Situação</th></tr></thead><tbody className="divide-y divide-slate-100">{attendees.content.map((person) => <tr key={person.registrationId}><td className="px-5 py-4 font-bold">{person.name}</td><td className="px-5 py-4">{person.email}</td><td className="px-5 py-4">{formatDate(person.registeredAt)}</td><td className="px-5 py-4"><Badge className={person.status === "CANCELLED" ? "bg-red-50 text-red-700" : ""}>{person.status === "CONFIRMED" ? "Confirmada" : "Cancelada"}</Badge></td></tr>)}</tbody></table>{!attendees.content.length && <p className="p-8 text-center text-slate-500">Nenhuma inscrição nesta página.</p>}</div>
    <nav aria-label="Páginas de inscritos" className="mt-6 flex items-center justify-center gap-4 text-sm font-bold">{page > 0 && <Link href={`/organizador/eventos/${id}/inscritos?page=${page - 1}`} className="rounded-xl border border-slate-200 px-4 py-2">Anterior</Link>}<span>Página {page + 1} de {Math.max(1, attendees.totalPages)}</span>{page + 1 < attendees.totalPages && <Link href={`/organizador/eventos/${id}/inscritos?page=${page + 1}`} className="rounded-xl border border-slate-200 px-4 py-2">Próxima</Link>}</nav>
  </main>;
}
