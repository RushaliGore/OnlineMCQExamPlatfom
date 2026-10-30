package com.exampro.assessment.web;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import com.exampro.assessment.account.StudentAccount;
import com.exampro.assessment.account.StudentAccountService;
import com.exampro.assessment.quiz.QuizService;

@Controller
public class QuizController {

    private final QuizService quizService;
    private final StudentAccountService studentService;

    public QuizController(QuizService quizService, StudentAccountService studentService) {
        this.quizService = quizService;
        this.studentService = studentService;
    }

    @GetMapping("/quiz")
    public String quiz(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "invalidAnswers", defaultValue = "false") boolean invalidAnswers,
            Model model) {
        String selectedCategory = category == null ? QuizService.DEFAULT_CATEGORY : category;
        if (!quizService.isSupportedCategory(selectedCategory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown assessment category.");
        }
        model.addAttribute("category", selectedCategory);
        model.addAttribute("questions", quizService.getQuestionViews(selectedCategory));
        model.addAttribute("durationMinutes", quizService.getDurationMinutes(selectedCategory));
        model.addAttribute("invalidAnswers", invalidAnswers);
        return "quiz";
    }

    @PostMapping("/quiz/submit")
    public String submit(
            @RequestParam MultiValueMap<String, String> formData,
            @RequestParam String category,
            Principal principal,
            Model model) {
        try {
            StudentAccount student = studentService.findByEmail(principal.getName());
            model.addAttribute("category", category);
            model.addAttribute("result", quizService.grade(student, category, parseStringAnswers(formData)));
            return "result";
        } catch (IllegalArgumentException exception) {
            return "redirect:/quiz?category=" + UriUtils.encode(category, StandardCharsets.UTF_8) + "&invalidAnswers=true";
        }
    }

    private Map<String, List<String>> parseStringAnswers(MultiValueMap<String, String> formData) {
        Map<String, List<String>> answers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
            if (!entry.getKey().startsWith("q_")) {
                continue;
            }
            String questionId = entry.getKey().substring(2);
            answers.put(questionId, new ArrayList<>(entry.getValue()));
        }
        return answers;
    }
}
