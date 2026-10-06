package com.ridelink.account.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // 256-bit test secret and 1 hour expiration
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 3600000L);
    }

    @Test
    @DisplayName("Should generate token and extract correct claims")
    void testGenerateTokenAndExtractClaims() {
        String token = jwtTokenProvider.generateToken(10L, "rider@ridelink.com", "PASSENGER", "Rider One");

        assertNotNull(token);
        assertTrue(token.length() > 20);

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("rider@ridelink.com", username);

        Long accountId = jwtTokenProvider.getAccountIdFromToken(token);
        assertEquals(10L, accountId);

        String role = jwtTokenProvider.getRoleFromToken(token);
        assertEquals("PASSENGER", role);

        boolean isValid = jwtTokenProvider.validateToken(token);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false when validating an invalid or corrupted token")
    void testValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.structure"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
