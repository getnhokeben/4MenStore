package com.example.sp.boloc;

import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    private static final String LOGIN_PAGE = "/dang-nhap.html";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final List<String> protectedPrefixes = List.of(
            "/thong-ke",
            "/san-pham/",
            "/ban-hang-tai-quay",
            "/chat-ho-tro",
            "/hoa-don",
            "/thuoc-tinh/",
            "/dot-giam-gia",
            "/phieu-giam-gia",
            "/nhan-vien",
            "/khach-hang"
    );

    private final List<String> staffAllowedPrefixes = List.of(
            "/san-pham/trang-chu",
            "/trang-chu",
            "/ban-hang-tai-quay",
            "/chat-ho-tro",
            "/hoa-don",
            "/khach-hang"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isProtectedPagePath(path)) {
            HttpSession session = request.getSession(false);

            if (!isLoggedInAsEmployee(session)) {
                rejectUnauthenticated(request, response);
                return;
            }

            if (!isManager(session) && !isStaffAllowedPath(path)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());

                if (isBrowserPageRequest(request)) {
                    response.sendRedirect("/san-pham/trang-chu");
                }

                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPagePath(String path) {
        if (path == null) return false;

        if (path.equals(LOGIN_PAGE) || path.equals("/dang-nhap") || path.equals("/dang-nhap.html")) {
            return false;
        }

        return protectedPrefixes.stream().anyMatch(path::startsWith);
    }

    private boolean isStaffAllowedPath(String path) {
        if (path == null) return false;
        return staffAllowedPrefixes.stream().anyMatch(path::startsWith);
    }

    private boolean isLoggedInAsEmployee(HttpSession session) {
        if (session == null) return false;

        Integer id = (Integer) session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        return id != null;
    }

    private boolean isManager(HttpSession session) {
        if (session == null) return false;

        String role = normalizeRole(session.getAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO));

        if (role.isBlank()) {
            return false;
        }

        return !role.contains("nhan vien")
                && !role.contains("nhanvien")
                && !role.contains("staff")
                && !role.contains("employee");
    }

    private String normalizeRole(Object role) {
        String value = String.valueOf(role == null ? "" : role).trim();
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);

        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .toLowerCase(Locale.ROOT);
    }

    private void rejectUnauthenticated(HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "session");

        if (isBrowserPageRequest(request)) {
            response.sendRedirect(LOGIN_PAGE);
        }
    }

    private boolean isBrowserPageRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        boolean wantsHtml = accept != null && accept.contains("text/html");

        return wantsHtml || !"XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }
}
