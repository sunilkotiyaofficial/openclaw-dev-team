package com.example.knowledgehub.domain.jpa;

/** Topic priority — P0 must-know, P2 shows depth. */
public enum Priority {
    P0,  // Critical — must master
    P1,  // Important — should know
    P2   // Nice to have — shows depth
}
