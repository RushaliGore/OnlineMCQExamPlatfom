# CodePrep Java Assessment Platform

CodePrep is a Spring Boot student assessment application using Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, and Hibernate. It includes student registration, sign-in, category-based exams, server-side scoring, answer explanations, and saved attempt history.

## Requirements

- Java 21 JDK with `JAVA_HOME` set to the JDK folder

## Run locally

From the project directory in PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The Maven wrapper downloads the project-pinned Maven distribution automatically. Open [http://localhost:8080](http://localhost:8080), create a student account, then sign in. Email verification is disabled by default for local development.

## Question bank

The bundled [question-bank.json](./src/main/resources/question-bank.json) contains 300 multiple-select questions: 20 for each of the 15 Java categories. At startup, the application validates the full bank and imports it into the database in a transaction. Importing is idempotent by stable question ID, so restarting the app updates existing entries instead of creating duplicates.

Questions and options use separate relational entities: each question owns four option rows, and each option stores its own correctness flag. This one-to-many model supports per-option scoring metadata and question-specific explanations and code/output content without a many-to-many join table. Correctness flags, explanations, and expected outputs are not included in the active exam page model; the server evaluates exact option-set matches and reveals review details only after submission.

The category page shows active database counts. Starting a category opens its own timed exam with navigation, answer persistence while navigating, a question palette, answer clearing, and review marking. Submission, scoring, and attempt persistence are handled by the server.

The default H2 database is stored under `data/` and survives restarts. For deployment, set `DATABASE_URL` to a PostgreSQL JDBC URL, along with `DATABASE_USERNAME` and `DATABASE_PASSWORD`. Set `APP_BASE_URL` to the public HTTPS URL and `COOKIE_SECURE=true` behind HTTPS.

## Optional email verification

For deployments that need verified email addresses, configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_FROM`, then set `REQUIRE_EMAIL_VERIFICATION=true`. Do not put real credentials in source files. Verification links expire after 24 hours and can only be used once.

## Assessment behavior

- Exactly 15 categories with 20 active questions each.
- Four answer options per question; multiple options may be correct.
- Exact-set scoring: a question earns one point only when the submitted selection matches the complete correct set. Unanswered questions receive zero points without an additional penalty.
- The browser does not receive correctness flags, explanations, or expected outputs before submission.
- The result page shows selected and correct answers, explanations, and expected output where applicable.
