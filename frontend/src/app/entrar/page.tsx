import Link from "next/link";
import { Suspense } from "react";
import { AuthForm } from "@/components/auth-form";
export const metadata = { title: "Entrar" };
export default function LoginPage() { return <main className="container-shell grid min-h-[calc(100vh-12rem)] place-items-center py-12"><section className="surface w-full max-w-md p-7 shadow-soft sm:p-9"><p className="text-sm font-bold uppercase tracking-[.14em] text-[var(--primary)]">Sua conta</p><h1 className="mt-2 text-3xl font-black tracking-[-.04em]">Que bom ter você de volta</h1><p className="mt-2 text-slate-500">Acesse seus ingressos e eventos em um só lugar.</p><div className="mt-7"><Suspense><AuthForm mode="login" /></Suspense></div><p className="mt-6 text-center text-sm text-slate-500">Ainda não tem conta? <Link className="font-bold text-[var(--primary)]" href="/cadastro">Cadastre-se</Link></p><div className="mt-5 rounded-xl bg-slate-50 p-3 text-xs text-slate-500"><strong>Conta de demonstração:</strong> participante@eventhub.dev / Demo@123</div></section></main>; }

