package com.zg.ai.service.impl;

import com.zg.ai.entity.dto.user.ChangePasswordDTO;
import com.zg.ai.entity.dto.user.UserDTO;
import com.zg.ai.entity.dto.user.UserLoginDTO;
import com.zg.ai.entity.dto.user.UserRegisterDTO;
import com.zg.ai.entity.dto.user.UserUpdateDTO;
import com.zg.ai.entity.po.User;
import com.zg.ai.repository.UserRepository;
import com.zg.ai.service.UserService;
import com.zg.ai.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/*
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // 用户登录
    @Override
    public Mono<UserDTO> login(UserLoginDTO loginDTO) {
        return userRepository.findByUsername(loginDTO.getUsername())
                .filter(user -> PasswordUtil.checkPassword(loginDTO.getPassword(), user.getPasswordHash()))
                .flatMap(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(user).map(this::convertToDTO);
                });
    }

    // 用户注册
    @Override
    public Mono<UserDTO> register(UserRegisterDTO registerDTO) {
        return userRepository.findByUsername(registerDTO.getUsername())
                .flatMap(existing -> Mono.<UserDTO>error(new RuntimeException("用户名已存在")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User();
                    user.setEmail(registerDTO.getEmail());
                    user.setUsername(registerDTO.getUsername());
                    user.setPasswordHash(PasswordUtil.hashPassword(registerDTO.getPassword()));
                    user.setTotalDocuments(0);
                    return userRepository.save(user).map(this::convertToDTO);
                }));
    }

    // 更新用户信息
    @Override
    public Mono<UserDTO> update(String userId, UserUpdateDTO updateDTO) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setUsername(updateDTO.getUsername());
                    user.setEmail(updateDTO.getEmail());
                    return userRepository.save(user).map(this::convertToDTO);
                });
    }

    // 更改密码
    @Override
    public Mono<UserDTO> changePassword(String userId, ChangePasswordDTO changePasswordDTO) {
        return userRepository.findById(userId)
                .filter(user -> PasswordUtil.checkPassword(changePasswordDTO.getOldPassword(), user.getPasswordHash()))
                .flatMap(user -> {
                    user.setPasswordHash(PasswordUtil.hashPassword(changePasswordDTO.getNewPassword()));
                    return userRepository.save(user).map(this::convertToDTO);
                });
    }

    // 删除用户
    @Override
    public Mono<Void> delete(String userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setDeleted(true);
                    return userRepository.save(user);
                }).then();
    }

    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}