import type { EventView, PageView } from "@/lib/types";

const serverApi = process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
const apiTimeoutMs = 90_000;

export const sampleEvents: EventView[] = [
  {
    id: "a0a14f4e-9991-41ff-b8a7-76355061dd65", organizerId: "demo", slug: "festival-de-rock-2026", title: "Festival de Rock 2026",
    description: "Uma noite para celebrar grandes bandas independentes, novos sons e a energia de quem vive música ao vivo.",
    venue: "Arena Beira-Mar", address: "Av. Jornalista Rubens de Arruda Ramos, 1200", city: "Florianópolis", state: "SC", timezone: "America/Sao_Paulo",
    startsAt: "2026-11-15T22:00:00Z", endsAt: "2026-11-16T04:00:00Z", status: "PUBLISHED",
    coverUrl: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1600&q=85", ticketTypeId: "demo-rock", capacity: 500, confirmedCount: 347, available: 153,
  },
  {
    id: "e09c285e-2ed8-4695-92e7-79b95df38174", organizerId: "demo", slug: "summit-criativo", title: "Summit Criativo",
    description: "Design, produto e tecnologia em conversas práticas com quem está construindo o futuro.",
    venue: "Centro de Inovação", address: "Rua do Futuro, 80", city: "São Paulo", state: "SP", timezone: "America/Sao_Paulo",
    startsAt: "2026-12-04T12:30:00Z", endsAt: "2026-12-04T21:00:00Z", status: "PUBLISHED",
    coverUrl: "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=1600&q=85", ticketTypeId: "demo-summit", capacity: 280, confirmedCount: 189, available: 91,
  },
  {
    id: "b23a90de-c7fe-4b54-b9a2-57a519849bd7", organizerId: "demo", slug: "jazz-no-parque", title: "Jazz no Parque",
    description: "Fim de tarde ao ar livre com artistas locais, gastronomia e música para toda a família.",
    venue: "Parque Barigui", address: "Av. Cândido Hartmann, s/n", city: "Curitiba", state: "PR", timezone: "America/Sao_Paulo",
    startsAt: "2026-11-28T19:00:00Z", endsAt: "2026-11-28T23:00:00Z", status: "PUBLISHED",
    coverUrl: "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=1600&q=85", ticketTypeId: "demo-jazz", capacity: 800, confirmedCount: 412, available: 388,
  },
];

export async function getEvents(query = "", city = "", from = "", page = 0): Promise<PageView<EventView>> {
  try {
    const params = new URLSearchParams({ page: String(page), size: "12" });
    if (query) params.set("query", query);
    if (city) params.set("city", city);
    if (from) params.set("from", new Date(`${from}T00:00:00-03:00`).toISOString());
    const response = await fetch(`${serverApi}/events?${params}`, { next: { revalidate: 30 }, signal: AbortSignal.timeout(apiTimeoutMs) });
    if (!response.ok) throw new Error("API indisponível");
    return response.json();
  } catch (error) {
    if (process.env.NODE_ENV === "production") throw new Error("EVENTHUB_API_UNAVAILABLE", { cause: error });
    const normalizedQuery = query.trim().toLocaleLowerCase("pt-BR");
    const normalizedCity = city.trim().toLocaleLowerCase("pt-BR");
    const filtered = sampleEvents.filter((event) =>
      (!normalizedQuery || `${event.title} ${event.description}`.toLocaleLowerCase("pt-BR").includes(normalizedQuery))
      && (!normalizedCity || `${event.city} ${event.state}`.toLocaleLowerCase("pt-BR").includes(normalizedCity))
      && (!from || event.startsAt.slice(0, 10) >= from));
    return { content: filtered.slice(page * 12, (page + 1) * 12), page, size: 12, totalElements: filtered.length, totalPages: Math.ceil(filtered.length / 12) };
  }
}

export async function getEvent(slug: string): Promise<EventView | null> {
  try {
    const response = await fetch(`${serverApi}/events/${encodeURIComponent(slug)}`, { next: { revalidate: 30 }, signal: AbortSignal.timeout(apiTimeoutMs) });
    if (response.status === 404) return null;
    if (!response.ok) throw new Error("API indisponível");
    return response.json();
  } catch (error) {
    if (process.env.NODE_ENV === "production") throw new Error("EVENTHUB_API_UNAVAILABLE", { cause: error });
    return sampleEvents.find((event) => event.slug === slug) ?? null;
  }
}

export function apiBase() { return serverApi; }

