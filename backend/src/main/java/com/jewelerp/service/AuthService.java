package com.jewelerp.service;

import com.jewelerp.config.ApiException;
import com.jewelerp.model.AppUser;
import com.jewelerp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(1);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuditService audit;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Value("${app.jwt.expiry-hours}")
    private long expiryHours;

    public record LoginResult(String token, AppUser user) {
    }

    private record Attempts(int count, Instant firstAt) {
    }

    public LoginResult login(String email, String password) {
        String key = email.toLowerCase();
        Attempts current = attempts.get(key);
        if (current != null && current.count() >= MAX_ATTEMPTS
                && current.firstAt().plus(LOCK_WINDOW).isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again in a minute.");
        }

        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            recordFailure(key);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your account has been deactivated. Contact your administrator.");
        }

        attempts.remove(key);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(expiryHours)))
                .claim("roles", List.of(user.getRole()))
                .claim("name", user.getName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        audit.log(user, "Auth", "login", user.getName() + " logged in");
        return new LoginResult(token, user);
    }

    private void recordFailure(String key) {
        attempts.compute(key, (k, old) -> {
            if (old == null || old.firstAt().plus(LOCK_WINDOW).isBefore(Instant.now())) {
                return new Attempts(1, Instant.now());
            }
            return new Attempts(old.count() + 1, old.firstAt());
        });
    }
}
