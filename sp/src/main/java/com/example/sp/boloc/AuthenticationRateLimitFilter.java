package com.example.sp.boloc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits authentication and password-reset requests per client IP. It is an
 * in-memory guard for a single application instance; a multi-node deployment
 * should move this rule to its gateway or shared cache.
 */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final int LOGIN_LIMIT = 10;
    private static final int PASSWORD_RESET_LIMIT = 5;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    // Thực hiện xử lý nghiệp vụ của hàm do filter internal.
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        int limit = limitFor(request);
        if (limit == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = request.getRequestURI() + ':' + request.getRemoteAddr();
        Window window = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startedAt >= WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            return new Window(existing.startedAt, existing.count + 1);
        });

        discardExpiredEntries(now);
        if (window.count > limit) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MILLIS - (now - window.startedAt) + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Thực hiện xử lý nghiệp vụ của hàm limit for.
    private int limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return 0;
        String path = request.getRequestURI();
        if (path.endsWith("/forgot-password") || path.endsWith("/quen-mat-khau")) {
            return PASSWORD_RESET_LIMIT;
        }
        if (path.endsWith("/login") || path.endsWith("/register")) {
            return LOGIN_LIMIT;
        }
        return 0;
    }

    // Thực hiện xử lý nghiệp vụ của hàm discard expired entries.
    private void discardExpiredEntries(long now) {
        if (attempts.size() <= MAX_TRACKED_CLIENTS) return;
        attempts.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
    }

    // Thực hiện xử lý nghiệp vụ của hàm window.
    private record Window(long startedAt, int count) {
    }
}
