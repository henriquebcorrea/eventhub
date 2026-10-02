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
  return <article className="surface p-5"><span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-900">Na lista de espera · posição {request.position}</span><h2 className="mt-4 text-xl font-black"><Link href={`/eventos/${request.eventSlug}`}>{request.eventTitle}</Link></h2><p className="mt-2 text-sm text-slate-600">{request.ticketTypeName} · {request.attendeeNames.length} {request.attendeeNames.length === 1 ? "ingresso" : "ingressos"}</p><p className="mt-1 text-sm text-slate-500">{request.attendeeNames.join(", ")}</p><p className="mt-3 text-xs text-slate-500">O grupo só será confirmado quando houver vagas para todos. Os QR Codes aparecerão aqui automaticamente.</p><Button variant="secondary" className="mt-4" disabled={pending} onClick={leave}>{pending ? "Saindo..." : "Sair da fila"}</Button></article>;
}
