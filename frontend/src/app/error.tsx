"use client";

import { useEffect } from "react";
import { RefreshCw, WifiOff } from "lucide-react";

export default function ErrorPage() {
  useEffect(() => {
    const retry = window.setInterval(() => window.location.reload(), 25_000);
    return () => window.clearInterval(retry);
  }, []);

  return (
    <main className="container-shell grid min-h-[70vh] place-items-center py-12 text-center">
      <div className="w-full max-w-xl border border-[var(--navy)] bg-[var(--navy)] p-8 text-[var(--paper)] shadow-[12px_12px_0_#c7f36b] sm:p-12">
        <WifiOff aria-hidden="true" className="mx-auto size-12 text-[var(--accent)]" />
        <p className="eyebrow mt-5 text-[var(--accent)]">Um instante / Estamos voltando</p>
        <h1 className="display-tight mt-3 text-4xl font-extrabold">O serviço está iniciando<span className="text-[var(--primary)]">.</span></h1>
        <p className="mt-5 text-white/70">Nossa API gratuita pode levar alguns minutos para despertar após um período sem acessos. Esta página tentará novamente automaticamente.</p>
        <button onClick={() => window.location.reload()} className="focus-ring mt-7 inline-flex min-h-12 items-center gap-2 bg-[var(--accent)] px-5 py-3 font-extrabold text-[var(--navy)] hover:bg-[#dcffa0]">
          <RefreshCw aria-hidden="true" className="size-4" /> Tentar agora
        </button>
      </div>
    </main>
  );
}
