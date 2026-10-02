import { expect, test } from "@playwright/test";

test("login sem JavaScript nunca envia a senha na URL", async ({ browser }) => {
  const context = await browser.newContext({ javaScriptEnabled: false });
  const page = await context.newPage();
  try {
    await page.goto("/entrar");
    await page.getByLabel("E-mail").fill("teste@example.com");
    await page.getByLabel("Senha").fill("Senha-de-teste-123");
    const submission = page.waitForRequest((request) => request.method() === "POST" && new URL(request.url()).pathname === "/entrar");
    await page.getByRole("button", { name: "Entrar na minha conta" }).click();
    const request = await submission;
    expect(request.postData()).toContain("Senha-de-teste-123");
    expect(new URL(request.url()).searchParams.has("password")).toBe(false);
  } finally {
    await context.close();
  }
});
