"use client";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { TicketCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import type { Problem } from "@/lib/types";

export function RegisterButton({ eventId, soldOut }: { eventId: string; soldOut: boolean }) {
  const router = useRouter();
  const registration = useMutation({
    mutationFn: async (): Promise<{ ticket: { id: string } } | null> => {
      const response = await fetch(`/api/backend/events/${eventId}/registrations`, { method: "POST" });
      if (response.status === 401) { router.push(`/entrar?next=${encodeURIComponent(window.location.pathname)}`); return null; }
      if (!response.ok) { const problem = await response.json() as Problem; throw new Error(problem.detail ?? "Não foi possível concluir a inscrição."); }
      return response.json();
    },
    onSuccess: (data) => { if (!data) return; toast.success("Inscrição confirmada! Seu ingresso já está disponível."); router.push(`/ingressos/${data.ticket.id}`); },
    onError: (error) => toast.error(error.message),
  });
  return <Button size="lg" className="w-full" onClick={() => registration.mutate()} disabled={soldOut || registration.isPending}><TicketCheck className="size-5" /> {soldOut ? "Ingressos esgotados" : registration.isPending ? "Confirmando..." : "Garantir ingresso gratuito"}</Button>;
}

