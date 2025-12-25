package com.notus.backend.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            String path = System.getenv("FIREBASE_CREDENTIALS");
            if (path == null || path.isBlank()) {
                throw new IllegalStateException("Brak env FIREBASE_CREDENTIALS (ścieżka do serviceAccountKey.json)");
            }

            try (FileInputStream serviceAccount = new FileInputStream(path)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
            }

            log.info("Firebase Admin zainicjalizowany poprawnie.");
        } catch (Exception e) {
            throw new RuntimeException("Firebase init failed", e);
        }
    }
}
