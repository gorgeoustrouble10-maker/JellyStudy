package com.jellystudy.coach.service;

import com.jellystudy.coach.auth.AuthTokenStore;
import com.jellystudy.coach.document.AppUser;
import com.jellystudy.coach.repository.AppUserRepository;
import com.jellystudy.common.entity.AuthSessionDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long EXPIRES_HOURS = 168;

    private final AppUserRepository userRepository;
    private final AuthTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    void seedDemoUsers() {
        ensureUser("demo", "demo123", "演示用户");
        ensureUser("32308117", "123456", "吕宇轩");
    }

    private void ensureUser(String username, String rawPassword, String displayName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .createdAt(new Date())
                .build());
        log.info("已初始化账号: {}", username);
    }

    public AuthSessionDTO register(String username, String password, String displayName) {
        String normalized = normalizeUsername(username);
        validatePassword(password);
        if (userRepository.existsByUsername(normalized)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String name = (displayName != null && !displayName.isBlank()) ? displayName.trim() : normalized;
        userRepository.save(AppUser.builder()
                .username(normalized)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(name)
                .createdAt(new Date())
                .build());
        return login(normalized, password);
    }

    public AuthSessionDTO login(String username, String password) {
        String normalized = normalizeUsername(username);
        AppUser user = userRepository.findByUsername(normalized)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = tokenStore.issueToken(user.getUsername());
        return AuthSessionDTO.builder()
                .token(token)
                .userId(user.getUsername())
                .displayName(user.getDisplayName())
                .expiresInHours(EXPIRES_HOURS)
                .build();
    }

    public void logout(String token) {
        tokenStore.revoke(stripBearer(token));
    }

    public AuthSessionDTO me(String token) {
        String username = tokenStore.resolveUsername(stripBearer(token))
                .orElseThrow(() -> new IllegalArgumentException("登录已过期，请重新登录"));
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return AuthSessionDTO.builder()
                .token(stripBearer(token))
                .userId(user.getUsername())
                .displayName(user.getDisplayName())
                .expiresInHours(EXPIRES_HOURS)
                .build();
    }

    public Map<String, String> loginHint() {
        return Map.of(
                "demo", "demo / demo123",
                "student", "32308117 / 123456");
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 32) {
            throw new IllegalArgumentException("用户名长度需 3～32 字符");
        }
        if (!normalized.matches("[a-zA-Z0-9_\\u4e00-\\u9fa5]+")) {
            throw new IllegalArgumentException("用户名仅支持字母、数字、下划线或中文");
        }
        return normalized;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
    }

    private static String stripBearer(String token) {
        if (token == null) {
            return "";
        }
        return token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
    }
}
