package com.notus.backend.controller;

import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/me")
    public UserDto me(Authentication authentication, HttpServletRequest request) {
        String uid = (String) authentication.getPrincipal();
        String email = (String) request.getAttribute("clerk_email");

        return userService.findOrCreate(uid, email, "User");
    }
}
