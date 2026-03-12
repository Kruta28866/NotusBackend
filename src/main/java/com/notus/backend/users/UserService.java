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
        Role targetRole = roleFromEmail(email);

        var user = repo.findByFirebaseUid(firebaseUid).orElseGet(() -> {
            User u = new User();
            u.setFirebaseUid(firebaseUid);
            u.setEmail(email);
            u.setName((name == null || name.isBlank()) ? "User" : name);

            // ✅ rola wg domeny
            u.setRole(targetRole);

            // index tylko sensowny dla STUDENT (pjwstk)
            if (targetRole == Role.STUDENT && email != null && email.contains("@")) {
                String local = email.split("@")[0];
                if (!local.isBlank()) u.setIndexNumber(local);
            }

            return repo.save(u);
        });

        // ✅ jeśli user już istnieje, to (poza ADMIN) aktualizujemy rolę wg domeny
        if (email != null && user.getRole() != Role.ADMIN && user.getRole() != targetRole) {
            user.setRole(targetRole);

            // index dla STUDENT
            if (targetRole == Role.STUDENT && email.contains("@")) {
                String local = email.split("@")[0];
                user.setIndexNumber(local);
            }

            repo.save(user);
        }

        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getIndexNumber());
    }

    private Role roleFromEmail(String email) {
        if (email == null) return Role.STUDENT;

        String e = email.toLowerCase();
        if (e.endsWith("@gmail.com")) return Role.TEACHER;
        if (e.endsWith("@pjwstk.edu.pl")) return Role.STUDENT;

        // domyślnie (możesz też tu dać wyjątek)
        return Role.STUDENT;
    }
}
