package com.notus.backend.controller;

import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/me")
    public UserDto me(Authentication authentication) {
        // principal = uid z filtra
        String uid = (String) authentication.getPrincipal();

        // details = mapa z filtra
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) authentication.getDetails();

        String email = details.get("email");
        String name = details.get("name");

        return userService.findOrCreate(uid, email, name);
    }
}
