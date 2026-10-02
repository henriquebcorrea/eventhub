"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import type { Problem, WaitlistView } from "@/lib/types";

export function WaitlistCard({ request }: { request: WaitlistView }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  async function leave() {
    if (!window.confirm("Sair da lista de espera? Para entrar novamente, você irá para o fim da fila.")) return;
    setPending(true);
    try {
      const response = await fetch(`/api/backend/waitlist/${request.id}`, { method: "DELETE" });
      if (!response.ok) { const problem = await response.json() as Problem; toast.error(problem.detail ?? "Não foi possível sair da fila."); return; }
      toast.success("Você saiu da lista de espera."); router.refresh();
    } catch { toast.error("Não foi possível conectar ao servidor."); }
    finally { setPending(false); }
  }
  return <article className="border border-[var(--border)] bg-[var(--surface)] p-5 sm:p-6"><span className="poster-label border-[var(--primary)] text-[var(--primary)]">Na lista de espera · posição {request.position}</span><h2 className="mt-5 font-display text-2xl font-extrabold leading-tight tracking-[-.05em]"><Link className="hover:text-[var(--primary)]" href={`/eventos/${request.eventSlug}`}>{request.eventTitle}</Link></h2><p className="mt-3 text-sm font-bold text-slate-700">{request.ticketTypeName} · {request.attendeeNames.length} {request.attendeeNames.length === 1 ? "ingresso" : "ingressos"}</p><p className="mt-1 text-sm text-slate-600">{request.attendeeNames.join(", ")}</p><p className="mt-5 border-t border-[var(--border)] pt-4 text-xs text-slate-600">O grupo só será confirmado quando houver vagas para todos. Os QR Codes aparecerão aqui automaticamente.</p><Button variant="secondary" className="mt-4" disabled={pending} onClick={leave}>{pending ? "Saindo..." : "Sair da fila"}</Button></article>;
}
