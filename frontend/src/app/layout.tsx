import type { Metadata } from "next";
import Link from "next/link";
import { DM_Sans, Syne } from "next/font/google";
import "./globals.css";
import { Header } from "@/components/header";
import { Providers } from "@/components/providers";

const bodyFont = DM_Sans({ subsets: ["latin"], variable: "--font-body", display: "swap" });
const displayFont = Syne({ subsets: ["latin"], variable: "--font-display", display: "swap" });

export const metadata: Metadata = {
  title: { default: "EventHub — encontre seu próximo evento", template: "%s · EventHub" },
  description: "Descubra eventos, garanta seu ingresso e faça check-in com QR Code.",
  icons: { icon: "/favicon.svg" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body className={`${bodyFont.variable} ${displayFont.variable}`}><Providers><Header />{children}<footer className="border-t border-white/15 bg-[var(--navy)] text-[var(--paper)]"><div className="container-shell grid gap-8 py-12 md:grid-cols-[1fr_auto] md:items-end"><div><p className="font-display text-3xl font-extrabold tracking-[-.07em]">Event<span className="text-[var(--accent)]">Hub</span><span className="text-[var(--primary)]">.</span></p><p className="mt-3 max-w-md text-sm text-white/60">Eventos que aproximam pessoas. Ingressos gratuitos, entrada simples e boas histórias para viver.</p></div><div className="flex flex-wrap gap-x-6 gap-y-2 text-sm font-bold"><Link className="hover:text-[var(--accent)]" href="/#eventos">Explorar eventos</Link><Link className="hover:text-[var(--accent)]" href="/organizador/eventos">Organizar</Link></div></div><div className="container-shell flex flex-col gap-2 border-t border-white/10 py-5 text-xs text-white/45 sm:flex-row sm:justify-between"><p>© 2026 EventHub.</p><p>O próximo encontro começa aqui.</p></div></footer></Providers></body></html>;
}

