package com.eventhub.registrations;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {
    private final RegistrationService registrations;
    public RegistrationController(RegistrationService registrations) { this.registrations = registrations; }

    @PostMapping("/events/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationService.RegistrationResult register(@PathVariable UUID eventId, @RequestBody(required = false) RegistrationRequest request, JwtAuthenticationToken auth) {
        return request == null ? registrations.register(eventId, userId(auth)) : registrations.register(eventId, userId(auth), request.ticketTypeId(), request.attendeeNames());
    }

    @PostMapping("/events/{eventId}/waitlist")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationService.WaitlistView joinWaitlist(@PathVariable UUID eventId, @RequestBody RegistrationRequest request, JwtAuthenticationToken auth) {
        return registrations.joinWaitlist(eventId, userId(auth), request.ticketTypeId(), request.attendeeNames());
    }

    @GetMapping("/waitlist/mine")
    List<RegistrationService.WaitlistView> myWaitlist(JwtAuthenticationToken auth) { return registrations.myWaitlist(userId(auth)); }

    @DeleteMapping("/waitlist/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leaveWaitlist(@PathVariable Long id, JwtAuthenticationToken auth) { registrations.leaveWaitlist(id, userId(auth)); }

    @DeleteMapping("/tickets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelTicket(@PathVariable UUID id, JwtAuthenticationToken auth) { registrations.cancelTicket(id, userId(auth)); }

    @DeleteMapping("/registrations/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID registrationId, JwtAuthenticationToken auth) { registrations.cancel(registrationId, userId(auth)); }

    @GetMapping("/registrations/mine")
    List<Registration> mine(JwtAuthenticationToken auth) { return registrations.mine(userId(auth)); }
    private UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getName()); }
    record RegistrationRequest(UUID ticketTypeId, List<String> attendeeNames) {}
}
