package com.notus.backend.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.notus.backend.users.Role;
import com.notus.backend.users.RoleResolver;
import java.io.IOException;
import java.util.List;

public class ClerkAuthFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_DOMAINS =
            List.of("@pjwstk.edu.pl", "@gmail.com");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("BRAK LUB ZŁY AUTH HEADER dla: " + request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        System.out.println("AUTH HEADER: " + header);
        System.out.println("TOKEN START: " + token.substring(0, Math.min(30, token.length())));

        try {
            DecodedJWT jwt = JWT.decode(token);

            String userId = jwt.getSubject();

            System.out.println("REQUEST URI: " + request.getRequestURI());
            System.out.println("SUBJECT: " + userId);
            System.out.println("CLAIMS: " + jwt.getClaims().keySet());

            if (userId == null || userId.isBlank()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Brak userId w tokenie");
                return;
            }

// 🔥 NIE SPRAWDZAMY EMAIL
            Role role = Role.STUDENT;

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);

        } catch (Exception e) {
            System.out.println("BŁĄD W ClerkAuthFilter:");
            e.printStackTrace();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Niepoprawny token Clerk");
        }
    }
}