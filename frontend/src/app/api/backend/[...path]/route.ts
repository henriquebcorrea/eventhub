import { NextRequest, NextResponse } from "next/server";

const API_URL = process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
const allowedRoots = new Set(["events", "tickets", "registrations", "organizer", "media", "users"]);
const secure = process.env.COOKIE_SECURE
  ? process.env.COOKIE_SECURE === "true"
  : process.env.NODE_ENV === "production";

async function refreshSession(request: NextRequest) {
  const refreshToken = request.cookies.get("eh_refresh")?.value;
  if (!refreshToken) return null;
  const response = await fetch(`${API_URL}/auth/refresh`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken }), cache: "no-store" });
  return response.ok ? response.json() as Promise<{ accessToken: string; refreshToken: string; expiresIn: number }> : null;
}

async function handler(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const path = (await context.params).path;
  if (!path.length || !allowedRoots.has(path[0])) return NextResponse.json({ detail: "Rota não permitida." }, { status: 404 });
  const origin = request.headers.get("origin");
  if (!["GET", "HEAD"].includes(request.method) && origin && origin !== request.nextUrl.origin) return NextResponse.json({ detail: "Origem inválida." }, { status: 403 });
  const url = `${API_URL}/${path.map(encodeURIComponent).join("/")}${request.nextUrl.search}`;
  const rawBody = ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer();
  let token = request.cookies.get("eh_access")?.value;
  const call = (access?: string) => fetch(url, {
    method: request.method,
    headers: { ...(request.headers.get("content-type") ? { "Content-Type": request.headers.get("content-type")! } : {}), ...(access ? { Authorization: `Bearer ${access}` } : {}) },
    body: rawBody, cache: "no-store", redirect: "manual",
  });
  let upstream = await call(token);
  let renewed: Awaited<ReturnType<typeof refreshSession>> = null;
  if (upstream.status === 401) { renewed = await refreshSession(request); if (renewed) { token = renewed.accessToken; upstream = await call(token); } }
  const response = new NextResponse(upstream.body, { status: upstream.status, headers: { "Content-Type": upstream.headers.get("content-type") ?? "application/json" } });
  if (renewed) {
    response.cookies.set("eh_access", renewed.accessToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: renewed.expiresIn });
    response.cookies.set("eh_refresh", renewed.refreshToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: 60 * 60 * 24 * 30 });
  }
  return response;
}

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;
