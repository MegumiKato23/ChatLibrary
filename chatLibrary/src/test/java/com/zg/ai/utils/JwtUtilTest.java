package com.zg.ai.utils;

import com.zg.ai.entity.po.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JwtUtilTest {

    @Test
    public void testGenerateAndValidateToken() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("testUser");
        user.setEmail("test@example.com");

        String token = JwtUtil.generateToken(user);
        Assertions.assertNotNull(token);

        String userId = JwtUtil.validateTokenAndGetUserId(token);
        Assertions.assertEquals("user-123", userId);
    }

    @Test
    public void testInvalidToken() {
        String userId = JwtUtil.validateTokenAndGetUserId("invalid-token");
        Assertions.assertNull(userId);
    }
}
