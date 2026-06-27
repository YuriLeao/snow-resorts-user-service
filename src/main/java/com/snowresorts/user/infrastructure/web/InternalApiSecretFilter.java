package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.application.InternalApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the shared secret on internal service-to-service endpoints. These routes are listed
 * in {@code snow.security.public-paths} so JWT is not required; this filter is the sole gate.
 */
public class InternalApiSecretFilter extends OncePerRequestFilter {

    static final String INTERNAL_PATH_PREFIX = "/snow-resort-service/v1/users/internal/";

    private final InternalApiProperties properties;

    public InternalApiSecretFilter(InternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String presented = request.getHeader(properties.header());
        if (!properties.secret().equals(presented)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid internal API credentials.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
