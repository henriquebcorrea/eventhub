import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { apiBase } from "@/lib/api";

export async function authenticatedGet<T>(path: string, returnTo = path.startsWith("tickets") ? "/meus-ingressos" : "/organizador/eventos"): Promise<T> {
  const session = await cookies();
  const token = session.get("eh_access")?.value;
  const renew = `/api/auth/refresh?next=${encodeURIComponent(returnTo)}`;
  if (!token) redirect(session.has("eh_refresh") ? renew : `/entrar?next=${encodeURIComponent(returnTo)}`);
  const response = await fetch(`${apiBase()}/${path}`, { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" });
  if (response.status === 401) redirect(session.has("eh_refresh") ? renew : `/entrar?next=${encodeURIComponent(returnTo)}`);
  if (!response.ok) throw new Error(`Falha ao carregar ${path}`);
  return response.json();
}

