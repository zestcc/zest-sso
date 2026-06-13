package cn.zest.sso.server.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    @Test
    void shouldEncodeAndMatchPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = "admin123";
        String encoded = encoder.encode(raw);
        assertTrue(encoder.matches(raw, encoded));
    }

    @Test
    void shouldMatchSeededClientSecret() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$G1ADy6oi0ALHQCqTyvXHheSM9siuMbhU44YCBt9ZNNUzIiXZ2UzVq";
        assertTrue(encoder.matches("change-me-in-production", hash));
    }
}
