export type EventStatus = "DRAFT" | "PUBLISHED" | "CANCELLED" | "COMPLETED";

export interface EventView {
  id: string;
  organizerId: string;
  slug: string;
  title: string;
  description: string;
  venue: string;
  address: string;
  city: string;
  state: string;
  timezone: string;
  startsAt: string;
  endsAt: string;
  status: EventStatus;
  coverUrl?: string;
  coverPublicId?: string;
  ticketTypeId: string;
  capacity: number;
  confirmedCount: number;
  available: number;
}

export interface PageView<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface UserView { id: string; name: string; email: string; roles: ("USER" | "ORGANIZER" | "ADMIN")[]; }
export interface TicketView {
  id: string; registrationId: string; publicCode: string; status: "ACTIVE" | "CANCELLED"; issuedAt: string; qrPayload: string;
  eventId: string; eventSlug: string; eventTitle: string; venue: string; city: string; state: string; startsAt: string; coverUrl?: string;
  eventStatus: EventStatus;
}
export interface Metrics { confirmed: number; capacity: number; checkedIn: number; checkInRate: number; registrationsByDay: { day: string; count: number }[]; }
export interface Problem { title?: string; detail?: string; code?: string; fields?: Record<string, string>; }

