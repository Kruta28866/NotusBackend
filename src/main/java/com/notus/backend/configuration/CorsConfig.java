package com.notus.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Adres frontendu uruchomionego przez Vite
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));

        // Metody, na które pozwalamy
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Nagłówki (później będzie tu Authorization)
        cfg.setAllowedHeaders(List.of("*"));

        // Pozwala na cookies/credentials (nie przeszkadza teraz, bywa potrzebne później)
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
