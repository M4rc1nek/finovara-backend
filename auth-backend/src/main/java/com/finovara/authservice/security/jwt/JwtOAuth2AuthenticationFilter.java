package com.finovara.authservice.security.jwt;

import com.finovara.authservice.security.CustomUserDetails;
import com.finovara.authservice.security.SecurityProperties;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtOAuth2AuthenticationFilter extends OncePerRequestFilter {

    private static final String SET_PASSWORD_PATH = "/api/auth/set-password";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;
    private final JwtTokenResolver jwtTokenResolver;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = jwtTokenResolver.resolve(request).orElse(null);
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtService.isTokenValid(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtService.extractUserId(jwt);
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            CustomUserDetails userDetails = new CustomUserDetails(user);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            if (!user.isPasswordSet() && !isSetPasswordRequest(request)) {
                if (isWhitelistedRequest(request)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Password setup required");
                return;
            }
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("JWT authentication failed: {}", exception.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSetPasswordRequest(HttpServletRequest request) {
        return SET_PASSWORD_PATH.equals(normalizedPath(request));
    }

    private boolean isWhitelistedRequest(HttpServletRequest request) {
        String path = normalizedPath(request);
        return securityProperties.getWhitelist().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String normalizedPath(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && requestPath.startsWith(contextPath)) {
            return requestPath.substring(contextPath.length());
        }

        return requestPath;
    }

}
