package com.notus.backend.users;

import com.notus.backend.users.Role;

public record UserDto(
        Long id,
        String email,
        String name,
        Role role,
        String indexNumber
) {}
