package com.exampro.assessment.quiz;

import java.time.Instant;

import com.exampro.assessment.account.StudentAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentAccount student;

    @Column(nullable = false)
    private int score;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private int questionCount;

    @Column(nullable = false)
    private Instant submittedAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(StudentAccount student, String category, int score, int questionCount) {
        this.student = student;
        this.category = category;
        this.score = score;
        this.questionCount = questionCount;
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public String getCategory() {
        return category;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public int getPercentage() {
        return questionCount == 0 ? 0 : (int) Math.round(score * 100.0 / questionCount);
    }
}
