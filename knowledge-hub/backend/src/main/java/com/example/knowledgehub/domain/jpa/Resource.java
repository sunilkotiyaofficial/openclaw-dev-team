package com.example.knowledgehub.domain.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity — Resource (article, video, book) linked to a Topic.
 *
 * <p>Demonstrates the OWNING side of a bidirectional relationship.</p>
 *
 * <p><b>Interview talking point — Lazy vs Eager fetch:</b></p>
 * <blockquote>
 * "Default for {@code @ManyToOne} is EAGER — fetched whenever the entity loads.
 * I prefer LAZY for everything and use explicit @EntityGraph or JOIN FETCH
 * when the relationship is needed. Eager fetching causes N+1 queries and
 * cartesian product explosions in unexpected places."
 * </blockquote>
 */
@Entity
@Table(name = "resources", indexes = {
        @Index(name = "idx_resource_topic", columnList = "topic_id"),
        @Index(name = "idx_resource_type", columnList = "type")
})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String title;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceType type;

    @Size(max = 100)
    @Column(length = 100)
    private String source;  // e.g., "Anthropic Blog", "ACM", "O'Reilly"

    /**
     * Many-to-one: the OWNING side. Foreign key column "topic_id" lives here.
     * LAZY fetch — only loads when accessed (avoid N+1).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum ResourceType {
        ARTICLE, VIDEO, BOOK, COURSE, REPO, TALK
    }

    // Getters/Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource resource)) return false;
        return id != null && Objects.equals(id, resource.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
