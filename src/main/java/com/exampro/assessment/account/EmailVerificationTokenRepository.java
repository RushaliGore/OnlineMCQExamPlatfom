package com.exampro.assessment.account;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    void deleteByStudent(StudentAccount student);
}
