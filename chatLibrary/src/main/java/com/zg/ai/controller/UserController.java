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
import reactor.core.publisher.Mono;

/**
 * 用户管理控制器：提供用户登录、注册、更新、删除及查询接口
 */
@Tag(name = "User Management", description = "用户管理接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Mono<Result<LoginResponse>> login(@Valid @RequestBody UserLoginDTO loginDTO, ServerHttpResponse response) {
        // 从请求体中获取登录信息，并调用服务进行登录验证
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
        // 从请求体中获取注册信息，并调用服务进行用户注册
        return userService.register(request)
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
                .onErrorResume(e -> Mono.just(Result.failure(e.getMessage())));
    }

    @Operation(summary = "更新用户信息")
    @PostMapping("/update/{userId}")
    public Mono<Result<UserDTO>> update(@PathVariable String userId, @Valid @RequestBody UserUpdateDTO request) {
        // 从路径变量中获取用户 ID，并从请求体中获取更新信息，调用服务更新用户信息
        return userService.update(userId, request)
                .map(Result::success)
                .defaultIfEmpty(Result.failure(ResultCode.FAILURE))
                .onErrorResume(e -> Mono.just(Result.failure(e.getMessage())));
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password/{userId}")
    public Mono<Result<UserDTO>> changePassword(@PathVariable String userId,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        // 从路径变量中获取用户 ID，并从请求体中获取密码更新信息，调用服务修改用户密码
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
        // 从路径变量中获取用户 ID，并调用服务删除用户
        return userService.delete(userId)
                .thenReturn(Result.<Void>success())
                .onErrorResume(e -> Mono.just(Result.failure(e.getMessage())));
    }
}