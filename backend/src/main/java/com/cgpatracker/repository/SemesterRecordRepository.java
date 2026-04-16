package com.cgpatracker.repository;

import com.cgpatracker.model.SemesterRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRecordRepository extends JpaRepository<SemesterRecord, Long> {

    List<SemesterRecord> findByStudentIdOrderBySemesterNumberAsc(Long studentId);

    Optional<SemesterRecord> findByStudentIdAndSemesterNumber(Long studentId, Integer semesterNumber);

    boolean existsByStudentIdAndSemesterNumber(Long studentId, Integer semesterNumber);

    @Query("SELECT COUNT(r) FROM SemesterRecord r WHERE r.student.id = :studentId")
    int countByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT AVG(r.gpa) FROM SemesterRecord r WHERE r.student.id = :studentId")
    Double calculateCgpaByStudentId(@Param("studentId") Long studentId);
}
