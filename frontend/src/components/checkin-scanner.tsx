"use client";
import { BrowserQRCodeReader } from "@zxing/browser";
import { CheckCircle2, Keyboard, ScanLine, XCircle } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { Problem } from "@/lib/types";

type ResultState = { kind: "success" | "error"; title: string; detail: string } | null;
export function CheckInScanner({ eventId }: { eventId: string }) {
  const video = useRef<HTMLVideoElement>(null); const lock = useRef(false); const [manual, setManual] = useState(""); const [result, setResult] = useState<ResultState>(null); const [cameraError, setCameraError] = useState(false); const [cooldown, setCooldown] = useState(false);
  async function validate(token: string) {
    if (!token || lock.current) return; lock.current = true; setCooldown(true); setResult(null);
    try {
      const response = await fetch(`/api/backend/organizer/events/${eventId}/check-ins`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ token }) });
      if (response.ok) { const data = await response.json(); setResult({ kind: "success", title: "Entrada autorizada", detail: `${data.attendeeName ?? "Participante"} · ${data.ticketTypeName ?? "Ingresso geral"} · ${data.publicCode}` }); }
      else { const problem = await response.json() as Problem; setResult({ kind: "error", title: problem.code === "ALREADY_CHECKED_IN" ? "Ingresso já utilizado" : "Ingresso inválido", detail: problem.detail ?? "Não foi possível validar este ingresso." }); }
    } catch { setResult({ kind: "error", title: "Falha de conexão", detail: "Não foi possível conectar ao servidor. Tente novamente." }); }
    finally { window.setTimeout(() => { lock.current = false; setCooldown(false); }, 1800); }
  }
  useEffect(() => {
    if (!video.current) return; const reader = new BrowserQRCodeReader(); let controls: { stop(): void } | undefined;
    reader.decodeFromVideoDevice(undefined, video.current, (scan) => { if (scan) validate(scan.getText()); }).then((value) => { controls = value; }).catch(() => setCameraError(true));
    return () => controls?.stop();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
  return <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
    <div className="overflow-hidden border-[6px] border-[var(--navy)] bg-slate-950"><div className="relative aspect-[4/3]"><video ref={video} className="h-full w-full object-cover" muted playsInline /><div className="pointer-events-none absolute inset-[15%] border-2 border-[var(--accent)] shadow-[0_0_0_999px_rgba(2,6,23,.5)]"><ScanLine className="absolute -left-8 -top-10 size-7 text-[var(--accent)]" /></div>{cameraError && <div className="absolute inset-0 grid place-items-center bg-slate-950 p-8 text-center text-white"><div><XCircle className="mx-auto size-10 text-[var(--primary)]" /><h2 className="mt-3 text-xl font-bold">Câmera indisponível</h2><p className="mt-1 text-sm text-slate-400">Autorize o acesso ou use a entrada manual abaixo.</p></div></div>}</div></div>
    <aside className="grid content-start gap-5"><div className="border border-[var(--border)] bg-[var(--surface)] p-5"><h2 className="flex items-center gap-2 font-display text-xl font-extrabold"><Keyboard className="size-5 text-[var(--primary)]" /> Validar manualmente</h2><p className="mt-2 text-sm text-slate-600">Cole o conteúdo do QR Code quando a câmera não estiver disponível.</p><div className="mt-5 grid gap-2"><Input value={manual} onChange={(e) => setManual(e.target.value)} placeholder="v1.ticket.assinatura" /><Button variant="dark" disabled={cooldown} onClick={() => validate(manual)}>{cooldown ? "Aguarde para validar novamente" : "Validar ingresso"}</Button></div></div>
      {result && <div role="status" className={`border p-6 ${result.kind === "success" ? "border-[#98bd4e] bg-[#e5f7bc] text-[var(--navy)]" : "border-red-300 bg-red-50 text-red-950"}`}>{result.kind === "success" ? <CheckCircle2 className="size-9 text-[#527124]" /> : <XCircle className="size-9 text-red-700" />}<h2 className="mt-3 font-display text-xl font-extrabold">{result.title}</h2><p className="mt-1 text-sm opacity-80">{result.detail}</p></div>}
    </aside>
  </div>;
}

