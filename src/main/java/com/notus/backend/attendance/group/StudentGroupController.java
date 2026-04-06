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

    @GetMapping
    public List<StudentGroup> getAll() {
        return studentGroupRepository.findAll();
    }
}
