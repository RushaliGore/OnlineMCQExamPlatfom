package com.exampro.assessment.web;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.exampro.assessment.account.RegistrationForm;
import com.exampro.assessment.account.StudentAccount;
import com.exampro.assessment.account.StudentAccountService;
import com.exampro.assessment.quiz.QuizAttempt;
import com.exampro.assessment.quiz.QuizService;

import jakarta.validation.Valid;

@Controller
public class PageController {

    private final StudentAccountService studentService;
    private final QuizService quizService;

    public PageController(
            StudentAccountService studentService,
            QuizService quizService) {
        this.studentService = studentService;
        this.quizService = quizService;
    }

    @GetMapping("/")
    public String home(Principal principal) {
        return principal == null ? "home" : "redirect:/dashboard";
    }

    @GetMapping("/home")
    public String landingPage() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", quizService.getCategories());
        model.addAttribute("questionCount", quizService.getActiveQuestionCount());
        return "categories";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        model.addAttribute("emailVerificationRequired", studentService.isEmailVerificationRequired());
        model.addAttribute("registrationAvailable", studentService.isRegistrationAvailable());
        return "register";
    }

    @PostMapping("/register")
    public String createAccount(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult,
            Model model) {
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("emailVerificationRequired", studentService.isEmailVerificationRequired());
            model.addAttribute("registrationAvailable", studentService.isRegistrationAvailable());
            return "register";
        }

        try {
            studentService.register(form);
            return studentService.isEmailVerificationRequired() ? "verification-sent" : "account-created";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("emailVerificationRequired", studentService.isEmailVerificationRequired());
            model.addAttribute("registrationAvailable", studentService.isRegistrationAvailable());
            model.addAttribute("formError", exception.getMessage());
            return "register";
        }
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam(name = "token", required = false) String token, Model model) {
        model.addAttribute("verified", studentService.verifyEmail(token));
        return "verify-result";
    }

    @GetMapping("/resend-verification")
    public String resendVerification() {
        return "resend-verification";
    }

    @PostMapping("/resend-verification")
    public String sendVerificationAgain(@RequestParam String email, Model model) {
        if (email.isBlank()) {
            model.addAttribute("formError", "Enter the email address used to register.");
            return "resend-verification";
        }

        try {
            studentService.resendVerification(email);
            model.addAttribute("submitted", true);
        } catch (IllegalStateException exception) {
            model.addAttribute("formError", exception.getMessage());
        }
        return "resend-verification";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        StudentAccount student = studentService.findByEmail(principal.getName());
        List<QuizAttempt> attempts = quizService.getRecentAttempts(student);

        model.addAttribute("student", student);
        model.addAttribute("attempts", attempts);
        model.addAttribute("questionCount", quizService.getActiveQuestionCount());
        model.addAttribute("categories", quizService.getCategories());
        model.addAttribute("bestScore", quizService.getBestScore(student));
        model.addAttribute("attemptCount", attempts.size());
        return "dashboard";
    }
}
