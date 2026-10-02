"use client";
import { zodResolver } from "@hookform/resolvers/zod";
import { ImagePlus, LoaderCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import type { EventView, Problem } from "@/lib/types";

const schema = z.object({ title: z.string().min(3).max(140), description: z.string().min(30).max(5000), venue: z.string().min(2).max(160), address: z.string().min(5).max(220), city: z.string().min(2).max(120), state: z.string().length(2), timezone: z.string().min(1), startsAt: z.string().min(1), endsAt: z.string().min(1) });
type Values = z.infer<typeof schema>;
type TicketTypeDraft = { id?: string; name: string; capacity: number };

function dateInput(value: string, timezone: string) {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: timezone, year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date(value)).replace(" ", "T");
}

function zonedIso(value: string, timezone: string) {
  const desired = Date.parse(`${value}:00Z`);
  let guess = desired;
  for (let attempt = 0; attempt < 3; attempt++) {
    const local = dateInput(new Date(guess).toISOString(), timezone);
    guess += desired - Date.parse(`${local}:00Z`);
  }
  return new Date(guess).toISOString();
}

export function EventForm({ event }: { event?: EventView }) {
  const router = useRouter(); const [cover, setCover] = useState<{ url: string; publicId: string } | null>(event?.coverUrl && event.coverPublicId ? { url: event.coverUrl, publicId: event.coverPublicId } : null); const [uploading, setUploading] = useState(false);
  const [ticketTypes, setTicketTypes] = useState<TicketTypeDraft[]>(event?.ticketTypes?.length ? event.ticketTypes.map(({ id, name, capacity }) => ({ id, name, capacity })) : [{ id: event?.ticketTypeId, name: "Ingresso geral", capacity: event?.capacity ?? 100 }]);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: event ? { title: event.title, description: event.description, venue: event.venue, address: event.address, city: event.city, state: event.state, timezone: event.timezone, startsAt: dateInput(event.startsAt, event.timezone), endsAt: dateInput(event.endsAt, event.timezone) } : { timezone: "America/Sao_Paulo" } });
  async function upload(file?: File) {
    if (!file) return; if (file.size > 5 * 1024 * 1024) { toast.error("A imagem deve ter no máximo 5 MB."); return; }
    setUploading(true);
    const signatureResponse = await fetch("/api/backend/media/cloudinary-signature", { method: "POST" });
    if (!signatureResponse.ok) { toast.error("Upload indisponível. Você pode salvar o evento sem capa."); setUploading(false); return; }
    const signed = await signatureResponse.json(); const data = new FormData(); data.set("file", file); data.set("api_key", signed.apiKey); data.set("timestamp", String(signed.timestamp)); data.set("folder", signed.folder); data.set("allowed_formats", signed.allowedFormats); data.set("signature", signed.signature);
    const response = await fetch(`https://api.cloudinary.com/v1_1/${signed.cloudName}/image/upload`, { method: "POST", body: data });
    if (!response.ok) toast.error("Não foi possível enviar a imagem."); else { const image = await response.json(); setCover({ url: image.secure_url, publicId: image.public_id }); toast.success("Capa enviada."); }
    setUploading(false);
  }
  async function submit(values: Values) {
    if (ticketTypes.some((type) => !type.name.trim() || !Number.isInteger(type.capacity) || type.capacity < 1 || type.capacity > 100000) || new Set(ticketTypes.map((type) => type.name.trim().toLowerCase())).size !== ticketTypes.length) { toast.error("Informe nomes únicos e capacidades válidas para os ingressos."); return; }
    const response = await fetch(event ? `/api/backend/events/${event.id}` : "/api/backend/events", { method: event ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ...values, capacity: ticketTypes.reduce((total, type) => total + type.capacity, 0), ticketTypes: ticketTypes.map((type) => ({ ...type, name: type.name.trim() })), state: values.state.toUpperCase(), startsAt: zonedIso(values.startsAt, values.timezone), endsAt: zonedIso(values.endsAt, values.timezone), coverUrl: cover?.url, coverPublicId: cover?.publicId }) });
    if (!response.ok) { const problem = await response.json() as Problem; toast.error(problem.detail ?? "Não foi possível salvar o evento."); return; }
    const saved = await response.json() as EventView; toast.success(event ? "Evento atualizado." : "Rascunho criado."); router.push(`/organizador/eventos/${saved.id}`); router.refresh();
  }
  const fieldClass = "grid gap-1.5 text-sm font-bold";
  return <form className="grid gap-5" onSubmit={handleSubmit(submit)}>
    <p className="eyebrow border-b border-[var(--border)] pb-3 text-[var(--primary)]">01 / Informações do evento</p>
    <div className="grid gap-5 sm:grid-cols-2"><label className={`${fieldClass} sm:col-span-2`}>Nome do evento<Input placeholder="Ex.: Festival de Rock 2026" {...register("title")} />{errors.title && <span className="text-xs text-red-600">Informe um nome válido.</span>}</label><label className={`${fieldClass} sm:col-span-2`}>Descrição<Textarea placeholder="Conte ao público o que torna este evento especial." {...register("description")} />{errors.description && <span className="text-xs text-red-600">Use pelo menos 30 caracteres.</span>}</label><label className={fieldClass}>Local<Input placeholder="Arena, auditório..." {...register("venue")} /></label><label className={fieldClass}>Endereço<Input placeholder="Rua e número" {...register("address")} /></label><label className={fieldClass}>Cidade<Input placeholder="Florianópolis" {...register("city")} /></label><label className={fieldClass}>Estado<Input maxLength={2} placeholder="SC" {...register("state")} /></label><label className={fieldClass}>Fuso horário<select className="focus-ring h-11 rounded-xl border border-slate-200 bg-white px-3" {...register("timezone")}><option value="America/Sao_Paulo">Brasília (UTC−3)</option><option value="America/Manaus">Manaus (UTC−4)</option><option value="America/Rio_Branco">Rio Branco (UTC−5)</option><option value="America/Noronha">Fernando de Noronha (UTC−2)</option></select></label><label className={fieldClass}>Início<Input type="datetime-local" {...register("startsAt")} /></label><label className={fieldClass}>Término<Input type="datetime-local" {...register("endsAt")} /></label></div>
    <section className="grid gap-4 border border-[var(--border)] bg-[var(--paper)] p-5"><div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><div><p className="eyebrow text-[var(--primary)]">02 / Ingressos</p><h2 className="mt-2 font-display text-xl font-extrabold">Tipos de ingresso gratuitos</h2><p className="text-sm font-normal text-slate-600">Cada tipo tem sua própria capacidade e lista de espera.</p></div><Button type="button" variant="secondary" disabled={ticketTypes.length >= 10} onClick={() => setTicketTypes([...ticketTypes, { name: "", capacity: 50 }])}>Adicionar tipo</Button></div>{ticketTypes.map((type, index) => <div key={type.id ?? `new-${index}`} className="grid gap-2 border-t border-[var(--border)] pt-4 sm:grid-cols-[1fr_130px_auto]"><label className={fieldClass}>Nome<Input aria-label={`Nome do tipo ${index + 1}`} value={type.name} maxLength={100} onChange={(e) => setTicketTypes(ticketTypes.map((item, i) => i === index ? { ...item, name: e.target.value } : item))} /></label><label className={fieldClass}>Vagas<Input aria-label={`Vagas do tipo ${index + 1}`} type="number" min={1} max={100000} value={type.capacity} onChange={(e) => setTicketTypes(ticketTypes.map((item, i) => i === index ? { ...item, capacity: Number(e.target.value) } : item))} /></label><Button type="button" variant="secondary" className="self-end" disabled={ticketTypes.length === 1} onClick={() => setTicketTypes(ticketTypes.filter((_, i) => i !== index))}>Remover</Button></div>)}</section>
    <label className="focus-ring grid min-h-36 cursor-pointer place-items-center border-2 border-dashed border-[var(--border)] bg-[var(--paper)] p-5 text-center hover:border-[var(--primary)]"><input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={(e) => upload(e.target.files?.[0])} /><span>{uploading ? <LoaderCircle className="mx-auto size-7 animate-spin text-[var(--primary)]" /> : <ImagePlus className="mx-auto size-7 text-[var(--primary)]" />}<strong className="mt-2 block">{cover ? "Capa pronta" : "Adicionar capa do evento"}</strong><span className="text-sm font-normal text-slate-600">JPG, PNG ou WebP de até 5 MB</span></span></label>
    <div className="flex justify-end"><Button size="lg" disabled={isSubmitting || uploading}>{isSubmitting ? "Salvando..." : event ? "Salvar alterações" : "Criar rascunho"}</Button></div>
  </form>;
}

