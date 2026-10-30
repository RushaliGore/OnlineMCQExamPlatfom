package com.exampro.assessment.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAccountService {

    private final StudentAccountRepository studentRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final boolean emailVerificationRequired;
    private final SecureRandom secureRandom = new SecureRandom();

    public StudentAccountService(
            StudentAccountRepository studentRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.require-email-verification:false}") boolean emailVerificationRequired) {
        this.studentRepository = studentRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailVerificationRequired = emailVerificationRequired;
    }

    public boolean isRegistrationAvailable() {
        return !emailVerificationRequired || emailService.isConfigured();
    }

    public boolean isEmailVerificationRequired() {
        return emailVerificationRequired;
    }

    @Transactional
    public void register(RegistrationForm form) {
        if (!isRegistrationAvailable()) {
            throw new IllegalStateException("Email verification is required but no email service is configured. Ask the administrator to configure SMTP.");
        }
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be no more than 72 UTF-8 bytes.");
        }

        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account already exists for that email address.");
        }

        StudentAccount student = studentRepository.save(new StudentAccount(
                email,
                form.getDisplayName().trim(),
                passwordEncoder.encode(form.getPassword())));

        if (!emailVerificationRequired) {
            student.verifyEmail();
            return;
        }

        byte[] randomToken = new byte[32];
        secureRandom.nextBytes(randomToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        tokenRepository.save(new EmailVerificationToken(
                student,
                hashToken(token),
                Instant.now().plus(24, ChronoUnit.HOURS)));

        emailService.sendVerificationEmail(email, token);
    }

    @Transactional
    public boolean verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return tokenRepository.findByTokenHashAndExpiresAtAfter(hashToken(token), Instant.now())
                .map(verificationToken -> {
                    verificationToken.getStudent().verifyEmail();
                    tokenRepository.delete(verificationToken);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void resendVerification(String email) {
        if (emailVerificationRequired && !emailService.isConfigured()) {
            throw new IllegalStateException("Email verification is not configured. Contact the application administrator.");
        }

        if (!emailVerificationRequired) {
            return;
        }

        studentRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(student -> !student.isEnabled())
                .ifPresent(student -> {
                    tokenRepository.deleteByStudent(student);
                    byte[] randomToken = new byte[32];
                    secureRandom.nextBytes(randomToken);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
                    tokenRepository.save(new EmailVerificationToken(
                            student,
                            hashToken(token),
                            Instant.now().plus(24, ChronoUnit.HOURS)));
                    emailService.sendVerificationEmail(student.getEmail(), token);
                });
    }

    public StudentAccount findByEmail(String email) {
        return studentRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Student account was not found."));
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required but unavailable.", exception);
        }
    }
}
