import { expect, test } from "@playwright/test";

const widths = [320, 390, 768, 1024, 1440];
const routes = ["/", "/eventos/festival-de-rock-2026", "/entrar", "/cadastro"];

for (const width of widths) {
  test(`páginas públicas não cortam conteúdo em ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    for (const route of routes) {
      await page.goto(route);
      await expect(page.locator("main")).toBeVisible();
      const overflow = await page.evaluate(() => ({
        document: document.documentElement.scrollWidth,
        viewport: window.innerWidth,
      }));
      expect(overflow.document, `${route} em ${width}px`).toBeLessThanOrEqual(overflow.viewport);
    }

    if (width <= 390) {
      await page.getByLabel("Abrir menu").click();
      await expect(page.getByRole("navigation", { name: "Navegação mobile" }).getByRole("link", { name: "Explorar agenda" })).toBeVisible();
      const documentWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      expect(documentWidth, `menu aberto em ${width}px`).toBeLessThanOrEqual(width);
    }
  });
}
