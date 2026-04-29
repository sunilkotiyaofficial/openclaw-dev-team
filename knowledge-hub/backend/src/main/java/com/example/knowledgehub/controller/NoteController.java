package com.example.knowledgehub.controller;

import com.example.knowledgehub.domain.mongo.Note;
import com.example.knowledgehub.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Note REST controller — REACTIVE (Mono/Flux) end-to-end.
 *
 * <p>Demonstrates Server-Sent Events (SSE) endpoint pattern for
 * real-time updates to a UI.</p>
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/{id}")
    public Mono<Note> findById(@PathVariable String id) {
        return noteService.findById(id);
    }

    @GetMapping(value = "/by-topic/{topicId}")
    public Flux<Note> findByTopic(@PathVariable Long topicId) {
        return noteService.findByTopic(topicId);
    }

    @PostMapping
    public Mono<Note> create(@Valid @RequestBody Note note) {
        return noteService.create(note);
    }

    @PutMapping("/{id}")
    public Mono<Note> update(@PathVariable String id, @Valid @RequestBody Note note) {
        return noteService.update(id, note);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return noteService.delete(id);
    }

    /**
     * Server-Sent Events — streams notes to the client as they arrive.
     * Browser EventSource API consumes this directly.
     */
    @GetMapping(value = "/stream/{topicId}",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Note> stream(@PathVariable Long topicId) {
        return noteService.streamLatestNotes(topicId);
    }

    /** Search by tag — reactive Flux returns matching notes. */
    @GetMapping("/search/by-tag/{tag}")
    public Flux<Note> searchByTag(@PathVariable String tag) {
        return noteService.searchByTagRanked(tag);
    }
}
