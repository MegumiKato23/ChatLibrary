package com.zg.ai.service;

import com.zg.ai.entity.dto.user.ChangePasswordDTO;
import com.zg.ai.entity.dto.user.UserDTO;
import com.zg.ai.entity.dto.user.UserLoginDTO;
import com.zg.ai.entity.dto.user.UserRegisterDTO;
import com.zg.ai.entity.dto.user.UserUpdateDTO;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserDTO> login(UserLoginDTO loginDTO);

    Mono<UserDTO> register(UserRegisterDTO registerDTO);
    
    Mono<UserDTO> update(String userId, UserUpdateDTO updateDTO);

    Mono<UserDTO> changePassword(String userId, ChangePasswordDTO changePasswordDTO);
    
    Mono<Void> delete(String userId); 
}