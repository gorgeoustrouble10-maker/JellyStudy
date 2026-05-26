package com.jellystudy.coach.controller;

import com.jellystudy.coach.service.AuthService;
import com.jellystudy.common.entity.AuthSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthSessionDTO register(@RequestBody Map<String, String> body) {
        return authService.register(
                body.get("username"),
                body.get("password"),
                body.get("displayName"));
    }

    @PostMapping("/login")
    public AuthSessionDTO login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("username"), body.get("password"));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Map.of("message", "已退出登录");
    }

    @GetMapping("/me")
    public AuthSessionDTO me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.me(authorization);
    }

    @GetMapping("/hint")
    public Map<String, String> hint() {
        return authService.loginHint();
    }
}
