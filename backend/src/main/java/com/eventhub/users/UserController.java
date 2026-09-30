package com.eventhub.users;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }

    @GetMapping("/me")
    UserView me(JwtAuthenticationToken authentication) {
        var user = users.require(UUID.fromString(authentication.getName()));
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRoles());
    }

    public record UserView(UUID id, String name, String email, Set<UserRole> roles) {}
}
