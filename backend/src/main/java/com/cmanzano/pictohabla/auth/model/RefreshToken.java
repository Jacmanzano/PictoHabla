package com.cmanzano.pictohabla.auth.model;

import com.cmanzano.pictohabla.teacher.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_hash", length = 64)
    private String replacedByTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RefreshToken() {
    }

    public RefreshToken(UUID id, Teacher teacher, String tokenHash, Instant expiresAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.teacher = teacher;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public void revoke(String replacedByHash) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.replacedByTokenHash = replacedByHash;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByTokenHash() {
        return replacedByTokenHash;
    }

    public void setReplacedByTokenHash(String replacedByTokenHash) {
        this.replacedByTokenHash = replacedByTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
