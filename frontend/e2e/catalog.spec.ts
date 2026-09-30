import { expect, test } from "@playwright/test";
test("catálogo permite encontrar e abrir um evento", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /Encontre um evento/ })).toBeVisible();
  await page.getByPlaceholder("Evento ou tema").fill("Rock");
  await page.getByRole("button", { name: "Buscar" }).click();
  await expect(page.getByRole("heading", { name: "Festival de Rock 2026" })).toBeVisible();
  await page.getByRole("heading", { name: "Festival de Rock 2026" }).click();
  await expect(page.getByRole("button", { name: /Garantir ingresso/ })).toBeVisible();
});

