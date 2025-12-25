package com.notus.backend.users;

public record UserDto(
        Long id,
        String email,
        String name,
        Role role,
        String indexNumber
) {}
