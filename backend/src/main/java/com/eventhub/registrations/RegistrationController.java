package com.eventhub.registrations;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {
    private final RegistrationService registrations;
    public RegistrationController(RegistrationService registrations) { this.registrations = registrations; }

    @PostMapping("/events/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationService.RegistrationResult register(@PathVariable UUID eventId, JwtAuthenticationToken auth) { return registrations.register(eventId, userId(auth)); }

    @DeleteMapping("/registrations/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID registrationId, JwtAuthenticationToken auth) { registrations.cancel(registrationId, userId(auth)); }

    @GetMapping("/registrations/mine")
    List<Registration> mine(JwtAuthenticationToken auth) { return registrations.mine(userId(auth)); }
    private UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getName()); }
}
