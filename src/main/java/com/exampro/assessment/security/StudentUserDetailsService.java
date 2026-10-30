package com.exampro.assessment.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.exampro.assessment.account.StudentAccount;
import com.exampro.assessment.account.StudentAccountRepository;

@Service
public class StudentUserDetailsService implements UserDetailsService {

    private final StudentAccountRepository studentRepository;

    public StudentUserDetailsService(StudentAccountRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        StudentAccount student = studentRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Student account was not found."));

        return User.withUsername(student.getEmail())
                .password(student.getPasswordHash())
                .disabled(!student.isEnabled())
                .roles("STUDENT")
                .build();
    }
}
