package com.cgpatracker.service;

import com.cgpatracker.dto.*;
import com.cgpatracker.model.*;
import com.cgpatracker.model.SemesterRecord.ProgressStatus;
import com.cgpatracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final int TOTAL_SEMESTERS = 8;

    private final StudentRepository studentRepository;
    private final SemesterRecordRepository semesterRecordRepository;

    // ── Register Student ───────────────────────────────────
    @Transactional
    public StudentDTO.Response registerStudent(StudentDTO.Request req) {
        if (studentRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already registered: " + req.getEmail());
        if (studentRepository.existsByRegisterNumber(req.getRegisterNumber()))
            throw new RuntimeException("Register number already exists: " + req.getRegisterNumber());

        Student student = Student.builder()
                .name(req.getName())
                .email(req.getEmail())
                .registerNumber(req.getRegisterNumber())
                .branch(req.getBranch())
                .targetCgpa(req.getTargetCgpa())
                .build();

        student = studentRepository.save(student);
        return mapToResponse(student, List.of());
    }

    // ── Get Student ────────────────────────────────────────
    public StudentDTO.Response getStudent(Long id) {
        Student student = findStudentById(id);
        List<SemesterRecord> records = semesterRecordRepository
                .findByStudentIdOrderBySemesterNumberAsc(id);
        return mapToResponse(student, records);
    }

    // ── Get All Students ───────────────────────────────────
    public List<StudentDTO.Response> getAllStudents() {
        return studentRepository.findAllOrderByCgpaDesc().stream()
                .map(s -> {
                    List<SemesterRecord> records = semesterRecordRepository
                            .findByStudentIdOrderBySemesterNumberAsc(s.getId());
                    return mapToResponse(s, records);
                }).collect(Collectors.toList());
    }

    // ── Add Semester GPA ───────────────────────────────────
    @Transactional
    public StudentDTO.Response addSemesterGpa(Long studentId, SemesterDTO.Request req) {
        Student student = findStudentById(studentId);

        if (semesterRecordRepository.existsByStudentIdAndSemesterNumber(studentId, req.getSemesterNumber()))
            throw new RuntimeException("Semester " + req.getSemesterNumber() + " already recorded.");

        List<SemesterRecord> existingRecords = semesterRecordRepository
                .findByStudentIdOrderBySemesterNumberAsc(studentId);

        // Validate sequential entry
        int expectedNext = existingRecords.size() + 1;
        if (req.getSemesterNumber() != expectedNext)
            throw new RuntimeException("Please enter Semester " + expectedNext + " first.");

        // Calculate new CGPA
        double totalGpa = existingRecords.stream().mapToDouble(SemesterRecord::getGpa).sum()
                + req.getGpa();
        int totalSems = existingRecords.size() + 1;
        double newCgpa = totalGpa / totalSems;

        // Determine progress
        ProgressStatus status = determineStatus(newCgpa, student.getTargetCgpa(), totalSems);
        double requiredGpaRemaining = calculateRequiredGpa(newCgpa, student.getTargetCgpa(), totalSems);
        String guidance = generateGuidance(status, newCgpa, student.getTargetCgpa(),
                totalSems, requiredGpaRemaining, student.getName());

        SemesterRecord record = SemesterRecord.builder()
                .student(student)
                .semesterNumber(req.getSemesterNumber())
                .gpa(req.getGpa())
                .cgpaAfterSemester(Math.round(newCgpa * 100.0) / 100.0)
                .requiredGpaRemaining(requiredGpaRemaining)
                .progressStatus(status)
                .guidanceMessage(guidance)
                .build();

        semesterRecordRepository.save(record);

        // Update student
        student.setCurrentCgpa(Math.round(newCgpa * 100.0) / 100.0);
        student.setCurrentSemester(totalSems);
        studentRepository.save(student);

        existingRecords.add(record);
        return mapToResponse(student, existingRecords);
    }

    // ── Update Semester GPA ────────────────────────────────
    @Transactional
    public StudentDTO.Response updateSemesterGpa(Long studentId, Integer semesterNumber, SemesterDTO.Request req) {
        Student student = findStudentById(studentId);
        SemesterRecord record = semesterRecordRepository
                .findByStudentIdAndSemesterNumber(studentId, semesterNumber)
                .orElseThrow(() -> new RuntimeException("Semester " + semesterNumber + " not found"));

        record.setGpa(req.getGpa());

        List<SemesterRecord> allRecords = semesterRecordRepository
                .findByStudentIdOrderBySemesterNumberAsc(studentId);

        // Recalculate from that semester forward
        double totalGpa = allRecords.stream().mapToDouble(SemesterRecord::getGpa).sum();
        int totalSems = allRecords.size();
        double newCgpa = totalGpa / totalSems;

        ProgressStatus status = determineStatus(newCgpa, student.getTargetCgpa(), totalSems);
        double requiredGpa = calculateRequiredGpa(newCgpa, student.getTargetCgpa(), totalSems);
        String guidance = generateGuidance(status, newCgpa, student.getTargetCgpa(),
                totalSems, requiredGpa, student.getName());

        record.setCgpaAfterSemester(Math.round(newCgpa * 100.0) / 100.0);
        record.setRequiredGpaRemaining(requiredGpa);
        record.setProgressStatus(status);
        record.setGuidanceMessage(guidance);
        semesterRecordRepository.save(record);

        student.setCurrentCgpa(Math.round(newCgpa * 100.0) / 100.0);
        studentRepository.save(student);

        return mapToResponse(student, allRecords);
    }

    // ── Core CGPA Logic ────────────────────────────────────
    private ProgressStatus determineStatus(double cgpa, double target, int completedSems) {
        if (completedSems >= TOTAL_SEMESTERS) {
            return cgpa >= target ? ProgressStatus.ACHIEVED : ProgressStatus.BEHIND;
        }

        double remainingSems = TOTAL_SEMESTERS - completedSems;
        double totalPoints = cgpa * completedSems;
        double neededTotal = target * TOTAL_SEMESTERS;
        double neededFromHere = (neededTotal - totalPoints) / remainingSems;

        if (cgpa >= target) return ProgressStatus.AHEAD;
        if (neededFromHere <= 8.5) return ProgressStatus.ON_TRACK;
        if (neededFromHere <= 9.5) return ProgressStatus.BEHIND;
        return ProgressStatus.CRITICAL;
    }

    private double calculateRequiredGpa(double cgpa, double target, int completedSems) {
        if (completedSems >= TOTAL_SEMESTERS) return 0.0;
        double remainingSems = TOTAL_SEMESTERS - completedSems;
        double totalPoints = cgpa * completedSems;
        double neededTotal = target * TOTAL_SEMESTERS;
        double required = (neededTotal - totalPoints) / remainingSems;
        return Math.max(0, Math.min(10.0, Math.round(required * 100.0) / 100.0));
    }

    private String generateGuidance(ProgressStatus status, double cgpa, double target,
                                     int completedSems, double requiredGpa, String name) {
        int remaining = TOTAL_SEMESTERS - completedSems;
        String firstName = name.split(" ")[0];

        return switch (status) {
            case ACHIEVED -> String.format(
                    "🎉 Congratulations %s! You've achieved your target CGPA of %.2f! " +
                    "Your current CGPA is %.2f. You've successfully completed your academic goal " +
                    "across all 8 semesters. Excellent dedication!", firstName, target, cgpa);

            case AHEAD -> String.format(
                    "🌟 Outstanding %s! Your CGPA %.2f already exceeds your target of %.2f. " +
                    "You're %d semesters ahead with a comfortable lead. Keep this momentum — " +
                    "you need just %.2f GPA per remaining semester to stay on track. " +
                    "Consider aiming even higher!", firstName, cgpa, target, remaining, requiredGpa);

            case ON_TRACK -> String.format(
                    "✅ Great work %s! You're on track with a CGPA of %.2f against your target of %.2f. " +
                    "With %d semesters remaining, you need to score %.2f GPA per semester. " +
                    "Maintain consistency, revise regularly, and don't let distractions derail you. " +
                    "You're doing well — keep pushing!", firstName, cgpa, target, remaining, requiredGpa);

            case BEHIND -> String.format(
                    "⚠️ Attention %s! Your CGPA is %.2f, and your target is %.2f. " +
                    "You have %d semesters left and need %.2f GPA per semester to catch up. " +
                    "This is achievable but requires focused effort: attend all lectures, " +
                    "practice previous year papers, and seek professor guidance regularly. " +
                    "Step up your preparation now!", firstName, cgpa, target, remaining, requiredGpa);

            case CRITICAL -> String.format(
                    "🚨 Critical Alert %s! Your CGPA is %.2f against your target of %.2f. " +
                    "With %d semesters remaining, you'd need %.2f GPA per semester — which is extremely challenging. " +
                    "Consider revising your target or consulting your academic advisor immediately. " +
                    "Focus intensely on upcoming exams, use study groups, and prioritize high-weightage subjects. " +
                    "Every mark counts — don't give up!", firstName, cgpa, target, remaining, requiredGpa);
        };
    }

    // ── Mapping ────────────────────────────────────────────
    private StudentDTO.Response mapToResponse(Student student, List<SemesterRecord> records) {
        int completed = records.size();
        int remaining = TOTAL_SEMESTERS - completed;
        double reqGpa = calculateRequiredGpa(
                student.getCurrentCgpa() == null ? 0 : student.getCurrentCgpa(),
                student.getTargetCgpa(), completed);

        String overallStatus = completed == 0 ? "NOT_STARTED" :
                determineStatus(student.getCurrentCgpa(), student.getTargetCgpa(), completed).name();

        String motivational = completed == 0
                ? "🎯 Welcome! Your journey to CGPA " + student.getTargetCgpa() + " starts now. " +
                  "You need " + student.getTargetCgpa() + " GPA per semester consistently. Believe and achieve!"
                : records.get(records.size() - 1).getGuidanceMessage();

        List<SemesterDTO.Response> semDtos = records.stream().map(r ->
                SemesterDTO.Response.builder()
                        .id(r.getId())
                        .semesterNumber(r.getSemesterNumber())
                        .gpa(r.getGpa())
                        .cgpaAfterSemester(r.getCgpaAfterSemester())
                        .requiredGpaRemaining(r.getRequiredGpaRemaining())
                        .progressStatus(r.getProgressStatus())
                        .guidanceMessage(r.getGuidanceMessage())
                        .semesterLabel("Semester " + r.getSemesterNumber())
                        .recordedAt(r.getRecordedAt())
                        .build()
        ).collect(Collectors.toList());

        return StudentDTO.Response.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .registerNumber(student.getRegisterNumber())
                .branch(student.getBranch())
                .targetCgpa(student.getTargetCgpa())
                .currentCgpa(student.getCurrentCgpa())
                .currentSemester(student.getCurrentSemester())
                .semestersCompleted(completed)
                .semestersRemaining(remaining)
                .requiredGpaPerSemester(reqGpa)
                .overallStatus(overallStatus)
                .motivationalMessage(motivational)
                .createdAt(student.getCreatedAt())
                .semesterRecords(semDtos)
                .build();
    }

    private Student findStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }
}
