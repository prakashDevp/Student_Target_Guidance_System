package com.cgpatracker.controller;

import com.cgpatracker.dto.*;
import com.cgpatracker.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;

    // POST /api/students — Register new student
    @PostMapping
    public ResponseEntity<ApiResponse<StudentDTO.Response>> register(
            @Valid @RequestBody StudentDTO.Request req) {
        StudentDTO.Response data = studentService.registerStudent(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student registered successfully", data));
    }

    // GET /api/students — Get all students
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentDTO.Response>>> getAll() {
        List<StudentDTO.Response> data = studentService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success("Students fetched", data));
    }

    // GET /api/students/{id} — Get student by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> getById(@PathVariable Long id) {
        StudentDTO.Response data = studentService.getStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student fetched", data));
    }

    // POST /api/students/{id}/semesters — Add semester GPA
    @PostMapping("/{id}/semesters")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> addSemester(
            @PathVariable Long id,
            @Valid @RequestBody SemesterDTO.Request req) {
        StudentDTO.Response data = studentService.addSemesterGpa(id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Semester GPA recorded successfully", data));
    }

    // PUT /api/students/{id}/semesters/{semesterNumber} — Update semester GPA
    @PutMapping("/{id}/semesters/{semesterNumber}")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> updateSemester(
            @PathVariable Long id,
            @PathVariable Integer semesterNumber,
            @Valid @RequestBody SemesterDTO.Request req) {
        StudentDTO.Response data = studentService.updateSemesterGpa(id, semesterNumber, req);
        return ResponseEntity.ok(ApiResponse.success("Semester GPA updated", data));
    }
}
