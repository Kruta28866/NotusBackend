package com.notus.backend.users.settings;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDto getSettings(Authentication authentication) {
        String clerkUserId = (String) authentication.getPrincipal();
        return settingsService.getSettings(clerkUserId);
    }

    @PutMapping
    public SettingsDto updateSettings(Authentication authentication, @RequestBody SettingsDto dto) {
        String clerkUserId = (String) authentication.getPrincipal();
        return settingsService.updateSettings(clerkUserId, dto);
    }

    @PostMapping("/deactivate")
    public SettingsDto toggleDeactivation(Authentication authentication) {
        String clerkUserId = (String) authentication.getPrincipal();
        return settingsService.toggleDeactivation(clerkUserId);
    }

    @PostMapping("/delete")
    public void markForDeletion(Authentication authentication) {
        String clerkUserId = (String) authentication.getPrincipal();
        settingsService.markForDeletion(clerkUserId);
    }
}
