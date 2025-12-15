package com.zg.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zg.ai.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User register(String username, String password);
}
