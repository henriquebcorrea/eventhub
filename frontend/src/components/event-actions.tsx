"use client";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Ban, Pencil, Rocket } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import type { Problem } from "@/lib/types";
export function EventActions({ eventId, status }: { eventId: string; status: "DRAFT" | "PUBLISHED" | "CANCELLED" | "COMPLETED" }) {
  const router = useRouter();
  async function publish() { const response = await fetch(`/api/backend/events/${eventId}/publish`, { method: "POST" }); if (!response.ok) { const p = await response.json() as Problem; toast.error(p.detail ?? "Não foi possível publicar."); return; } toast.success("Evento publicado!"); router.refresh(); }
  async function cancel() { if (!window.confirm("Cancelar este evento? Os ingressos deixarão de ser válidos.")) return; const response = await fetch(`/api/backend/events/${eventId}/cancel`, { method: "POST" }); if (!response.ok) { const p = await response.json() as Problem; toast.error(p.detail ?? "Não foi possível cancelar."); return; } toast.success("Evento cancelado."); router.refresh(); }
  return <div className="flex flex-wrap gap-2">{(status === "DRAFT" || status === "PUBLISHED") && <Link href={`/organizador/eventos/${eventId}/editar`} className="focus-ring inline-flex min-h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-5 text-sm font-bold hover:bg-slate-50"><Pencil className="size-4" /> Editar</Link>}{status === "DRAFT" && <Button onClick={publish}><Rocket className="size-4" /> Publicar evento</Button>}{status === "PUBLISHED" && <Button variant="secondary" onClick={cancel}><Ban className="size-4" /> Cancelar evento</Button>}</div>;
}

