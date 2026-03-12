package com.notus.backend.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FirebaseAuthFilter extends OncePerRequestFilter {

    // dopuszczamy tylko domenę uczelnianą
    private static final List<String> ALLOWED_DOMAINS =
            List.of("@pjwstk.edu.pl", "@gmail.com");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // brak tokena → lecimy dalej, endpointy publiczne działają, chronione zwrócą 401
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (token.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Brak tokena");
            return;
        }

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);

            String email = decoded.getEmail();
            boolean allowed = email != null && ALLOWED_DOMAINS.stream()
                    .anyMatch(d -> email.toLowerCase().endsWith(d));

            if (!allowed) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("Niedozwolona domena email");
                return;
            }

            String uid = decoded.getUid();
            String name = (decoded.getName() != null && !decoded.getName().isBlank()) ? decoded.getName() : "User";

            // wkładamy do security context
            var auth = new UsernamePasswordAuthenticationToken(
                    uid,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            // w details wrzucamy to co nam potrzebne w kontrolerach
            auth.setDetails(Map.of(
                    "email", email,
                    "name", name
            ));

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);

        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Niepoprawny token");
        }
    }
}
