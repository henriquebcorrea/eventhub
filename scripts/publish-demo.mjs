import { randomBytes } from "node:crypto";
import { readFile } from "node:fs/promises";

const api = process.env.EVENTHUB_API_URL ?? "https://eventhub-api-3c8f.onrender.com/api/v1";
const web = process.env.EVENTHUB_WEB_URL ?? "https://eventhub-alpha-sandy.vercel.app";
const samples = [
  { title: "Festival Aurora", city: "Florianópolis", state: "SC", image: "festival-aurora.png", startsAt: "2027-01-16T22:00:00Z", endsAt: "2027-01-17T04:00:00Z", types: [["Pista", 80], ["Área tranquila", 40]], theme: "música independente à beira-mar" },
  { title: "Summit Criativo", city: "São Paulo", state: "SP", image: "summit-criativo.png", startsAt: "2027-02-04T12:00:00Z", endsAt: "2027-02-04T21:00:00Z", types: [["Auditório", 60], ["Networking", 30]], theme: "design, produto e tecnologia" },
  { title: "Jazz no Jardim", city: "Curitiba", state: "PR", image: "jazz-no-jardim.png", startsAt: "2027-02-20T20:00:00Z", endsAt: "2027-02-21T00:00:00Z", types: [["Gramado", 90], ["Área coberta", 30]], theme: "jazz ao ar livre no fim da tarde" },
  { title: "Feira de Sabores", city: "Belo Horizonte", state: "MG", image: "feira-de-sabores.png", startsAt: "2027-03-06T14:00:00Z", endsAt: "2027-03-06T22:00:00Z", types: [["Manhã", 70], ["Tarde", 70]], theme: "gastronomia regional e encontros" },
  { title: "Corrida Orla Viva", city: "Rio de Janeiro", state: "RJ", image: "corrida-orla-viva.png", startsAt: "2027-03-21T09:00:00Z", endsAt: "2027-03-21T13:00:00Z", types: [["Caminhada", 100], ["Corrida", 100]], theme: "movimento e bem-estar na orla" },
  { title: "Cinema ao Ar Livre", city: "Recife", state: "PE", image: "cinema-ao-ar-livre.png", startsAt: "2027-04-10T21:00:00Z", endsAt: "2027-04-11T01:00:00Z", types: [["Cadeiras", 80], ["Almofadas", 40]], theme: "cinema sob as estrelas" },
];

async function request(path, token, body) {
  const response = await fetch(`${api}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(300_000),
  });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(`${path}: ${response.status} ${value.code ?? ""} ${value.detail ?? ""}`);
  return value;
}

async function cover(file, token) {
  const signed = await request("/media/cloudinary-signature", token);
  const bytes = await readFile(new URL(`../frontend/public/demo-covers/${file}`, import.meta.url));
  const form = new FormData();
  form.set("file", new Blob([bytes], { type: "image/png" }), file);
  form.set("api_key", signed.apiKey);
  form.set("timestamp", String(signed.timestamp));
  form.set("folder", signed.folder);
  form.set("allowed_formats", signed.allowedFormats);
  form.set("signature", signed.signature);
  const response = await fetch(`https://api.cloudinary.com/v1_1/${signed.cloudName}/image/upload`, { method: "POST", body: form, signal: AbortSignal.timeout(120_000) });
  const image = await response.json();
  if (!response.ok || !image.secure_url) throw new Error(`Cloudinary ${file}: ${response.status} ${image.error?.message ?? ""}`);
  return { coverUrl: image.secure_url, coverPublicId: image.public_id };
}

const email = `eventhub-demo-${Date.now()}-${randomBytes(3).toString("hex")}@example.com`;
const password = randomBytes(24).toString("base64url");
const session = await request("/auth/register", null, { name: "EventHub Portfólio", email, password, organizer: true });
console.log(`CREDENTIALS ${JSON.stringify({ email, password })}`);
const links = [];
for (const sample of samples) {
  const image = await cover(sample.image, session.accessToken);
  const event = await request("/events", session.accessToken, {
    title: sample.title,
    description: `Evento fictício para demonstração do EventHub. Esta experiência de ${sample.theme} não acontecerá de verdade. Você pode fazer uma inscrição de teste, receber QR Codes e explorar o check-in, mas o ingresso não dá acesso a um evento real.`,
    venue: "Local ilustrativo",
    address: "Endereço de demonstração, 100",
    city: sample.city, state: sample.state, timezone: "America/Sao_Paulo",
    startsAt: sample.startsAt, endsAt: sample.endsAt,
    capacity: sample.types.reduce((total, [, count]) => total + count, 0),
    ticketTypes: sample.types.map(([name, capacity]) => ({ name, capacity })),
    ...image,
  });
  await request(`/events/${event.id}/publish`, session.accessToken);
  links.push(`${web}/eventos/${event.slug}`);
  console.log(`PUBLISHED ${sample.title} ${links.at(-1)}`);
}
console.log(`COMPLETE ${JSON.stringify(links)}`);
