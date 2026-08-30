package com.example.sp.boloc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Adds browser protections without changing the existing page scripts. */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    // Thực hiện xử lý nghiệp vụ của hàm do filter internal.
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // QR scanning needs camera access from pages served by this application.
        response.setHeader("Permissions-Policy", "camera=(self), microphone=(), geolocation=()");
        filterChain.doFilter(request, response);
    }
}
