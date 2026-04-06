package com.notus.backend.attendance.group;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student-groups")
@RequiredArgsConstructor
public class StudentGroupController {

    private final StudentGroupRepository studentGroupRepository;

    // Open to all authenticated users — students also need this for schedule display.
    // Access is gated by the global /api/** authenticated() rule in SecurityConfig.
    @GetMapping
    public List<StudentGroup> getAll() {
        return studentGroupRepository.findAll();
    }
}
