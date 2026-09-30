import { expect, test } from "@playwright/test";

interface EventResult {
  id: string;
  title: string;
  status: "DRAFT" | "PUBLISHED";
}

interface RegistrationResult {
  ticket: { qrPayload: string };
}

interface MetricsResult {
  confirmed: number;
  checkedIn: number;
}

test("@full cria evento, inscreve participante, valida ingresso e atualiza dashboard", async ({ browser }) => {
  test.skip(!process.env.E2E_FULL, "Exige a stack completa do Docker Compose.");

  const suffix = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const password = "EventHub@2026";
  const organizer = await browser.newContext();
  const participant = await browser.newContext();
  const organizerRequest = organizer.request;
  const participantRequest = participant.request;

  try {
    const organizerRegistration = await organizerRequest.post("/api/auth/register", {
      data: {
        name: "Organizador E2E",
        email: `organizador-${suffix}@eventhub.test`,
        password,
        organizer: true,
      },
    });
    expect(organizerRegistration.status()).toBe(201);

    const startsAt = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000);
    const endsAt = new Date(startsAt.getTime() + 4 * 60 * 60 * 1000);
    const title = `Evento E2E ${suffix}`;
    const eventResponse = await organizerRequest.post("/api/backend/events", {
      data: {
        title,
        description: "Evento criado automaticamente para validar todo o fluxo operacional do EventHub.",
        venue: "Centro de Testes",
        address: "Rua da Qualidade, 100",
        city: "Florianópolis",
        state: "SC",
        timezone: "America/Sao_Paulo",
        startsAt: startsAt.toISOString(),
        endsAt: endsAt.toISOString(),
        capacity: 10,
        coverUrl: null,
        coverPublicId: null,
      },
    });
    expect(eventResponse.status()).toBe(201);
    const event = (await eventResponse.json()) as EventResult;

    const publication = await organizerRequest.post(`/api/backend/events/${event.id}/publish`);
    expect(publication.ok()).toBeTruthy();

    const participantRegistration = await participantRequest.post("/api/auth/register", {
      data: {
        name: "Participante E2E",
        email: `participante-${suffix}@eventhub.test`,
        password,
        organizer: false,
      },
    });
    expect(participantRegistration.status()).toBe(201);

    const registrationResponse = await participantRequest.post(`/api/backend/events/${event.id}/registrations`);
    expect(registrationResponse.status()).toBe(201);
    const registration = (await registrationResponse.json()) as RegistrationResult;

    const checkIn = await organizerRequest.post(`/api/backend/organizer/events/${event.id}/check-ins`, {
      data: { token: registration.ticket.qrPayload },
    });
    expect(checkIn.status()).toBe(201);

    const repeatedCheckIn = await organizerRequest.post(`/api/backend/organizer/events/${event.id}/check-ins`, {
      data: { token: registration.ticket.qrPayload },
    });
    expect(repeatedCheckIn.status()).toBe(409);
    await expect(repeatedCheckIn.json()).resolves.toMatchObject({ code: "ALREADY_CHECKED_IN" });

    const metricsResponse = await organizerRequest.get(`/api/backend/organizer/events/${event.id}/metrics`);
    expect(metricsResponse.ok()).toBeTruthy();
    const metrics = (await metricsResponse.json()) as MetricsResult;
    expect(metrics).toMatchObject({ confirmed: 1, checkedIn: 1 });

    const dashboard = await organizer.newPage();
    await dashboard.goto(`/organizador/eventos/${event.id}`);
    await expect(dashboard.getByRole("heading", { name: title })).toBeVisible();
    await expect(dashboard.getByText("1 inscrição no total")).toBeVisible();
  } finally {
    await participant.close();
    await organizer.close();
  }
});
