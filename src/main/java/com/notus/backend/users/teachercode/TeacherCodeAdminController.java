package com.notus.backend.users.teachercode;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teacher-codes")
public class TeacherCodeAdminController {

    private final TeacherCodeService teacherCodeService;

    public TeacherCodeAdminController(TeacherCodeService teacherCodeService) {
        this.teacherCodeService = teacherCodeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherCodeDto generateCode(@RequestBody(required = false) GenerateTeacherCodeRequest request) {
        return teacherCodeService.generateCode(
                request != null ? request.code() : null,
                request != null ? request.expiresAt() : null,
                request != null ? request.usageLimit() : null
        );
    }

    @GetMapping
    public List<TeacherCodeDto> listActiveCodes() {
        return teacherCodeService.listActiveCodes();
    }

    @PostMapping("/{id}/disable")
    public TeacherCodeDto disableCode(@PathVariable Long id) {
        return teacherCodeService.deactivateCode(id);
    }
}
