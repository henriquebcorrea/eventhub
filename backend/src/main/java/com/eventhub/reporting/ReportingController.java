package com.eventhub.reporting;

import com.eventhub.events.EventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}")
@PreAuthorize("hasRole('ORGANIZER')")
public class ReportingController {
    private final ReportingService reporting;
    public ReportingController(ReportingService reporting) { this.reporting = reporting; }
    @GetMapping("/metrics") ReportingService.Metrics metrics(@PathVariable UUID eventId, JwtAuthenticationToken auth) { return reporting.metrics(eventId, userId(auth)); }
    @GetMapping("/attendees") EventService.PageView<ReportingService.Attendee> attendees(@PathVariable UUID eventId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size, JwtAuthenticationToken auth) { return reporting.attendees(eventId, userId(auth), page, size); }
    private UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getName()); }
}

