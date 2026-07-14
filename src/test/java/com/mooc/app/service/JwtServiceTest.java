package com.mooc.app.service;

import com.mooc.app.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long-48chars";
    private static final long ACCESS_TOKEN_EXPIRY = 1800L;
    private static final long REFRESH_TOKEN_EXPIRY = 604800L;

    private JwtService jwtService;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        jwtService = createJwtService(SECRET, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY);
        testUser = createTestUser(
                UUID.randomUUID(), "test@example.com", UserEntity.State.active);
    }

    // ── Constructor ──────────────────────────────────────────────

    @Nested
    class Constructor {

        @Test
        void constructor_validSecret_initializesService() {
            JwtService service = assertDoesNotThrow(
                    () -> createJwtService(SECRET, 1800, 604800));
            assertNotNull(service);
        }

        @Test
        void constructor_shortSecret_throwsWeakKeyException() {
            assertThrows(WeakKeyException.class,
                    () -> createJwtService("short", 1800, 604800));
        }
    }

    // ── generateAccessToken ──────────────────────────────────────

    @Nested
    class GenerateAccessToken {

        @Test
        void generateAccessToken_parseable() {
            String token = jwtService.generateAccessToken(testUser);
            Optional<Claims> result = jwtService.parseToken(token);
            assertTrue(result.isPresent());
        }

        @Test
        void generateAccessToken_subjectIsUserId() {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();
            assertEquals(testUser.getId().toString(), claims.getSubject());
        }

        @Test
        void generateAccessToken_emailClaimMatches() {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();
            assertEquals("test@example.com", claims.get("email", String.class));
        }

        @Test
        void generateAccessToken_stateClaimMatches() {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();
            assertEquals("active", claims.get("state", String.class));
        }

        @Test
        void generateAccessToken_expiryIsCorrect() {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();

            Date iat = claims.getIssuedAt();
            Date exp = claims.getExpiration();
            long durationSeconds = Duration.between(iat.toInstant(), exp.toInstant()).getSeconds();

            assertTrue(Math.abs(durationSeconds - ACCESS_TOKEN_EXPIRY) <= 2,
                    "exp - iat should be ~" + ACCESS_TOKEN_EXPIRY + " but was " + durationSeconds);
        }

        @Test
        void generateAccessToken_noJti() {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();
            assertNull(claims.getId());
        }

        @Test
        void generateAccessToken_differentUsersDifferentTokens() {
            UserEntity userA = createTestUser(UUID.randomUUID(), "a@test.com", UserEntity.State.active);
            UserEntity userB = createTestUser(UUID.randomUUID(), "b@test.com", UserEntity.State.locked);

            String tokenA = jwtService.generateAccessToken(userA);
            String tokenB = jwtService.generateAccessToken(userB);

            assertNotEquals(tokenA, tokenB);
        }
    }

    // ── generateRefreshToken ─────────────────────────────────────

    @Nested
    class GenerateRefreshToken {

        @Test
        void generateRefreshToken_parseable() {
            String token = jwtService.generateRefreshToken(testUser);
            Optional<Claims> result = jwtService.parseToken(token);
            assertTrue(result.isPresent());
        }

        @Test
        void generateRefreshToken_subjectIsUserId() {
            String token = jwtService.generateRefreshToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();
            assertEquals(testUser.getId().toString(), claims.getSubject());
        }

        @Test
        void generateRefreshToken_jtiIsValidUuid() {
            String token = jwtService.generateRefreshToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();

            String jti = claims.getId();
            assertNotNull(jti);
            assertDoesNotThrow(() -> UUID.fromString(jti));
        }

        @Test
        void generateRefreshToken_jtiIsUnique() {
            String token1 = jwtService.generateRefreshToken(testUser);
            String token2 = jwtService.generateRefreshToken(testUser);

            String jti1 = jwtService.parseToken(token1).orElseThrow().getId();
            String jti2 = jwtService.parseToken(token2).orElseThrow().getId();

            assertNotEquals(jti1, jti2);
        }

        @Test
        void generateRefreshToken_noEmailOrState() {
            String token = jwtService.generateRefreshToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();

            assertNull(claims.get("email"));
            assertNull(claims.get("state"));
        }

        @Test
        void generateRefreshToken_expiryIsCorrect() {
            String token = jwtService.generateRefreshToken(testUser);
            Claims claims = jwtService.parseToken(token).orElseThrow();

            Date iat = claims.getIssuedAt();
            Date exp = claims.getExpiration();
            long durationSeconds = Duration.between(iat.toInstant(), exp.toInstant()).getSeconds();

            assertTrue(Math.abs(durationSeconds - REFRESH_TOKEN_EXPIRY) <= 2,
                    "exp - iat should be ~" + REFRESH_TOKEN_EXPIRY + " but was " + durationSeconds);
        }
    }

    // ── parseToken — error paths ─────────────────────────────────

    @Nested
    class ParseTokenErrors {

        @Test
        void parseToken_wrongSignature_returnsEmpty() {
            JwtService otherService = createJwtService(
                    "a-completely-different-secret-key-that-is-long-enough", 1800, 604800);
            String token = otherService.generateAccessToken(testUser);

            Optional<Claims> result = jwtService.parseToken(token);
            assertTrue(result.isEmpty());
        }

        @Test
        void parseToken_expiredToken_returnsEmpty() {
            JwtService expiredService = createJwtService(SECRET, 0, 0);
            String token = expiredService.generateAccessToken(testUser);

            Optional<Claims> result = jwtService.parseToken(token);
            assertTrue(result.isEmpty());
        }

        @Test
        void parseToken_malformedString_returnsEmpty() {
            assertTrue(jwtService.parseToken("not.a.jwt").isEmpty());
        }

        @Test
        void parseToken_null_returnsEmpty() {
            assertTrue(jwtService.parseToken(null).isEmpty());
        }

        @Test
        void parseToken_emptyString_returnsEmpty() {
            assertTrue(jwtService.parseToken("").isEmpty());
        }

        @Test
        void parseToken_tamperedToken_returnsEmpty() {
            String token = jwtService.generateAccessToken(testUser);
            // flip a character in the middle of the payload section
            char[] chars = token.toCharArray();
            int mid = token.indexOf('.') + 5;
            chars[mid] = (chars[mid] == 'A') ? 'B' : 'A';
            String tampered = new String(chars);

            assertTrue(jwtService.parseToken(tampered).isEmpty());
        }
    }

    // ── Getters ──────────────────────────────────────────────────

    @Nested
    class Getters {

        @Test
        void getAccessTokenExpirySeconds_returnsConfiguredValue() {
            assertEquals(ACCESS_TOKEN_EXPIRY, jwtService.getAccessTokenExpirySeconds());
        }

        @Test
        void getRefreshTokenExpirySeconds_returnsConfiguredValue() {
            assertEquals(REFRESH_TOKEN_EXPIRY, jwtService.getRefreshTokenExpirySeconds());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static JwtService createJwtService(String secret, long accessExpiry, long refreshExpiry) {
        return new JwtService(secret, accessExpiry, refreshExpiry);
    }

    private static UserEntity createTestUser(UUID id, String email, UserEntity.State state) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setState(state);
        return user;
    }
}
