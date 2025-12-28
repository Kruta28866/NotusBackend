package com.notus.backend.configuration;

import com.notus.backend.auth.FirebaseAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        // publiczne testy
                        .requestMatchers("/api/health", "/api/test").permitAll()
                        // reszta API wymaga auth
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                );

        http.addFilterBefore(new FirebaseAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
