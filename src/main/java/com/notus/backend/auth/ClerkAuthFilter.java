package com.notus.backend.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.notus.backend.users.Role;
import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClerkAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ClerkAuthFilter.class);
    private final UserService userService;
    private final AuthTokenService authTokenService;

    public ClerkAuthFilter(UserService userService, AuthTokenService authTokenService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String token = null;
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        } else if ("/api/teacher/realtime/stream".equals(request.getRequestURI())) {
            token = request.getParameter("token");
        }

        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        if (token.startsWith("mock-dev-token:")) {
            String payload = token.substring("mock-dev-token:".length());
            String[] parts = payload.split(":", 2);
            String roleHint = parts.length == 2 ? parts[0] : null;
            String email = parts.length == 2 ? parts[1] : payload;
            String userId = "dev-user-" + (email.contains("@") ? email.split("@")[0] : email);
            
            request.setAttribute("clerk_email", email);
            request.setAttribute("clerk_name", "Dev User (" + email + ")");
            
            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authoritiesFor(userId, roleHint)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
            return;
        }

        if (tryAuthenticateLocalToken(token, request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT jwt = JWT.decode(token);
            String userId = jwt.getSubject();

            // Try to extract email from common Clerk claims
            String email = jwt.getClaim("email").asString();
            if (email == null) {
                // Some Clerk tokens might have it in other claims depending on JWT template
                var emailClaim = jwt.getClaim("email_address");
                if (!emailClaim.isMissing()) email = emailClaim.asString();
            }

            if (userId == null || userId.isBlank()) {
                log.warn("No userId in token subject for request: {}", request.getRequestURI());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Brak userId w tokenie");
                return;
            }

            // Try to extract name/username from common Clerk claims
            String name = jwt.getClaim("name").asString();
            if (name == null) {
                name = jwt.getClaim("username").asString();
            }

            // Set attributes as request attribute for controllers to use
            if (email != null) {
                request.setAttribute("clerk_email", email);
            }
            if (name != null) {
                request.setAttribute("clerk_name", name);
            }

            // We use a generic ROLE_USER here; the controller will perform detailed role checks via UserService
            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authoritiesFor(userId, null)
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error processing auth token", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Niepoprawny token Clerk");
        }

    }

    private boolean tryAuthenticateLocalToken(String token, HttpServletRequest request) {
        try {
            DecodedJWT jwt = authTokenService.verifyLocalToken(token);
            String userId = jwt.getSubject();
            String email = jwt.getClaim("email").asString();

            if (userId == null || userId.isBlank()) {
                return false;
            }

            request.setAttribute("clerk_email", email);
            userService.findExistingByUid(userId).ifPresent(user -> request.setAttribute("clerk_name", user.name()));

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authoritiesFor(userId, null)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<SimpleGrantedAuthority> authoritiesFor(String userId, String roleHint) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        userService.findExistingByUid(userId)
                .map(UserDto::role)
                .or(() -> parseRole(roleHint))
                .ifPresent(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));

        return authorities;
    }

    private java.util.Optional<Role> parseRole(String roleHint) {
        if (roleHint == null || roleHint.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            return java.util.Optional.of(Role.valueOf(roleHint.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }
}
