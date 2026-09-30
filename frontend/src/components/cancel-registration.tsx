"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import type { Problem } from "@/lib/types";

export function CancelRegistration({ registrationId }: { registrationId: string }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  async function cancel() {
    if (!window.confirm("Cancelar sua inscrição? Este ingresso deixará de ser válido.")) return;
    setLoading(true);
    try {
      const response = await fetch(`/api/backend/registrations/${registrationId}`, { method: "DELETE" });
      if (!response.ok) {
        const problem = await response.json() as Problem;
        toast.error(problem.detail ?? "Não foi possível cancelar a inscrição.");
        return;
      }
      toast.success("Inscrição cancelada.");
      router.refresh();
    } catch { toast.error("Não foi possível conectar ao servidor."); }
    finally { setLoading(false); }
  }
  return <Button variant="secondary" disabled={loading} onClick={cancel}>{loading ? "Cancelando..." : "Cancelar inscrição"}</Button>;
}
