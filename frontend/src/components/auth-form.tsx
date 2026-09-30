"use client";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { Problem } from "@/lib/types";

const formSchema = z.object({
  email: z.email("Informe um e-mail válido"),
  password: z.string().min(8, "Use ao menos 8 caracteres"),
  name: z.string().max(120).optional(),
  organizer: z.boolean(),
});
type FormData = z.infer<typeof formSchema>;

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter(); const search = useSearchParams();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(formSchema), defaultValues: { name: "", organizer: false } });
  async function submit(data: FormData) {
    if (mode === "register" && (!data.name || data.name.trim().length < 2)) { toast.error("Informe seu nome completo."); return; }
    const response = await fetch(`/api/auth/${mode === "login" ? "login" : "register"}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(data) });
    if (!response.ok) { const problem = await response.json() as Problem; toast.error(problem.detail ?? "Não foi possível continuar."); return; }
    const session = await response.json() as { roles: string[] };
    toast.success(mode === "login" ? "Bem-vindo de volta!" : "Sua conta foi criada.");
    const requested = search.get("next");
    const safeNext = requested?.startsWith("/") && !requested.startsWith("//") && !requested.includes("\\") ? requested : null;
    router.push(safeNext || (session.roles.includes("ORGANIZER") ? "/organizador/eventos" : "/meus-ingressos")); router.refresh();
  }
  return <form onSubmit={handleSubmit(submit)} className="grid gap-4">
    {mode === "register" && <label className="grid gap-1.5 text-sm font-bold">Nome completo<Input autoComplete="name" {...register("name")} /><span className="text-xs font-normal text-red-600">{errors.name?.message}</span></label>}
    <label className="grid gap-1.5 text-sm font-bold">E-mail<Input type="email" autoComplete="email" {...register("email")} /><span className="text-xs font-normal text-red-600">{errors.email?.message}</span></label>
    <label className="grid gap-1.5 text-sm font-bold">Senha<Input type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} {...register("password")} /><span className="text-xs font-normal text-red-600">{errors.password?.message}</span></label>
    {mode === "register" && <label className="flex items-start gap-3 rounded-xl border border-slate-200 p-4"><input type="checkbox" className="mt-1 size-4 accent-[var(--primary)]" {...register("organizer")} /><span><strong className="block text-sm">Quero organizar eventos</strong><span className="text-sm text-slate-500">Você também poderá participar de outros eventos.</span></span></label>}
    <Button type="submit" size="lg" disabled={isSubmitting}>{isSubmitting ? "Aguarde..." : mode === "login" ? "Entrar na minha conta" : "Criar minha conta"}</Button>
  </form>;
}

