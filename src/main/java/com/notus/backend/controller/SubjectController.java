package com.notus.backend.controller;

import com.notus.backend.subject.Subject;
import com.notus.backend.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping("/seed")
    public void seedSubjects() {
        subjectService.seedSubjects();
    }

    @GetMapping
    public List<Subject> getSubjects() {
        return subjectService.getAll();
    }
}