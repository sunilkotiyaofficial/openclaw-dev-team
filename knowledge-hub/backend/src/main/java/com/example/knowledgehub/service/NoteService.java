package com.example.knowledgehub.service;

import com.example.knowledgehub.domain.mongo.Note;
import com.example.knowledgehub.exception.ResourceNotFoundException;
import com.example.knowledgehub.repository.mongo.NoteRepository;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Note service — REACTIVE (Mono/Flux) end-to-end.
 *
 * <p>This service represents the "reactive" side of the Knowledge Hub:
 * non-blocking, declarative pipelines, backpressure-aware, runs on
 * Netty event loops.</p>
 *
 * <p><b>Reactive operator vocabulary (interview-asked):</b></p>
 * <ul>
 *   <li>{@code map} — synchronous transformation 1-to-1</li>
 *   <li>{@code flatMap} — async transformation 1-to-1 (returns Mono/Flux)</li>
 *   <li>{@code filter} — keep only matching elements</li>
 *   <li>{@code switchIfEmpty} — fallback when source completes empty</li>
 *   <li>{@code onErrorResume} — fallback on error (don't propagate)</li>
 *   <li>{@code zip} — combine multiple Monos/Fluxes</li>
 *   <li>{@code defer} — lazy evaluation, useful for retries</li>
 * </ul>
 *
 * <p><b>Interview talking point — when to use Mono vs Flux:</b></p>
 * <blockquote>
 * "Mono = 0 or 1 element. Flux = 0 to N elements. The classic confusion:
 * a {@code findById} returns Mono (one or empty), {@code findAll} returns
 * Flux (potentially many). The Flux to Mono adapter when you want a List is
 * {@code .collectList()} — but try to keep streams streaming, only collect
 * at the API boundary."
 * </blockquote>
 */
@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    // ═══ Reactive CRUD ═══════════════════════════════════════════════════

    /**
     * Find one — Mono returns 0 or 1.
     * {@code switchIfEmpty} provides a fallback when not found.
     */
    @Timed("note.find_by_id")
    public Mono<Note> findById(String id) {
        return noteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Note", id)));
    }

    /**
     * Stream all notes for a topic — Flux can emit 0..N elements.
     *
     * <p>Note the {@code .doOnNext} for side-effect logging without
     * affecting the stream.</p>
     */
    @Timed("note.find_by_topic")
    public Flux<Note> findByTopic(Long topicId) {
        return noteRepository.findByTopicId(topicId)
                .doOnNext(note -> log.debug("Streaming note: {}", note.getId()));
    }

    public Mono<Note> create(Note note) {
        return noteRepository.save(note)
                .doOnSuccess(saved -> log.info("Created note id={}", saved.getId()));
    }

    /**
     * Update with optimistic locking — if another writer modified the note,
     * MongoDB throws OptimisticLockingFailureException.
     *
     * <p>The pipeline:</p>
     * <ol>
     *   <li>{@code findById} → Mono&lt;Note&gt;</li>
     *   <li>{@code flatMap} — async transform: load existing + apply changes</li>
     *   <li>{@code save} — returns Mono&lt;Note&gt; with updated state</li>
     * </ol>
     */
    public Mono<Note> update(String id, Note updates) {
        return findById(id)
                .flatMap(existing -> {
                    // Snapshot current content as a version before overwriting
                    if (!existing.getContent().equals(updates.getContent())) {
                        existing.getVersions().add(new Note.NoteVersion(
                                existing.getContent(),
                                "system",
                                Instant.now(),
                                "Auto-snapshot before update"));
                    }
                    existing.setTitle(updates.getTitle());
                    existing.setContent(updates.getContent());
                    existing.setTags(updates.getTags());
                    return noteRepository.save(existing);
                });
    }

    public Mono<Void> delete(String id) {
        return findById(id)
                .flatMap(noteRepository::delete);
    }

    // ═══ Server-Sent Events (live stream) ════════════════════════════════

    /**
     * Streams notes as they're created/updated — useful for real-time UI.
     *
     * <p>For a true live stream, this would need to integrate with a
     * Mongo change stream. The example here uses interval to demonstrate
     * the streaming endpoint pattern.</p>
     */
    public Flux<Note> streamLatestNotes(Long topicId) {
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> noteRepository.findByTopicId(topicId).take(10))
                .distinct(Note::getId);  // de-dupe across ticks
    }

    // ═══ Composition examples for interview ═════════════════════════════

    /**
     * Search notes by tag — demonstrates filter + flatMap pipeline.
     *
     * <p>Pipeline:</p>
     * <ol>
     *   <li>Find notes containing tag (Flux&lt;Note&gt;)</li>
     *   <li>Filter to those with non-empty content</li>
     *   <li>Sort by version count desc (richer history first)</li>
     *   <li>Take top 20</li>
     * </ol>
     */
    public Flux<Note> searchByTagRanked(String tag) {
        return noteRepository.findByTagsContaining(tag)
                .filter(n -> n.getContent() != null && !n.getContent().isBlank())
                .sort((a, b) -> Integer.compare(b.getVersions().size(),
                                                 a.getVersions().size()))
                .take(20);
    }

    /**
     * Combine multiple async sources with {@code Mono.zip}.
     *
     * <p>Use case: fetch a Note + its statistics in parallel, combine results.
     * Both calls happen concurrently — total time = max(t1, t2), not sum.</p>
     */
    public Mono<NoteWithStats> getNoteWithStats(String id) {
        Mono<Note> noteMono = findById(id);
        Mono<Long> noteCountMono = findById(id)
                .flatMap(n -> noteRepository.countByTopicId(n.getTopicId()));

        return Mono.zip(noteMono, noteCountMono)
                .map(tuple -> new NoteWithStats(tuple.getT1(), tuple.getT2()));
    }

    public record NoteWithStats(Note note, long siblingNoteCount) {}
}
