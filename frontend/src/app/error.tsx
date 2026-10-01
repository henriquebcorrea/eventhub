"use client";

import { useEffect } from "react";
import { RefreshCw, WifiOff } from "lucide-react";

export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    const retry = window.setInterval(reset, 15_000);
    return () => window.clearInterval(retry);
  }, [reset]);

  return (
    <main className="container-shell grid min-h-[60vh] place-items-center py-16 text-center">
      <div className="surface max-w-xl p-8 sm:p-12">
        <WifiOff aria-hidden="true" className="mx-auto size-12 text-[var(--primary)]" />
        <h1 className="mt-5 text-3xl font-black tracking-tight">O servi�o est� iniciando</h1>
        <p className="mt-3 text-slate-600">Nossa API gratuita pode levar alguns minutos para despertar ap�s um per�odo sem acessos. Esta p�gina tentar� novamente automaticamente.</p>
        <button onClick={reset} className="focus-ring mt-7 inline-flex items-center gap-2 rounded-xl bg-[var(--primary)] px-5 py-3 font-bold text-white hover:bg-[var(--primary-strong)]">
          <RefreshCw aria-hidden="true" className="size-4" /> Tentar agora
        </button>
      </div>
    </main>
  );
}

