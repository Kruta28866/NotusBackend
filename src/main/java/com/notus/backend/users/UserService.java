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

    public UserDto findOrCreate(String clerkUserId, String email, String name) {
        // Prepare robust fallbacks for NOT NULL columns
        String finalEmail = (email != null && !email.isBlank()) ? email : clerkUserId + "@no-email.clerk";
        String finalName = (name != null && !name.isBlank()) ? name : "User_" + (clerkUserId.length() > 5 ? clerkUserId.substring(0, 5) : clerkUserId);
        Role targetRole = roleFromEmail(finalEmail);

        var user = repo.findByClerkUserId(clerkUserId).orElseGet(() -> {
            User u = new User();
            u.setClerkUserId(clerkUserId);
            u.setEmail(finalEmail);
            u.setName(finalName);
            u.setRole(targetRole);

            // index number from email if student
            if (targetRole == Role.STUDENT && finalEmail.contains("@")) {
                String local = finalEmail.split("@")[0];
                if (!local.isBlank()) u.setIndexNumber(local);
            }
            return repo.save(u);
        });

        boolean changed = false;

        // Sync name if it changed
        if (!finalName.equals(user.getName())) {
            user.setName(finalName);
            changed = true;
        }

        // Sync email if it changed (and was originally missing but now present)
        if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }

        // Sync role if it changed (unless ADMIN)
        if (user.getRole() != Role.ADMIN && user.getRole() != targetRole) {
            user.setRole(targetRole);
            changed = true;
        }

        if (changed) {
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

