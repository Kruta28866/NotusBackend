package com.notus.backend.users;

public class RoleResolver {

    public static Role resolveRole(String email) {
        if (email == null || email.isBlank()) {
            return Role.TEACHER;
        }

        String normalized = email.trim().toLowerCase();

        if (normalized.matches("^s\\d{1,7}@pjwstk\\.edu\\.pl$")) {
            return Role.STUDENT;
        }

        return Role.TEACHER;
    }

    public static String extractIndexNumber(String email, Role role) {
        if (email == null || email.isBlank() || role != Role.STUDENT || !email.contains("@")) {
            return null;
        }

        return email.substring(0, email.indexOf('@'));
    }
}