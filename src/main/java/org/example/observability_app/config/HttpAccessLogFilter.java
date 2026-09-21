package org.example.observability_app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HttpAccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            String query = request.getQueryString();
            String uri = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
            log.info("{} {} -> {} ({} ms) remote={}",
                    request.getMethod(),
                    uri,
                    response.getStatus(),
                    durationMs,
                    request.getRemoteAddr());
        }
    }
}
