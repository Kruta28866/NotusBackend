package com.notus.backend.teachergroups;

import com.notus.backend.users.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherGroupRepository extends JpaRepository<TeacherGroup, Long> {
    List<TeacherGroup> findByTeacherAndActiveTrueOrderByCreatedAtDesc(Teacher teacher);
    Optional<TeacherGroup> findByIdAndTeacherAndActiveTrue(Long id, Teacher teacher);
    List<TeacherGroup> findByTeacherAndSubjectIgnoreCaseAndActiveTrue(Teacher teacher, String subject);

    @Query("""
            select g from TeacherGroup g
            where g.teacher = :teacher
              and g.active = true
              and lower(g.name) = lower(:name)
              and coalesce(lower(g.subject), '') = coalesce(lower(:subject), '')
              and coalesce(g.schoolYear, '') = coalesce(:schoolYear, '')
              and coalesce(g.semester, '') = coalesce(:semester, '')
            order by g.createdAt asc
            """)
    List<TeacherGroup> findActiveDuplicates(
            @Param("teacher") Teacher teacher,
            @Param("name") String name,
            @Param("subject") String subject,
            @Param("schoolYear") String schoolYear,
            @Param("semester") String semester
    );
}
