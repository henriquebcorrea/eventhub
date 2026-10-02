"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { Problem } from "@/lib/types";

export function TicketActions({ ticketId, attendeeName, canEdit }: { ticketId: string; attendeeName: string; canEdit: boolean }) {
  const router = useRouter();
  const [name, setName] = useState(attendeeName);
  const [loading, setLoading] = useState(false);
  async function request(path: string, method: string, body?: object) {
    setLoading(true);
    try {
      const response = await fetch(path, { method, headers: body ? { "Content-Type": "application/json" } : undefined, body: body ? JSON.stringify(body) : undefined });
      if (!response.ok) { const problem = await response.json() as Problem; toast.error(problem.detail ?? "Não foi possível concluir a operação."); return; }
      toast.success(method === "PATCH" ? "Nome atualizado." : "Ingresso cancelado.");
      router.refresh();
    } catch { toast.error("Não foi possível conectar ao servidor."); }
    finally { setLoading(false); }
  }
  if (!canEdit) return null;
  return <div className="mt-5 grid gap-3">
    <label className="grid gap-1.5 text-sm font-bold">Nome no ingresso
      <Input className="bg-white text-slate-900" maxLength={120} value={name} onChange={(event) => setName(event.target.value)} />
    </label>
    <div className="flex flex-wrap justify-center gap-2">
      <Button variant="secondary" disabled={loading || !name.trim() || name.trim() === attendeeName} onClick={() => request(`/api/backend/tickets/${ticketId}/attendee`, "PATCH", { name: name.trim() })}>Salvar nome</Button>
      <Button variant="secondary" disabled={loading} onClick={() => { if (window.confirm("Cancelar apenas este ingresso? O QR Code deixará de ser válido.")) request(`/api/backend/tickets/${ticketId}`, "DELETE"); }}>Cancelar este ingresso</Button>
    </div>
  </div>;
}
