package com.example.knowledgehub.domain.jpa;

/**
 * User roles — stored as STRING in DB, used for authorization.
 *
 * <p>Mapped to Spring Security authorities with "ROLE_" prefix
 * (Spring's convention; required for {@code hasRole()} matchers).</p>
 *
 * <p><b>Interview talking point — why enum + DB store as STRING:</b></p>
 * <blockquote>
 * "Storing the enum NAME, not its ordinal, means I can reorder, add,
 * or remove enum values without breaking existing rows. Ordinal storage
 * is a classic refactoring trap."
 * </blockquote>
 */
public enum Role {
    USER,         // standard user — read access
    EDITOR,       // can create/update topics + notes
    ADMIN;        // full access including delete and user management

    /** Spring Security authority format with ROLE_ prefix. */
    public String authority() {
        return "ROLE_" + this.name();
    }
}
