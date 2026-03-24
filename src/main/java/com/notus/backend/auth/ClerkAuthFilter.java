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
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class ClerkAuthFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_DOMAINS =
            List.of("@pjwstk.edu.pl", "@gmail.com");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        
        System.out.println("DEBUG AUTH: Processing request to: " + request.getRequestURI() + " [" + request.getMethod() + "]");

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

            // Try to extract email from common Clerk claims
            String email = jwt.getClaim("email").asString();
            if (email == null) {
                // Some Clerk tokens might have it in other claims depending on JWT template
                var emailClaim = jwt.getClaim("email_address");
                if (!emailClaim.isMissing()) email = emailClaim.asString();
            }

            System.out.println("REQUEST URI: " + request.getRequestURI());
            System.out.println("SUBJECT: " + userId);
            System.out.println("CLAIMS: " + jwt.getClaims().keySet());

            if (userId == null || userId.isBlank()) {
                System.out.println("DEBUG AUTH: No userId in token subject!");
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
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
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