package com.mooc.app.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.exception.AuthException;
import com.mooc.app.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private VerificationCodeStore codeStore;
    @Mock private RateLimitService rateLimitService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService,
                tokenBlacklistService, codeStore, rateLimitService);
    }

    private UserEntity createTestUser(UUID id, String email, UserEntity.State state) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setState(state);
        user.setPasswordHash("hashed-password");
        return user;
    }

    private Claims createMockClaims(String subject, String jti) {
        Map<String, Object> map = new HashMap<>();
        map.put("sub", subject);
        map.put("jti", jti);
        return new DefaultClaims(map);
    }

    // ======================== sendCode ========================

    @Nested
    class SendCode {

        @Test
        void sendCode_savesVerificationCode() {
            when(rateLimitService.isSendCodeIpRateLimited("1.2.3.4")).thenReturn(false);
            when(rateLimitService.isSendCodeEmailRateLimited("test@example.com")).thenReturn(false);

            authService.sendCode("test@example.com", "1.2.3.4");

            verify(codeStore).save(eq("test@example.com"), anyString(), eq(600L));
        }

        @Test
        void sendCode_ipRateLimited_throws429() {
            when(rateLimitService.isSendCodeIpRateLimited("1.2.3.4")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.sendCode("test@example.com", "1.2.3.4"));
            assertEquals("rate_limited", ex.getErrorCode());
        }

        @Test
        void sendCode_infoLogsDoNotContainCodeOrEmail() {
            when(rateLimitService.isSendCodeIpRateLimited("1.2.3.4")).thenReturn(false);
            when(rateLimitService.isSendCodeEmailRateLimited("test@example.com")).thenReturn(false);

            // Capture the code that gets saved
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // Attach ListAppender to AuthService logger at INFO level
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                    LoggerFactory.getLogger(AuthService.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);
            Level originalLevel = logger.getLevel();
            logger.setLevel(Level.INFO);

            try {
                authService.sendCode("test@example.com", "1.2.3.4");
                verify(codeStore).save(eq("test@example.com"), codeCaptor.capture(), eq(600L));
                String savedCode = codeCaptor.getValue();

                // Verify no INFO-level log message contains the email or the verification code
                for (ILoggingEvent event : listAppender.list) {
                    if (event.getLevel().isGreaterOrEqual(Level.INFO)) {
                        String msg = event.getFormattedMessage();
                        assertFalse(msg.contains("test@example.com"),
                                "INFO log should not contain email: " + msg);
                        assertFalse(msg.contains(savedCode),
                                "INFO log should not contain verification code: " + msg);
                    }
                }
            } finally {
                logger.setLevel(originalLevel);
                logger.detachAppender(listAppender);
            }
        }

        @Test
        void sendCode_emailRateLimited_returnsSilently() {
            when(rateLimitService.isSendCodeIpRateLimited("1.2.3.4")).thenReturn(false);
            when(rateLimitService.isSendCodeEmailRateLimited("test@example.com")).thenReturn(true);

            assertDoesNotThrow(() -> authService.sendCode("test@example.com", "1.2.3.4"));
            verify(codeStore, never()).save(anyString(), anyString(), anyLong());
        }
    }

    // ======================== register ========================

    @Nested
    class Register {

        @Test
        void register_createsUser() {
            when(rateLimitService.isRegisterRateLimited("1.2.3.4")).thenReturn(false);
            when(codeStore.getCode("test@example.com")).thenReturn(Optional.of("123456"));
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed");

            authService.register("test@example.com", "123456", "password123", "1.2.3.4");

            verify(userRepository).save(argThat(user ->
                    user.getEmail().equals("test@example.com") &&
                    user.getState() == UserEntity.State.active));
            verify(codeStore).remove("test@example.com");
        }

        @Test
        void register_ipRateLimited_throws429() {
            when(rateLimitService.isRegisterRateLimited("1.2.3.4")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.register("test@example.com", "123456", "pass", "1.2.3.4"));
            assertEquals("rate_limited", ex.getErrorCode());
        }

        @Test
        void register_invalidCode_throwsError() {
            when(rateLimitService.isRegisterRateLimited("1.2.3.4")).thenReturn(false);
            when(codeStore.getCode("test@example.com")).thenReturn(Optional.empty());
            when(codeStore.isExpired("test@example.com")).thenReturn(false);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.register("test@example.com", "wrong", "pass", "1.2.3.4"));
            assertEquals("invalid_code", ex.getErrorCode());
        }

        @Test
        void register_expiredCode_throwsError() {
            when(rateLimitService.isRegisterRateLimited("1.2.3.4")).thenReturn(false);
            when(codeStore.getCode("test@example.com")).thenReturn(Optional.empty());
            when(codeStore.isExpired("test@example.com")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.register("test@example.com", "wrong", "pass", "1.2.3.4"));
            assertEquals("expired_code", ex.getErrorCode());
        }

        @Test
        void register_wrongCode_throwsError() {
            when(rateLimitService.isRegisterRateLimited("1.2.3.4")).thenReturn(false);
            when(codeStore.getCode("test@example.com")).thenReturn(Optional.of("123456"));

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.register("test@example.com", "654321", "pass", "1.2.3.4"));
            assertEquals("invalid_code", ex.getErrorCode());
        }

        @Test
        void register_emailExists_throws409() {
            when(rateLimitService.isRegisterRateLimited("1.2.3.4")).thenReturn(false);
            when(codeStore.getCode("test@example.com")).thenReturn(Optional.of("123456"));
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.register("test@example.com", "123456", "pass", "1.2.3.4"));
            assertEquals("email_already_registered", ex.getErrorCode());
        }
    }

    // ======================== login ========================

    @Nested
    class Login {

        private final UUID userId = UUID.randomUUID();
        private UserEntity activeUser;

        @BeforeEach
        void setUp() {
            activeUser = createTestUser(userId, "test@example.com", UserEntity.State.active);
        }

        @Test
        void login_successReturnsTokens() {
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
            when(jwtService.generateAccessToken(activeUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);

            AuthService.LoginResult result = authService.login("test@example.com", "password123", "1.2.3.4");

            assertEquals("access-token", result.accessToken());
            assertEquals("refresh-token", result.refreshToken());
            assertEquals(1800L, result.expiresIn());
        }

        @Test
        void login_ipRateLimited_throws429() {
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "pass", "1.2.3.4"));
            assertEquals("rate_limited", ex.getErrorCode());
        }

        @Test
        void login_nonexistentEmail_throwsError() {
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "pass", "1.2.3.4"));
            assertEquals("invalid_credentials", ex.getErrorCode());
        }

        @Test
        void login_deletedUser_throwsError() {
            UserEntity deletedUser = createTestUser(userId, "test@example.com", UserEntity.State.deleted);
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(deletedUser));

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "pass", "1.2.3.4"));
            assertEquals("invalid_credentials", ex.getErrorCode());
        }

        @Test
        void login_lockedNotExpired_throwsError() {
            UserEntity lockedUser = createTestUser(userId, "test@example.com", UserEntity.State.locked);
            lockedUser.setLockedUntil(Instant.now().plusSeconds(1800));
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(lockedUser));

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "pass", "1.2.3.4"));
            assertEquals("account_locked", ex.getErrorCode());
        }

        @Test
        void login_lockedExpired_unlocksAccount() {
            UserEntity lockedUser = createTestUser(userId, "test@example.com", UserEntity.State.locked);
            lockedUser.setLockedUntil(Instant.now().minusSeconds(10));
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(lockedUser));
            when(passwordEncoder.matches("pass", "hashed-password")).thenReturn(true);
            when(jwtService.generateAccessToken(lockedUser)).thenReturn("at");
            when(jwtService.generateRefreshToken(lockedUser)).thenReturn("rt");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);

            authService.login("test@example.com", "pass", "1.2.3.4");

            assertEquals(UserEntity.State.active, lockedUser.getState());
            assertEquals(0, lockedUser.getFailedAttempts());
            assertNull(lockedUser.getLockedUntil());
            verify(userRepository).save(lockedUser);
        }

        @Test
        void login_emailUnverified_throws403() {
            UserEntity unverified = createTestUser(userId, "test@example.com", UserEntity.State.email_unverified);
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverified));

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "pass", "1.2.3.4"));
            assertEquals("email_unverified", ex.getErrorCode());
        }

        @Test
        void login_wrongPassword_incrementsFailedAttempts() {
            activeUser.setFailedAttempts(2);
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

            assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "wrong", "1.2.3.4"));

            assertEquals(3, activeUser.getFailedAttempts());
            verify(userRepository).save(activeUser);
        }

        @Test
        void login_5thWrongPassword_locksAccount() {
            activeUser.setFailedAttempts(4);
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

            assertThrows(AuthException.class,
                    () -> authService.login("test@example.com", "wrong", "1.2.3.4"));

            assertEquals(5, activeUser.getFailedAttempts());
            assertEquals(UserEntity.State.locked, activeUser.getState());
            assertNotNull(activeUser.getLockedUntil());
        }

        @Test
        void login_correctPassword_resetsFailedAttempts() {
            activeUser.setFailedAttempts(3);
            when(rateLimitService.isLoginRateLimited("1.2.3.4")).thenReturn(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("pass", "hashed-password")).thenReturn(true);
            when(jwtService.generateAccessToken(activeUser)).thenReturn("at");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("rt");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);

            authService.login("test@example.com", "pass", "1.2.3.4");

            assertEquals(0, activeUser.getFailedAttempts());
            verify(userRepository).save(activeUser);
        }
    }

    // ======================== refresh ========================

    @Nested
    class Refresh {

        private final UUID userId = UUID.randomUUID();

        @Test
        void refresh_returnsNewAccessToken() {
            UserEntity user = createTestUser(userId, "test@example.com", UserEntity.State.active);
            Claims claims = createMockClaims(userId.toString(), "jti-123");
            when(jwtService.parseToken("refresh-token")).thenReturn(Optional.of(claims));
            when(tokenBlacklistService.isBlacklisted("jti-123")).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn("new-access");
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(1800L);

            AuthService.RefreshResult result = authService.refresh("refresh-token");

            assertEquals("new-access", result.accessToken());
        }

        @Test
        void refresh_blacklistedJti_throwsError() {
            Claims claims = createMockClaims(userId.toString(), "jti-123");
            when(jwtService.parseToken("refresh-token")).thenReturn(Optional.of(claims));
            when(tokenBlacklistService.isBlacklisted("jti-123")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.refresh("refresh-token"));
            assertEquals("invalid_refresh_token", ex.getErrorCode());
        }
    }

    // ======================== logout ========================

    @Nested
    class Logout {

        @Test
        void logout_addsJtiToBlacklist() {
            Claims claims = createMockClaims("sub", "jti-abc");
            when(jwtService.parseToken("refresh-token")).thenReturn(Optional.of(claims));

            authService.logout("refresh-token");

            verify(tokenBlacklistService).add("jti-abc");
        }
    }
}
