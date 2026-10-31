package com.cmanzano.pictohabla.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Optional<Teacher> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Teacher> findByVerificationToken(String token);

    Optional<Teacher> findByPasswordResetToken(String token);

    long countByRole(Role role);

    boolean existsByRole(Role role);

    java.util.List<Teacher> findByRole(Role role);
}
