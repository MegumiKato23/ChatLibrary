package com.zg.ai.controller;

import com.zg.ai.common.Result;
import com.zg.ai.entity.dto.user.ChangePasswordDTO;
import com.zg.ai.entity.dto.user.LoginResponse;
import com.zg.ai.entity.dto.user.UserDTO;
import com.zg.ai.entity.dto.user.UserLoginDTO;
import com.zg.ai.entity.dto.user.UserRegisterDTO;
import com.zg.ai.entity.dto.user.UserUpdateDTO;
import com.zg.ai.enums.ResultCode;
import com.zg.ai.service.UserService;
import com.zg.ai.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Tag(name = "User Management", description = "用户管理接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Mono<Result<LoginResponse>> login(@Valid @RequestBody UserLoginDTO loginDTO, ServerHttpResponse response) {
        return userService.login(loginDTO)
                .map(userDTO -> {
                    String token = JwtUtil.generateToken(userDTO);
                    ResponseCookie cookie = ResponseCookie.from("token", token)
                            .httpOnly(true)
                            .path("/")
                            .maxAge(60 * 60)
                            .build();
                    response.addCookie(cookie);
                    return Result.success(new LoginResponse(userDTO));
                })
                .defaultIfEmpty(Result.failure(ResultCode.FAILURE));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Mono<Result<LoginResponse>> register(@Valid @RequestBody UserRegisterDTO request,
            ServerHttpResponse response) {
        return userService.register(request)
                .map(userDTO -> {
                    String token = JwtUtil.generateToken(userDTO);
                    // Register success -> Auto login
                    ResponseCookie cookie = ResponseCookie.from("token", token)
                            .httpOnly(true)
                            .path("/")
                            .maxAge(60 * 60)
                            .build();
                    response.addCookie(cookie);
                    return Result.success(new LoginResponse(userDTO));
                })
                .onErrorResume(e -> Mono.just(Result.failure(e.getMessage())));
    }

    @Operation(summary = "更新用户信息")
    @PostMapping("/update/{userId}")
    public Mono<Result<UserDTO>> update(@PathVariable String userId, @Valid @RequestBody UserUpdateDTO request) {
        return userService.update(userId, request)
                .map(Result::success)
                .defaultIfEmpty(Result.failure(ResultCode.FAILURE))
                .onErrorResume(e -> Mono.just(Result.failure(e.getMessage())));
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password/{userId}")
    public Mono<Result<UserDTO>> changePassword(@PathVariable String userId,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        if (userId == null) {
            return Mono.just(Result.failure(ResultCode.UNAUTHORIZED));
        }

        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            return Mono.just(Result.failure("两次输入密码不一致"));
        }

        return userService.changePassword(userId, changePasswordDTO)
                .map(Result::success)
                .defaultIfEmpty(Result.failure("修改密码失败，请检查原密码是否正确"));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{userId}")
    public Mono<Result<Void>> delete(@PathVariable String userId) {
        return userService.delete(userId)
                .thenReturn(Result.<Void>success())
                .onErrorResume(e -> Mono.just(Result.failure(e.getMessage())));
    }
}