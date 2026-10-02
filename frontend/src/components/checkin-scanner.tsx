"use client";
import { BrowserQRCodeReader } from "@zxing/browser";
import { CheckCircle2, Keyboard, ScanLine, XCircle } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { Problem } from "@/lib/types";

type ResultState = { kind: "success" | "error"; title: string; detail: string } | null;
export function CheckInScanner({ eventId }: { eventId: string }) {
  const video = useRef<HTMLVideoElement>(null); const lock = useRef(false); const [manual, setManual] = useState(""); const [result, setResult] = useState<ResultState>(null); const [cameraError, setCameraError] = useState(false);
  async function validate(token: string) {
    if (!token || lock.current) return; lock.current = true; setResult(null);
    const response = await fetch(`/api/backend/organizer/events/${eventId}/check-ins`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ token }) });
    if (response.ok) { const data = await response.json(); setResult({ kind: "success", title: "Entrada autorizada", detail: `${data.attendeeName ?? "Participante"} · ${data.ticketTypeName ?? "Ingresso geral"} · ${data.publicCode}` }); }
    else { const problem = await response.json() as Problem; setResult({ kind: "error", title: problem.code === "ALREADY_CHECKED_IN" ? "Ingresso já utilizado" : "Ingresso inválido", detail: problem.detail ?? "Não foi possível validar este ingresso." }); }
    window.setTimeout(() => { lock.current = false; }, 1800);
  }
  useEffect(() => {
    if (!video.current) return; const reader = new BrowserQRCodeReader(); let controls: { stop(): void } | undefined;
    reader.decodeFromVideoDevice(undefined, video.current, (scan) => { if (scan) validate(scan.getText()); }).then((value) => { controls = value; }).catch(() => setCameraError(true));
    return () => controls?.stop();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
  return <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
    <div className="overflow-hidden rounded-3xl bg-slate-950"><div className="relative aspect-[4/3]"><video ref={video} className="h-full w-full object-cover" muted playsInline /><div className="pointer-events-none absolute inset-[15%] rounded-3xl border-2 border-white/80 shadow-[0_0_0_999px_rgba(2,6,23,.5)]"><ScanLine className="absolute -left-8 -top-10 size-7 text-teal-300" /></div>{cameraError && <div className="absolute inset-0 grid place-items-center bg-slate-950 p-8 text-center text-white"><div><XCircle className="mx-auto size-10 text-[var(--primary)]" /><h2 className="mt-3 text-xl font-bold">Câmera indisponível</h2><p className="mt-1 text-sm text-slate-400">Autorize o acesso ou use a entrada manual ao lado.</p></div></div>}</div></div>
    <aside className="grid content-start gap-5"><div className="surface p-5"><h2 className="flex items-center gap-2 font-black"><Keyboard className="size-5 text-[var(--accent)]" /> Validar manualmente</h2><p className="mt-1 text-sm text-slate-500">Cole o conteúdo do QR Code quando a câmera não estiver disponível.</p><div className="mt-4 grid gap-2"><Input value={manual} onChange={(e) => setManual(e.target.value)} placeholder="v1.ticket.assinatura" /><Button variant="dark" onClick={() => validate(manual)}>Validar ingresso</Button></div></div>
      {result && <div role="status" className={`rounded-2xl border p-6 ${result.kind === "success" ? "border-teal-200 bg-teal-50 text-teal-950" : "border-red-200 bg-red-50 text-red-950"}`}>{result.kind === "success" ? <CheckCircle2 className="size-9 text-teal-600" /> : <XCircle className="size-9 text-red-600" />}<h2 className="mt-3 text-xl font-black">{result.title}</h2><p className="mt-1 text-sm opacity-75">{result.detail}</p></div>}
    </aside>
  </div>;
}

