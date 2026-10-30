package com.exampro.assessment.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_verification_token_hash", columnList = "token_hash", unique = true)
})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private StudentAccount student;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(StudentAccount student, String tokenHash, Instant expiresAt) {
        this.student = student;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public StudentAccount getStudent() {
        return student;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
