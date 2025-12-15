package com.zg.ai.controller;

import com.zg.ai.entity.User;
import com.zg.ai.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(userService.register(request.getUsername(), request.getPassword()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }
}
