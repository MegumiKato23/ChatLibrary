package com.zg.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zg.ai.entity.User;
import com.zg.ai.mapper.UserMapper;
import com.zg.ai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        String passwordHash = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        User user = this.getOne(new QueryWrapper<User>().eq("username", username).eq("password_hash", passwordHash));
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            this.updateById(user);
            return user;
        }
        return null;
    }

    @Override
    public User register(String username, String password) {
        if (this.count(new QueryWrapper<User>().eq("username", username)) > 0) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8)));
        user.setTotalDocuments(0);
        this.save(user);
        return user;
    }
}
