import Link from "next/link";
import { Suspense } from "react";
import { AuthForm } from "@/components/auth-form";
export const metadata = { title: "Criar conta" };
export default function RegisterPage() { return <main className="container-shell grid min-h-[calc(100vh-12rem)] place-items-center py-12"><section className="surface w-full max-w-lg p-7 shadow-soft sm:p-9"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Comece agora</p><h1 className="mt-2 text-3xl font-black tracking-[-.04em]">Crie sua conta EventHub</h1><p className="mt-2 text-slate-500">Garanta ingressos ou publique seu primeiro evento.</p><div className="mt-7"><Suspense><AuthForm mode="register" /></Suspense></div><p className="mt-6 text-center text-sm text-slate-500">Já tem conta? <Link className="font-bold text-[var(--primary)]" href="/entrar">Entrar</Link></p></section></main>; }
