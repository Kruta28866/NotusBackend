package com.notus.backend.realtime;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

@RestController
@RequestMapping("/api/teacher/realtime")
public class TeacherRealtimeController {

    private final TeacherRealtimeService realtimeService;

    public TeacherRealtimeController(TeacherRealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Principal principal) {
        return realtimeService.subscribe(principal.getName());
    }
}
