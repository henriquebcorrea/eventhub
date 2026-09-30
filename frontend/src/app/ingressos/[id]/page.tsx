import { CalendarDays, MapPin, ShieldCheck } from "lucide-react";
import { TicketQr } from "@/components/ticket-qr";
import { CancelRegistration } from "@/components/cancel-registration";
import { Badge } from "@/components/ui/badge";
import { authenticatedGet } from "@/lib/server-api";
import type { TicketView } from "@/lib/types";
import { formatDate } from "@/lib/utils";
export const metadata = { title: "Ingresso" };
export default async function TicketPage({ params }: { params: Promise<{ id: string }> }) {
  const id = (await params).id;
  const ticket = await authenticatedGet<TicketView>(`tickets/${id}`, `/ingressos/${id}`);
  const active = ticket.status === "ACTIVE" && ticket.eventStatus === "PUBLISHED";
  return <main className="container-shell py-10"><div className="mx-auto max-w-xl overflow-hidden rounded-3xl bg-[var(--navy)] text-white shadow-2xl"><div className="event-image relative h-52">{ticket.coverUrl && <img src={ticket.coverUrl} alt="" className="h-full w-full object-cover" />}<div className="absolute inset-0 bg-gradient-to-t from-[var(--navy)] to-transparent" /><div className="absolute bottom-5 left-6 right-6"><Badge>{active ? "Ingresso válido" : "Ingresso cancelado"}</Badge><h1 className="mt-3 text-3xl font-black tracking-[-.04em]">{ticket.eventTitle}</h1></div></div><div className="px-6 pb-7"><div className="grid gap-2 border-b border-white/15 pb-6 text-sm text-slate-300"><p className="flex items-center gap-2"><CalendarDays className="size-4 text-[var(--primary)]" />{formatDate(ticket.startsAt)}</p><p className="flex items-center gap-2"><MapPin className="size-4 text-[var(--primary)]" />{ticket.venue} · {ticket.city}, {ticket.state}</p></div><div className="py-7 text-center">{active ? <><TicketQr value={ticket.qrPayload} /><p className="mt-4 font-mono text-sm font-bold tracking-[.1em]">{ticket.publicCode}</p><p className="mt-2 text-sm text-slate-400">Apresente este QR Code na entrada.</p></> : <p className="rounded-xl bg-red-950/50 p-5 text-sm text-red-100">Este ingresso não é mais válido para entrada.</p>}</div>{active && <><div className="flex items-center justify-center gap-2 rounded-xl bg-white/8 p-3 text-sm text-slate-300"><ShieldCheck className="size-4 text-teal-300" /> Este ingresso permite apenas um check-in.</div><div className="mt-5 flex justify-center"><CancelRegistration registrationId={ticket.registrationId} /></div></>}</div></div></main>;
}

