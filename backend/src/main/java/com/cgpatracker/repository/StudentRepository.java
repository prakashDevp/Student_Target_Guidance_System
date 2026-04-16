package com.cgpatracker.repository;

import com.cgpatracker.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByRegisterNumber(String registerNumber);

    boolean existsByEmail(String email);

    boolean existsByRegisterNumber(String registerNumber);

    List<Student> findByBranch(String branch);

    @Query("SELECT s FROM Student s WHERE s.currentCgpa >= s.targetCgpa")
    List<Student> findStudentsWhoAchievedTarget();

    @Query("SELECT s FROM Student s ORDER BY s.currentCgpa DESC")
    List<Student> findAllOrderByCgpaDesc();
}
