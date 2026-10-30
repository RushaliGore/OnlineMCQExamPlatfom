package com.exampro.assessment.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "question_options", uniqueConstraints = {
        @UniqueConstraint(name = "uk_question_option_label", columnNames = { "question_id", "label" })
})
public class QuestionBankOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBankEntry question;

    @Column(nullable = false, length = 1)
    private String label;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected QuestionBankOption() {
    }

    QuestionBankOption(QuestionBankEntry question, String label, String text, boolean correct) {
        this.question = question;
        this.label = label;
        this.text = text;
        this.correct = correct;
    }

    void update(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    record Data(String label, String text, boolean correct) {
    }
}
