package com.eventhub.tickets;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService tickets;
    public TicketController(TicketService tickets) { this.tickets = tickets; }
    @GetMapping("/mine") List<TicketService.TicketView> mine(JwtAuthenticationToken auth) { return tickets.mine(UUID.fromString(auth.getName())); }
    @GetMapping("/{id}") TicketService.TicketView detail(@PathVariable UUID id, JwtAuthenticationToken auth) { return tickets.detail(id, UUID.fromString(auth.getName())); }
}

