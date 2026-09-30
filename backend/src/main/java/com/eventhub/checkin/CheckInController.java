package com.eventhub.checkin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}/check-ins")
@PreAuthorize("hasRole('ORGANIZER')")
public class CheckInController {
    private final CheckInService checkIns;
    public CheckInController(CheckInService checkIns) { this.checkIns = checkIns; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CheckInService.Result checkIn(@PathVariable UUID eventId, @Valid @RequestBody Request request, JwtAuthenticationToken auth) {
        return checkIns.checkIn(eventId, request.token(), UUID.fromString(auth.getName()));
    }
    record Request(@NotBlank String token) {}
}

