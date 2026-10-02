import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { CheckInScanner } from "@/components/checkin-scanner";
import { OrganizerHeading } from "@/components/organizer-heading";
import { authenticatedGet } from "@/lib/server-api";
import type { EventView } from "@/lib/types";
import { notFound } from "next/navigation";
export const metadata = { title: "Check-in" };
export default async function CheckInPage({ params }: { params: Promise<{ id: string }> }) { const id = (await params).id; const events = await authenticatedGet<EventView[]>("events/organizer/mine", `/organizador/eventos/${id}/check-in`); if (!events.some((event) => event.id === id && event.status === "PUBLISHED")) notFound(); return <main className="container-shell py-8 sm:py-12"><Link href={`/organizador/eventos/${id}`} className="focus-ring mb-5 inline-flex min-h-11 items-center gap-2 text-sm font-bold text-slate-600 hover:text-[var(--primary)]"><ArrowLeft className="size-4" /> Voltar ao painel</Link><OrganizerHeading eyebrow="Portaria / Check-in" title="Validar ingresso" description="Aponte a câmera para o QR Code do participante. A resposta aparece imediatamente abaixo." /><section className="mt-6"><CheckInScanner eventId={id} /></section></main>; }
