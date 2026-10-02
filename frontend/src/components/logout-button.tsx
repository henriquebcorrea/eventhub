"use client";
import { useRouter } from "next/navigation";
import { LogOut } from "lucide-react";

export function LogoutButton() {
  const router = useRouter();
  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.push("/");
    router.refresh();
  }
  return <button type="button" onClick={logout} className="focus-ring inline-flex min-h-11 items-center gap-2 px-3 text-sm font-bold text-white/80 hover:bg-white/10 hover:text-white"><LogOut className="size-4" /> Sair</button>;
}
