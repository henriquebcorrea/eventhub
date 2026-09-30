import type { Metadata } from "next";
import "./globals.css";
import { Header } from "@/components/header";
import { Providers } from "@/components/providers";

export const metadata: Metadata = {
  title: { default: "EventHub — encontre seu próximo evento", template: "%s · EventHub" },
  description: "Descubra eventos, garanta seu ingresso e faça check-in com QR Code.",
  icons: { icon: "/favicon.svg" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body><Providers><Header />{children}<footer className="mt-20 border-t border-slate-200 bg-white"><div className="container-shell flex flex-col gap-3 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between"><p>© 2026 EventHub. Eventos que aproximam pessoas.</p><p>Feito para aproximar pessoas por meio de boas experiências.</p></div></footer></Providers></body></html>;
}

