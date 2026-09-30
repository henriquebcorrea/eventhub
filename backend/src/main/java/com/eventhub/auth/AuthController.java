package com.eventhub.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthService.AuthTokens register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request.name(), request.email(), request.password(), request.organizer());
    }

    @PostMapping("/login")
    AuthService.AuthTokens login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    AuthService.AuthTokens refresh(@Valid @RequestBody RefreshRequest request) { return auth.refresh(request.refreshToken()); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) { auth.logout(request.refreshToken()); }

    record RegisterRequest(@NotBlank @Size(min = 2, max = 120) String name,
                           @NotBlank @Email String email,
                           @NotBlank @Size(min = 8, max = 128) String password,
                           boolean organizer) {}
    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    record RefreshRequest(@NotBlank String refreshToken) {}
}
