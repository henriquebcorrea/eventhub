package com.eventhub.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventService events;
    public EventController(EventService events) { this.events = events; }

    @GetMapping
    EventService.PageView<EventService.EventView> search(@RequestParam(required = false) String query,
                                                         @RequestParam(required = false) String city,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "12") int size) {
        return events.search(query, city, from, page, size);
    }

    @GetMapping("/{slug}")
    EventService.EventView detail(@PathVariable String slug) { return events.getPublished(slug); }

    @GetMapping("/organizer/mine")
    @PreAuthorize("hasRole('ORGANIZER')")
    List<EventService.EventView> mine(JwtAuthenticationToken auth) { return events.organizerEvents(userId(auth)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORGANIZER')")
    EventService.EventView create(@Valid @RequestBody EventRequest request, JwtAuthenticationToken auth) {
        return events.create(userId(auth), request.toData());
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventService.EventView update(@PathVariable UUID eventId, @Valid @RequestBody EventRequest request, JwtAuthenticationToken auth) {
        return events.update(eventId, userId(auth), request.toData());
    }

    @PostMapping("/{eventId}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventService.EventView publish(@PathVariable UUID eventId, JwtAuthenticationToken auth) { return events.publish(eventId, userId(auth)); }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventService.EventView cancel(@PathVariable UUID eventId, JwtAuthenticationToken auth) { return events.cancel(eventId, userId(auth)); }

    private UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getName()); }

    record EventRequest(@NotBlank @Size(max = 140) String title,
                        @NotBlank @Size(min = 30, max = 5000) String description,
                        @NotBlank @Size(max = 160) String venue,
                        @NotBlank @Size(max = 220) String address,
                        @NotBlank @Size(max = 120) String city,
                        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String state,
                        @NotBlank String timezone,
                        @NotNull @Future Instant startsAt,
                        @NotNull Instant endsAt,
                        @Min(1) @Max(100000) int capacity,
                        @Size(max = 600) String coverUrl,
                        @Size(max = 255) String coverPublicId,
                        List<EventService.TicketTypeData> ticketTypes) {
        EventService.EventData toData() { return new EventService.EventData(title, description, venue, address, city, state, timezone, startsAt, endsAt, capacity, coverUrl, coverPublicId, ticketTypes); }
    }
}
