package com.example.knowledgehub.domain.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document — Note attached to a Topic.
 *
 * <p>Demonstrates document database patterns:</p>
 * <ul>
 *   <li>Schema-less but typed via Java class</li>
 *   <li>Embedded sub-documents (versions, attachments)</li>
 *   <li>Indexed fields for query performance</li>
 *   <li>Auditing via {@code @CreatedDate} / {@code @LastModifiedDate}</li>
 *   <li>Optimistic locking via {@code @Version}</li>
 * </ul>
 *
 * <p><b>Interview talking point — When MongoDB over PostgreSQL?</b></p>
 * <blockquote>
 * "I reach for MongoDB when the schema is genuinely flexible — user-generated
 * content where structure varies, embedded sub-documents that always travel
 * together (like a Note + its versions + attachments), or when access patterns
 * are document-centric, not relational. For everything else — strict schemas,
 * complex joins, transactional integrity — PostgreSQL wins."
 * </blockquote>
 *
 * <p><b>The split here:</b></p>
 * <ul>
 *   <li>Topic, Resource → PostgreSQL (structured, queried by category/status)</li>
 *   <li>Note → MongoDB (flexible content, version history embedded)</li>
 * </ul>
 */
@Document(collection = "notes")
public class Note {

    @Id
    private String id;

    /** FK-by-convention to Topic.id (cross-store reference, not enforced) */
    @Indexed
    private Long topicId;

    private String title;

    /** Markdown body — can be very long */
    private String content;

    /** Tags for filtering / categorization */
    @Indexed
    private List<String> tags = new ArrayList<>();

    /** Embedded version history — naturally suits document model */
    private List<NoteVersion> versions = new ArrayList<>();

    /** Embedded attachments — files associated with the note */
    private List<Attachment> attachments = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;

    /** Embedded document — version snapshot */
    public record NoteVersion(
            String content,
            String editedBy,
            Instant editedAt,
            String comment
    ) {}

    /** Embedded document — attachment metadata */
    public record Attachment(
            String filename,
            String contentType,
            Long sizeBytes,
            String storageUrl
    ) {}

    // ─── Default constructor required by Spring Data ─────────────────
    public Note() {}

    public Note(Long topicId, String title, String content) {
        this.topicId = topicId;
        this.title = title;
        this.content = content;
    }

    // Getters/Setters
    public String getId() { return id; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<NoteVersion> getVersions() { return versions; }
    public void setVersions(List<NoteVersion> versions) { this.versions = versions; }
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
