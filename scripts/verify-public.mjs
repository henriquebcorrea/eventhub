import { randomBytes } from "node:crypto";

const api = process.env.EVENTHUB_API_URL ?? "https://eventhub-api-3c8f.onrender.com/api/v1";
const organizerEmail = process.env.EVENTHUB_DEMO_EMAIL;
const organizerPassword = process.env.EVENTHUB_DEMO_PASSWORD;
if (!organizerEmail || !organizerPassword) throw new Error("Informe EVENTHUB_DEMO_EMAIL e EVENTHUB_DEMO_PASSWORD no ambiente privado.");

async function call(method, path, token, body) {
  const response = await fetch(`${api}${path}`, {
    method,
    headers: { ...(body ? { "Content-Type": "application/json" } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(300_000),
  });
  const data = await response.json().catch(() => null);
  return { status: response.status, data };
}

function expectStatus(result, status, label) {
  if (result.status !== status) throw new Error(`${label}: HTTP ${result.status} ${result.data?.code ?? ""} ${result.data?.detail ?? ""}`);
  return result.data;
}

const organizer = expectStatus(await call("POST", "/auth/login", null, { email: organizerEmail, password: organizerPassword }), 200, "Login do organizador");
const suffix = `${Date.now()}-${randomBytes(3).toString("hex")}`;
const password = randomBytes(24).toString("base64url");
const first = expectStatus(await call("POST", "/auth/register", null, { name: "Participante técnico A", email: `eventhub-smoke-a-${suffix}@example.com`, password, organizer: false }), 201, "Cadastro A");
const second = expectStatus(await call("POST", "/auth/register", null, { name: "Participante técnico B", email: `eventhub-smoke-b-${suffix}@example.com`, password, organizer: false }), 201, "Cadastro B");
let eventId;
try {
  const startsAt = new Date(Date.now() + 7 * 86400_000);
  const event = expectStatus(await call("POST", "/events", organizer.accessToken, {
    title: `Teste técnico EventHub ${suffix}`,
    description: "Evento fictício para demonstração técnica das reservas em grupo, lista de espera e check-in. Ingressos não dão acesso a um evento real.",
    venue: "Local de teste", address: "Endereço ilustrativo, 1", city: "Florianópolis", state: "SC", timezone: "America/Sao_Paulo",
    startsAt: startsAt.toISOString(), endsAt: new Date(startsAt.getTime() + 4 * 3600_000).toISOString(), capacity: 4,
    ticketTypes: [{ name: "Pista", capacity: 2 }, { name: "Varanda", capacity: 2 }],
  }), 201, "Criação do evento");
  eventId = event.id;
  expectStatus(await call("POST", `/events/${eventId}/publish`, organizer.accessToken), 200, "Publicação");
  const pista = event.ticketTypes.find((type) => type.name === "Pista");
  const varanda = event.ticketTypes.find((type) => type.name === "Varanda");
  if (!pista || !varanda) throw new Error("Tipos de ingresso não encontrados.");
  const group = expectStatus(await call("POST", `/events/${eventId}/registrations`, first.accessToken, { ticketTypeId: pista.id, attendeeNames: ["Pessoa Um", "Pessoa Dois"] }), 201, "Grupo na Pista");
  if (group.tickets.length !== 2 || group.tickets[0].qrPayload === group.tickets[1].qrPayload) throw new Error("QR individuais não foram emitidos.");
  expectStatus(await call("POST", `/events/${eventId}/registrations`, first.accessToken, { ticketTypeId: varanda.id, attendeeNames: ["Pessoa Três"] }), 201, "Reserva na Varanda");
  const waiting = expectStatus(await call("POST", `/events/${eventId}/waitlist`, second.accessToken, { ticketTypeId: pista.id, attendeeNames: ["Pessoa Fila"] }), 201, "Entrada na fila");
  if (waiting.position !== 1) throw new Error(`Posição inesperada na fila: ${waiting.position}`);
  expectStatus(await call("DELETE", `/tickets/${group.tickets[0].id}`, first.accessToken), 204, "Cancelamento individual");
  const promoted = expectStatus(await call("GET", "/tickets/mine", second.accessToken), 200, "Promoção");
  if (promoted.length !== 1 || promoted[0].attendeeName !== "Pessoa Fila") throw new Error("Fila não foi promovida automaticamente.");
  const firstCheckIn = expectStatus(await call("POST", `/organizer/events/${eventId}/check-ins`, organizer.accessToken, { token: group.tickets[1].qrPayload }), 201, "Primeiro check-in");
  if (firstCheckIn.status !== "CHECKED_IN") throw new Error("Check-in não foi confirmado.");
  const duplicate = expectStatus(await call("POST", `/organizer/events/${eventId}/check-ins`, organizer.accessToken, { token: group.tickets[1].qrPayload }), 409, "Segundo check-in");
  if (duplicate.code !== "ALREADY_CHECKED_IN") throw new Error("Segundo check-in não foi rejeitado corretamente.");
  const metrics = expectStatus(await call("GET", `/organizer/events/${eventId}/metrics`, organizer.accessToken), 200, "Dashboard");
  if (metrics.confirmed !== 3 || metrics.checkedIn !== 1 || metrics.byType?.length !== 2) throw new Error(`Métricas inconsistentes: ${JSON.stringify(metrics)}`);
  console.log("PUBLIC_SMOKE_OK: dois tipos, grupo, QR individuais, fila, promoção, check-in único e dashboard.");
} finally {
  if (eventId) {
    expectStatus(await call("POST", `/events/${eventId}/cancel`, organizer.accessToken), 200, "Cancelamento do evento técnico");
    console.log("EVENTO_TECNICO_CANCELADO");
  }
}
