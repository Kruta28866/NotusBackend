package com.notus.backend.controller;

import com.notus.backend.users.UserDto;
import com.notus.backend.users.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/me")
    public UserDto me(
            @RequestHeader("X-Firebase-Uid") String uid,
            @RequestHeader("X-Email") String email,
            @RequestHeader(value = "X-Name", required = false) String name
    ) {
        return userService.findOrCreate(uid, email, name);
    }
}
