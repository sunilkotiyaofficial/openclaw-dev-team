package com.example.knowledgehub.domain.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA Entity — relational model for Topic.
 *
 * <p>Demonstrates several JPA fundamentals:</p>
 * <ul>
 *   <li>{@code @Entity} + {@code @Table} mapping</li>
 *   <li>Primary key strategy (IDENTITY for auto-increment)</li>
 *   <li>{@code @Enumerated(STRING)} — store enum names not ordinals (refactor-safe)</li>
 *   <li>{@code @Column} with constraints</li>
 *   <li>{@code @OneToMany} bidirectional relationship with {@link Resource}</li>
 *   <li>Auditing fields ({@code created_at}, {@code updated_at})</li>
 *   <li>Bean Validation annotations on the entity for defensive validation</li>
 * </ul>
 *
 * <p><b>Interview talking point — JPA relationships:</b></p>
 * <blockquote>
 * "Bidirectional relationships — like Topic↔Resource — require careful
 * lifecycle management. {@code cascade=ALL} means saving a Topic saves
 * its Resources. {@code orphanRemoval=true} deletes orphaned children
 * automatically. Both should be deliberate choices, not defaults —
 * cascade-all on heavyweight relationships causes performance issues."
 * </blockquote>
 *
 * <p><b>Note on {@code @Version}:</b> Used for optimistic locking. JPA throws
 * {@code OptimisticLockException} if two transactions try to update the
 * same row concurrently — preventing lost updates without locking.</p>
 */
@Entity
@Table(name = "topics", indexes = {
        @Index(name = "idx_topic_category", columnList = "category"),
        @Index(name = "idx_topic_status", columnList = "status")
})
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.P1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOT_STARTED;

    /**
     * One-to-many: a Topic has many Resources.
     * mappedBy on the OWNED side; foreign key lives on Resource.
     */
    @OneToMany(mappedBy = "topic",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<Resource> resources = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Optimistic locking — auto-incremented by JPA on each update. */
    @Version
    private Long version;

    // ─── JPA lifecycle callbacks for auditing ───────────────────────────
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ─── Helper for bidirectional relationship management ──────────────
    public void addResource(Resource resource) {
        resources.add(resource);
        resource.setTopic(this);
    }

    public void removeResource(Resource resource) {
        resources.remove(resource);
        resource.setTopic(null);
    }

    // ─── Getters / Setters (no setter for id — JPA manages it) ─────────
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<Resource> getResources() { return resources; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // equals/hashCode — based on ID for entities (interview question!)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Topic topic)) return false;
        return id != null && Objects.equals(id, topic.id);
    }

    @Override
    public int hashCode() {
        // Use class hash, not ID — to be consistent with equals during lifecycle
        return getClass().hashCode();
    }
}
