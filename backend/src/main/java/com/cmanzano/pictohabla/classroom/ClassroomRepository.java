package com.cmanzano.pictohabla.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    List<Classroom> findByTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(UUID teacherId);

    Optional<Classroom> findByIdAndTeacherId(UUID id, UUID teacherId);

    boolean existsByIdAndTeacherId(UUID id, UUID teacherId);
}
