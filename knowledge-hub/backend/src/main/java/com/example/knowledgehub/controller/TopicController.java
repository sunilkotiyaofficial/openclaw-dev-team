package com.example.knowledgehub.controller;

import com.example.knowledgehub.domain.jpa.Topic;
import com.example.knowledgehub.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Topic REST controller — TRADITIONAL (blocking) servlet style.
 *
 * <p>Even though this looks blocking, with Java 21 + Virtual Threads
 * configured globally, each request runs on a virtual thread —
 * non-pinning during DB I/O.</p>
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<Topic> findAll() {
        return topicService.findAll();
    }

    @GetMapping("/{id}")
    public Topic findById(@PathVariable Long id) {
        return topicService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Topic> create(@Valid @RequestBody Topic topic) {
        Topic saved = topicService.create(topic);
        return ResponseEntity
                .created(URI.create("/api/topics/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public Topic update(@PathVariable Long id, @Valid @RequestBody Topic topic) {
        return topicService.update(id, topic);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        topicService.delete(id);
    }

    /** Async endpoint — runs on virtual thread executor. */
    @PostMapping("/{category}/mark-mastered-async")
    public CompletableFuture<ResponseEntity<String>> bulkMarkMasteredAsync(
            @PathVariable com.example.knowledgehub.domain.jpa.Category category) {
        return topicService.bulkMarkMasteredAsync(category)
                .thenApply(count -> ResponseEntity.ok("Updated " + count + " topics"));
    }
}
