package com.example.knowledgehub.domain.jpa;

/** Learning status — a state machine. */
public enum Status {
    NOT_STARTED,
    IN_PROGRESS,
    STUDIED,
    QUIZ_READY,
    MASTERED
}
