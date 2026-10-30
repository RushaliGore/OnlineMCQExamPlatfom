package com.exampro.assessment.account;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;
    private final String baseUrl;
    private final String fromAddress;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            Environment environment,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.mail.from}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }

    public boolean isConfigured() {
        String host = environment.getProperty("SMTP_HOST", "");
        return !host.isBlank();
    }

    public void sendVerificationEmail(String recipient, String token) {
        if (!isConfigured()) {
            throw new IllegalStateException("Email verification is not configured. Set SMTP_HOST and SMTP settings before registering students.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Email verification is unavailable because the SMTP mail sender could not be initialized.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Verify your ExamPro student account");
        message.setText("Verify your email address by opening this link within 24 hours:\n\n"
                + baseUrl + "/verify?token=" + token
                + "\n\nIf you did not create an ExamPro account, you can ignore this message.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("We could not send the verification email. Check the SMTP settings and try again.", exception);
        }
    }
}
