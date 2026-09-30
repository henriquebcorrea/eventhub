import { afterEach, describe, expect, it, vi } from "vitest";
import { getEvent, getEvents } from "@/lib/api";

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe("catálogo em produção", () => {
  it("não confunde API indisponível com catálogo vazio", async () => {
    vi.stubEnv("NODE_ENV", "production");
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("API offline")));
    await expect(getEvents()).rejects.toThrow("EVENTHUB_API_UNAVAILABLE");
  });

  it("distingue evento inexistente de falha da API", async () => {
    vi.stubEnv("NODE_ENV", "production");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValueOnce(new Response(null, { status: 404 }))
      .mockResolvedValueOnce(new Response(null, { status: 503 })));
    await expect(getEvent("nao-existe")).resolves.toBeNull();
    await expect(getEvent("festival")).rejects.toThrow("EVENTHUB_API_UNAVAILABLE");
  });
});
