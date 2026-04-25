package com.notus.backend.grades;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDto {
    private Long id;
    private String subject;
    private String value;
    private LocalDateTime issueDate;
    private boolean isNew;
}
