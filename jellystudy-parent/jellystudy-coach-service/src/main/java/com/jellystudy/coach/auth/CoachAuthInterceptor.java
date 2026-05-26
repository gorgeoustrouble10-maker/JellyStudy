package com.jellystudy.coach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.coach.exception.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CoachAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "coachUserId";

    private static final Set<String> PUBLIC_SUFFIXES = Set.of(
            "/config",
            "/leaderboard");

    private final AuthTokenStore authTokenStore;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/coach")) {
            return true;
        }
        if (isPublicCoachPath(uri)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        String token = stripBearer(auth);
        return authTokenStore.resolveUsername(token).map(username -> {
            request.setAttribute(ATTR_USER_ID, username);
            return true;
        }).orElseGet(() -> {
            writeUnauthorized(response);
            return false;
        });
    }

    private boolean isPublicCoachPath(String uri) {
        return PUBLIC_SUFFIXES.stream().anyMatch(uri::endsWith);
    }

    private static String stripBearer(String auth) {
        if (auth == null) {
            return "";
        }
        return auth.startsWith("Bearer ") ? auth.substring(7).trim() : auth.trim();
    }

    private void writeUnauthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResponse<Void> body = ApiResponse.error(401, "请先登录后再访问成长教练");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
