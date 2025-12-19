package com.notus.backend.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UserDto findOrCreate(String firebaseUid, String email, String name) {
        var user = repo.findByFirebaseUid(firebaseUid).orElseGet(() -> {
            User u = new User();
            u.setFirebaseUid(firebaseUid);
            u.setEmail(email);
            u.setName((name == null || name.isBlank()) ? "User" : name);
            u.setRole(Role.STUDENT); // domyślna rola

            // index z local-part maila (tak jak robiłeś na froncie)
            String local = email != null ? email.split("@")[0] : null;
            if (local != null && !local.isBlank()) u.setIndexNumber(local);

            return repo.save(u);
        });

        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getIndexNumber());
    }
}
