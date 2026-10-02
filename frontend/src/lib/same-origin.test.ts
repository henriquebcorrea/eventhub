import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { sameOrigin } from "@/lib/same-origin";

function request(origin: string | null, host = "127.0.0.1:3000") {
  const headers = new Headers({ host });
  if (origin) headers.set("origin", origin);
  return new NextRequest("http://frontend:3000/api/auth/register", { method: "POST", headers });
}

describe("proteção de origem no BFF", () => {
  it("aceita o host público mesmo quando a URL interna do Next.js é diferente", () => {
    expect(sameOrigin(request("http://127.0.0.1:3000"))).toBe(true);
  });

  it("rejeita outro domínio, outra porta ou origem inválida", () => {
    expect(sameOrigin(request("https://evil.example"))).toBe(false);
    expect(sameOrigin(request("http://127.0.0.1:3001"))).toBe(false);
    expect(sameOrigin(request("null"))).toBe(false);
  });
});
