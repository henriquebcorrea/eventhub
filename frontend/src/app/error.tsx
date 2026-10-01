"use client";

import { useEffect } from "react";
import { RefreshCw, WifiOff } from "lucide-react";

export default function ErrorPage() {
  useEffect(() => {
    const retry = window.setInterval(() => window.location.reload(), 25_000);
    return () => window.clearInterval(retry);
  }, []);

  return (
    <main className="container-shell grid min-h-[60vh] place-items-center py-16 text-center">
      <div className="surface max-w-xl p-8 sm:p-12">
        <WifiOff aria-hidden="true" className="mx-auto size-12 text-[var(--primary)]" />
        <h1 className="mt-5 text-3xl font-black tracking-tight">O serviço está iniciando</h1>
        <p className="mt-3 text-slate-600">Nossa API gratuita pode levar alguns minutos para despertar após um período sem acessos. Esta página tentará novamente automaticamente.</p>
        <button onClick={() => window.location.reload()} className="focus-ring mt-7 inline-flex items-center gap-2 rounded-xl bg-[var(--primary)] px-5 py-3 font-bold text-white hover:bg-[var(--primary-strong)]">
          <RefreshCw aria-hidden="true" className="size-4" /> Tentar agora
        </button>
      </div>
    </main>
  );
}
