package com.precisionauto.auth;

import com.precisionauto.common.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 50) String username,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  Role role, String inviteCode) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String token, String role, long expiresInMs) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String inviteCode;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                          @Value("${auth.technician-invite-code}") String inviteCode) {
        this.users = users; this.encoder = encoder; this.jwt = jwt; this.inviteCode = inviteCode;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest r) {
        Role role = r.role() == null ? Role.CLIENT : r.role();
        if (role == Role.TECHNICIAN && !inviteCode.equals(r.inviteCode()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Valid inviteCode required for technician accounts");
        if (users.findByUsername(r.username()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        UserAccount saved = users.save(new UserAccount(r.username(), encoder.encode(r.password()), role));
        return Map.of("id", saved.getId(), "username", saved.getUsername(), "role", saved.getRole());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) {
        UserAccount u = users.findByUsername(r.username())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return new TokenResponse(jwt.generate(u.getUsername(), u.getRole().name()),
                u.getRole().name(), jwt.getExpirationMs());
    }
}
