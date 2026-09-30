import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { CheckInScanner } from "@/components/checkin-scanner";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";
import { notFound } from "next/navigation";
export const metadata = { title: "Check-in" };
export default async function CheckInPage({ params }: { params: Promise<{ id: string }> }) { const id = (await params).id; const events = await authenticatedGet<EventView[]>("events/organizer/mine", `/organizador/eventos/${id}/check-in`); if (!events.some((event) => event.id === id && event.status === "PUBLISHED")) notFound(); return <main className="container-shell py-8"><Link href={`/organizador/eventos/${id}`} className="inline-flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-slate-950"><ArrowLeft className="size-4" /> Voltar ao painel</Link><div className="mt-5"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Portaria</p><h1 className="mt-1 text-4xl font-black tracking-[-.045em]">Validar ingresso</h1><p className="mt-2 text-slate-500">Aponte a câmera para o QR Code do participante.</p></div><section className="mt-7"><CheckInScanner eventId={id} /></section></main>; }
