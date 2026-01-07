package com.zg.ai.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zg.ai.entity.dto.user.UserDTO;
import com.zg.ai.entity.po.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    private static final String SECRET_KEY = "MegumiKato2026MegumiKato2026MegumiKato2026"; // Must be at least 256 bits
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours

    /**
     * 生成JWT
     * @param user 用户
     * @return JWT字符串
     */
    public static String generateToken(User user) {
        return generateToken(user.getId(), user.getUsername(), user.getEmail());
    }

    public static String generateToken(UserDTO user) {
        return generateToken(user.getId(), user.getUsername(), user.getEmail());
    }

    private static String generateToken(String userId, String username, String email) {
        try {
            JWSSigner signer = new MACSigner(SECRET_KEY);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("username", username)
                    .claim("email", email)
                    .expirationTime(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error generating token", e);
            throw new RuntimeException("Error generating token", e);
        }
    }

    /**
     * 验证JWT并解析用户ID
     * @param token JWT字符串
     * @return 用户ID，如果无效返回null
     */
    public static String validateTokenAndGetUserId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(SECRET_KEY);
            
            if (signedJWT.verify(verifier)) {
                Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
                if (new Date().before(expirationTime)) {
                    return signedJWT.getJWTClaimsSet().getSubject();
                }
            }
        } catch (ParseException | JOSEException e) {
            log.warn("Invalid token: {}", e.getMessage());
        }
        return null;
    }
}
