import { NextRequest, NextResponse } from "next/server";
import { sameOrigin } from "@/lib/same-origin";

const API_URL = process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
const secure = process.env.COOKIE_SECURE
  ? process.env.COOKIE_SECURE === "true"
  : process.env.NODE_ENV === "production";

function setSession(response: NextResponse, data: { accessToken: string; refreshToken: string; expiresIn: number }) {
  response.cookies.set("eh_access", data.accessToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: data.expiresIn });
  response.cookies.set("eh_refresh", data.refreshToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: 60 * 60 * 24 * 30 });
}

function safeReturnPath(value: string | null) {
  return value?.startsWith("/") && !value.startsWith("//") && !value.includes("\\")
    ? value
    : "/meus-ingressos";
}

export async function GET(request: NextRequest, { params }: { params: Promise<{ action: string }> }) {
  if ((await params).action !== "refresh") return NextResponse.json({ detail: "Rota não encontrada." }, { status: 404 });
  const next = safeReturnPath(request.nextUrl.searchParams.get("next"));
  const refreshToken = request.cookies.get("eh_refresh")?.value;
  if (refreshToken) {
    try {
      const upstream = await fetch(`${API_URL}/auth/refresh`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }), cache: "no-store",
      });
      if (upstream.ok) {
        const tokens = await upstream.json();
        const response = NextResponse.redirect(new URL(next, request.url));
        setSession(response, tokens);
        return response;
      }
    } catch { /* A sessão expirada leva ao login. */ }
  }
  const login = new URL("/entrar", request.url);
  login.searchParams.set("next", next);
  const response = NextResponse.redirect(login);
  response.cookies.delete("eh_access");
  response.cookies.delete("eh_refresh");
  return response;
}

export async function POST(request: NextRequest, { params }: { params: Promise<{ action: string }> }) {
  if (!sameOrigin(request)) return NextResponse.json({ code: "INVALID_ORIGIN", detail: "Origem inválida." }, { status: 403 });
  const { action } = await params;
  if (!["login", "register", "refresh", "logout"].includes(action)) return NextResponse.json({ detail: "Rota não encontrada." }, { status: 404 });
  const refreshToken = request.cookies.get("eh_refresh")?.value;
  let body: unknown;
  if (action === "refresh" || action === "logout") body = { refreshToken };
  else body = await request.json();
  const upstream = await fetch(`${API_URL}/auth/${action}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body), cache: "no-store" });
  if (action === "logout") {
    const response = new NextResponse(null, { status: 204 }); response.cookies.delete("eh_access"); response.cookies.delete("eh_refresh"); return response;
  }
  const payload = await upstream.json().catch(() => ({ detail: "Serviço temporariamente indisponível." }));
  const response = NextResponse.json(upstream.ok
    ? { userId: payload.userId, name: payload.name, roles: payload.roles }
    : payload, { status: upstream.status });
  if (upstream.ok) setSession(response, payload);
  return response;
}

