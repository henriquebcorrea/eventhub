import type { NextRequest } from "next/server";

export function sameOrigin(request: NextRequest) {
  const origin = request.headers.get("origin");
  if (!origin) return true;

  try {
    const source = new URL(origin);
    const destinationHost = request.headers.get("host") ?? request.nextUrl.host;
    return ["http:", "https:"].includes(source.protocol) && source.host === destinationHost;
  } catch {
    return false;
  }
}
