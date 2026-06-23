package com.example.sp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 10240);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            String path = request.getRequestURI();
            if (path != null && path.startsWith("/san-pham/sua")) {
                byte[] buf = wrapped.getContentAsByteArray();
                if (buf != null && buf.length > 0) {
                    String payload;
                    try {
                        payload = new String(buf, wrapped.getCharacterEncoding() == null ? "UTF-8" : wrapped.getCharacterEncoding());
                    } catch (Exception ex) {
                        payload = "[unreadable]";
                    }
                    log.info("[REQUEST-LOG] {} payload: {}", path, payload);
                } else {
                    log.info("[REQUEST-LOG] {} payload: [empty]", path);
                }
            }
        }
    }
}
