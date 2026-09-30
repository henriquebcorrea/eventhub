package com.eventhub.users;

import com.eventhub.shared.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;

    UserService(UserRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public UserAccount require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public UserAccount requireByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha inválidos."));
    }

    @Transactional
    public UserAccount create(String name, String email, String passwordHash, boolean organizer) {
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Este e-mail já está cadastrado.");
        }
        return repository.save(new UserAccount(name, email, passwordHash, organizer));
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAllById(Collection<UUID> ids) { return repository.findAllById(ids); }
}

