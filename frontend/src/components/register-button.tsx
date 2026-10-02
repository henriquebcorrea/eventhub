"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { TicketCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { EventView, Problem } from "@/lib/types";

export function RegisterButton({ eventId, types }: { eventId: string; types: NonNullable<EventView["ticketTypes"]> }) {
  const router = useRouter();
  const [typeId, setTypeId] = useState(types[0]?.id ?? "");
  const [names, setNames] = useState<string[]>([""]);
  const [pending, setPending] = useState(false);
  const selected = types.find((type) => type.id === typeId) ?? types[0];
  const queueRequired = !!selected && (selected.available < names.length || selected.waitingCount > 0);
  const valid = names.every((name) => name.trim().length > 0 && name.trim().length <= 120);

  async function submit(waitlist: boolean) {
    if (!valid || !selected) { toast.error("Informe o nome de cada participante."); return; }
    setPending(true);
    try {
      const response = await fetch(waitlist ? `/api/backend/events/${eventId}/waitlist` : `/api/backend/events/${eventId}/registrations`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ticketTypeId: selected.id, attendeeNames: names.map((name) => name.trim()) }),
      });
      if (response.status === 401) { router.push(`/entrar?next=${encodeURIComponent(window.location.pathname)}`); return; }
      if (!response.ok) {
        const problem = await response.json() as Problem;
        toast.error(problem.detail ?? "Não foi possível concluir o pedido.");
        router.refresh();
        return;
      }
      const result = await response.json();
      if (waitlist) {
        toast.success("Pedido incluído na lista de espera. Acompanhe em Meus ingressos.");
        router.push("/meus-ingressos");
      } else {
        const firstId = result.tickets?.[0]?.id ?? result.ticket?.id;
        toast.success(names.length === 1 ? "Ingresso confirmado!" : `${names.length} ingressos confirmados!`);
        router.push(firstId ? `/ingressos/${firstId}` : "/meus-ingressos");
      }
      router.refresh();
    } catch { toast.error("Não foi possível conectar ao servidor."); }
    finally { setPending(false); }
  }

  return <div className="grid gap-4">
    <label className="grid gap-1.5 text-sm font-bold">Tipo de ingresso
      <select className="focus-ring h-11 rounded-sm border border-slate-200 bg-white px-3 text-[var(--navy)]" value={typeId} onChange={(event) => setTypeId(event.target.value)}>
        {types.map((type) => <option key={type.id} value={type.id}>{type.name} · {type.available} vagas{type.waitingCount ? " · fila ativa" : ""}</option>)}
      </select>
    </label>
    <label className="grid gap-1.5 text-sm font-bold">Quantidade (até 4)
      <select className="focus-ring h-11 rounded-sm border border-slate-200 bg-white px-3 text-[var(--navy)]" value={names.length} onChange={(event) => {
        const quantity = Number(event.target.value);
        setNames(Array.from({ length: quantity }, (_, index) => names[index] ?? ""));
      }}>{[1, 2, 3, 4].map((quantity) => <option key={quantity} value={quantity}>{quantity} {quantity === 1 ? "ingresso" : "ingressos"}</option>)}</select>
    </label>
    <div className="grid gap-2">{names.map((name, index) => <label key={index} className="grid gap-1 text-sm font-bold">Nome no ingresso {index + 1}
      <Input maxLength={120} autoComplete={index === 0 ? "name" : "off"} value={name} onChange={(event) => setNames(names.map((item, i) => i === index ? event.target.value : item))} placeholder="Nome do participante" />
    </label>)}</div>
    {queueRequired && <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Não há vagas para todo o grupo agora ou este tipo já possui fila. A ordem dos pedidos será respeitada; a promoção é automática quando houver vagas para o grupo inteiro.</p>}
    <Button size="lg" className="w-full" disabled={pending || !valid || queueRequired} onClick={() => submit(false)}><TicketCheck className="size-5" />{pending && !queueRequired ? "Confirmando..." : "Garantir ingressos gratuitos"}</Button>
    {queueRequired && <Button size="lg" variant="secondary" className="w-full" disabled={pending || !valid} onClick={() => submit(true)}>{pending ? "Entrando na fila..." : "Entrar na lista de espera"}</Button>}
  </div>;
}
