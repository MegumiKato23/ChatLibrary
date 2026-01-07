package com.zg.ai.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    /**
     * BCrypt哈希密码
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * 验证密码
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}