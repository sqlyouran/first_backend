package com.mooc.app.service;

import com.mooc.app.entity.UserEntity;
import com.mooc.app.exception.AuthException;
import com.mooc.app.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_SECONDS = 600; // 10 minutes
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 1800; // 30 minutes

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final VerificationCodeStore codeStore;
    private final RateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService,
                       VerificationCodeStore codeStore,
                       RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.codeStore = codeStore;
        this.rateLimitService = rateLimitService;
    }

    public void sendCode(String email, String ip) {
        if (rateLimitService.isSendCodeIpRateLimited(ip)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many requests. Please try again later.");
        }

        String normalizedEmail = email.toLowerCase();

        if (rateLimitService.isSendCodeEmailRateLimited(normalizedEmail)) {
            return;
        }

        String code = generateCode();
        codeStore.save(normalizedEmail, code, CODE_TTL_SECONDS);
        log.debug("Verification code generated");
    }

    public void register(String email, String code, String password, String ip) {
        if (rateLimitService.isRegisterRateLimited(ip)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many requests. Please try again later.");
        }

        String normalizedEmail = email.toLowerCase();

        Optional<String> storedCode = codeStore.getCode(normalizedEmail);
        if (storedCode.isEmpty()) {
            if (codeStore.isExpired(normalizedEmail)) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "expired_code",
                        "Verification code has expired");
            }
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_code",
                    "Invalid verification code");
        }
        if (!storedCode.get().equals(code)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_code",
                    "Invalid verification code");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException(HttpStatus.CONFLICT, "email_already_registered",
                    "Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setState(UserEntity.State.active);
        userRepository.save(user);

        codeStore.remove(normalizedEmail);
    }

    public LoginResult login(String email, String password, String ip) {
        if (rateLimitService.isLoginRateLimited(ip)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many requests. Please try again later.");
        }

        String normalizedEmail = email.toLowerCase();
        Optional<UserEntity> optUser = userRepository.findByEmail(normalizedEmail);

        if (optUser.isEmpty()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "Email or password is incorrect");
        }

        UserEntity user = optUser.get();

        if (user.getState() == UserEntity.State.deleted) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "Email or password is incorrect");
        }

        if (user.getState() == UserEntity.State.locked) {
            if (user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
                user.setState(UserEntity.State.active);
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            } else {
                throw new AuthException(HttpStatus.LOCKED, "account_locked",
                        "Account is locked");
            }
        }

        if (user.getState() == UserEntity.State.email_unverified) {
            throw new AuthException(HttpStatus.FORBIDDEN, "email_unverified",
                    "Email is not verified");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setState(UserEntity.State.locked);
                user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            }
            userRepository.save(user);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "Email or password is incorrect");
        }

        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResult(accessToken, refreshToken, jwtService.getAccessTokenExpirySeconds());
    }

    public RefreshResult refresh(String refreshToken) {
        Claims claims = jwtService.parseToken(refreshToken)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_refresh_token", "Invalid refresh token"));

        String jti = claims.getId();
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                    "Invalid refresh token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_refresh_token", "Invalid refresh token"));

        String newAccessToken = jwtService.generateAccessToken(user);
        return new RefreshResult(newAccessToken, jwtService.getAccessTokenExpirySeconds());
    }

    public void logout(String refreshToken) {
        Claims claims = jwtService.parseToken(refreshToken)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_refresh_token", "Invalid refresh token"));

        String jti = claims.getId();
        if (jti != null) {
            tokenBlacklistService.add(jti);
        }
    }

    public UserInfo getMe(String accessToken) {
        Claims claims = jwtService.parseToken(accessToken)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_credentials", "Invalid token"));

        UUID userId = UUID.fromString(claims.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_credentials", "Invalid token"));

        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getState().name(),
                user.getCreatedAt(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }

    public void deleteMe(String accessToken) {
        Claims claims = jwtService.parseToken(accessToken)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_credentials", "Invalid token"));

        UUID userId = UUID.fromString(claims.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "invalid_credentials", "Invalid token"));

        user.setState(UserEntity.State.deleted);
        userRepository.save(user);
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}
    public record RefreshResult(String accessToken, long expiresIn) {}
    public record UserInfo(UUID id, String email, String state, Instant createdAt,
                           String username, String nickname, String avatarUrl) {}
}
