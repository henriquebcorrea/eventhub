import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EventCard } from "@/components/event-card";
import { sampleEvents } from "@/lib/api";

describe("EventCard", () => {
  it("mostra informações essenciais e disponibilidade", () => {
    render(<EventCard event={sampleEvents[0]} />);
    expect(screen.getByRole("heading", { name: "Festival de Rock 2026" })).toBeInTheDocument();
    expect(screen.getByText(/153 vagas/)).toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveAttribute("href", "/eventos/festival-de-rock-2026");
  });
});

