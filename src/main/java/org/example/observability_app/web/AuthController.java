package org.example.observability_app.web;

import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.observability_app.entity.User;
import org.example.observability_app.repository.UserRepo;
import org.example.observability_app.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepo userRepo, PasswordEncoder encoder, JwtService jwt) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record AuthRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6) String password) {}

    public record TokenResponse(String token, String role) {}

    @PostMapping("/register")
    @Observed(name = "auth.register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody AuthRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            log.warn("register conflict for {}", req.email());
            throw new ResponseStatusException(CONFLICT, "email already registered");
        }
        User u = new User();
        u.setEmail(req.email());
        u.setPassword(encoder.encode(req.password()));
        u.setRole("USER");
        userRepo.save(u);
        log.info("user registered id={} email={}", u.getId(), u.getEmail());
        return ResponseEntity.ok(new TokenResponse(jwt.generate(u.getEmail(), u.getRole()), u.getRole()));
    }

    @PostMapping("/login")
    @Observed(name = "auth.login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody AuthRequest req) {
        User u = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "bad credentials"));
        if (!encoder.matches(req.password(), u.getPassword())) {
            log.warn("bad password for {}", req.email());
            throw new ResponseStatusException(UNAUTHORIZED, "bad credentials");
        }
        log.info("login ok email={}", u.getEmail());
        return ResponseEntity.ok(new TokenResponse(jwt.generate(u.getEmail(), u.getRole()), u.getRole()));
    }
}
