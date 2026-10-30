package com.cmanzano.pictohabla.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    List<Student> findByClassroomIdAndIsActiveTrue(UUID classroomId);

    Optional<Student> findByIdAndClassroomId(UUID id, UUID classroomId);

    Optional<Student> findByPseudonymCode(String pseudonymCode);

    boolean existsByPseudonymCode(String pseudonymCode);
}
