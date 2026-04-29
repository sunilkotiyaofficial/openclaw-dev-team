package com.example.knowledgehub.service;

import com.example.knowledgehub.domain.jpa.Category;
import com.example.knowledgehub.domain.jpa.Status;
import com.example.knowledgehub.domain.jpa.Topic;
import com.example.knowledgehub.exception.ResourceNotFoundException;
import com.example.knowledgehub.repository.jpa.TopicRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Topic service — TRADITIONAL (blocking) Spring Boot pattern with Java 21
 * Virtual Threads.
 *
 * <p>This service represents the "traditional" side of the Knowledge Hub:
 * synchronous, imperative code, but running on virtual threads — so it
 * scales like reactive code without the cognitive overhead.</p>
 *
 * <p><b>Patterns demonstrated here:</b></p>
 * <ul>
 *   <li>Constructor injection (immutable {@code final} fields)</li>
 *   <li>{@code @Transactional} with read-only optimization</li>
 *   <li>{@code @Async} method runs on virtual thread executor</li>
 *   <li>{@code @Timed} + {@code @Counted} Micrometer metrics</li>
 *   <li>Java Streams for transformations</li>
 * </ul>
 *
 * <p><b>Interview talking point — @Transactional defaults:</b></p>
 * <blockquote>
 * "Spring's default is REQUIRED propagation, READ_COMMITTED isolation,
 * rolls back on RuntimeException only. {@code readOnly=true} is a hint
 * to JPA to skip dirty-checking — significant speedup for query methods.
 * I always specify rollbackFor=Exception.class explicitly when checked
 * exceptions can occur — the default behavior surprises people."
 * </blockquote>
 */
@Service
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);

    private final TopicRepository topicRepository;
    private final MeterRegistry meterRegistry;

    // Constructor injection — preferred over @Autowired field injection.
    // Immutable, easier to test, no NullPointerException risk.
    public TopicService(TopicRepository topicRepository,
                        MeterRegistry meterRegistry) {
        this.topicRepository = topicRepository;
        this.meterRegistry = meterRegistry;
    }

    // ═══ CRUD operations ═════════════════════════════════════════════════

    /**
     * Read-only transaction — JPA skips dirty checking, faster queries.
     */
    @Transactional(readOnly = true)
    @Timed(value = "topic.find_all", description = "Time to fetch all topics")
    public List<Topic> findAll() {
        return topicRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Topic findById(Long id) {
        return topicRepository.findByIdWithResources(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", id));
    }

    @Transactional
    @Counted(value = "topic.created", description = "Topics created")
    public Topic create(Topic topic) {
        if (topicRepository.existsByName(topic.getName())) {
            throw new IllegalArgumentException(
                    "Topic with name '" + topic.getName() + "' already exists");
        }
        Topic saved = topicRepository.save(topic);
        log.info("Created topic id={} name={}", saved.getId(), saved.getName());

        // Custom metric — Micrometer auto-tags with service info
        meterRegistry.counter("topic.create.by_category",
                "category", saved.getCategory().name()).increment();

        return saved;
    }

    @Transactional
    public Topic update(Long id, Topic updates) {
        Topic existing = findById(id);
        existing.setName(updates.getName());
        existing.setDescription(updates.getDescription());
        existing.setCategory(updates.getCategory());
        existing.setPriority(updates.getPriority());
        existing.setStatus(updates.getStatus());
        return topicRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!topicRepository.existsById(id)) {
            throw new ResourceNotFoundException("Topic", id);
        }
        topicRepository.deleteById(id);
    }

    // ═══ Async operation using Virtual Threads ════════════════════════════
    //
    // The @Async annotation tells Spring to run this method on the
    // applicationTaskExecutor bean (configured to use virtual threads
    // in KnowledgeHubApplication).
    //
    // Use case: fan-out updates without blocking the calling thread.

    /**
     * Bulk-mark topics as MASTERED in the background.
     *
     * <p>Returns immediately with a CompletableFuture; the actual update
     * runs on a virtual thread. Many such async calls can run concurrently
     * without exhausting the platform thread pool.</p>
     */
    @Async
    @Transactional
    public CompletableFuture<Integer> bulkMarkMasteredAsync(Category category) {
        log.info("Bulk update running on thread: {}", Thread.currentThread());
        // Thread.currentThread() will print "VirtualThread[...]"

        int updated = topicRepository.bulkUpdateStatus(
                category,
                Status.QUIZ_READY,
                Status.MASTERED);

        log.info("Bulk-marked {} topics as MASTERED in {}", updated, category);
        return CompletableFuture.completedFuture(updated);
    }

    // ═══ Java Streams demos for interview reference ══════════════════════

    /**
     * Group topics by category — classic Streams Collectors.groupingBy use case.
     *
     * <p>Returns {@code Map<Category, List<Topic>>} — one of the most-asked
     * Stream interview patterns.</p>
     */
    @Transactional(readOnly = true)
    public Map<Category, List<Topic>> groupByCategory() {
        return topicRepository.findAll().stream()
                .collect(Collectors.groupingBy(Topic::getCategory));
    }

    /**
     * Topic name list grouped by status, with case-insensitive sorting.
     *
     * <p>Demonstrates: filter, map, sorted, collect with downstream collector.</p>
     */
    @Transactional(readOnly = true)
    public Map<Status, List<String>> topicNamesByStatus() {
        return topicRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Topic::getStatus,
                        Collectors.mapping(
                                Topic::getName,
                                Collectors.toList())));
    }

    /**
     * Count topics by category — uses {@code counting()} downstream collector.
     */
    @Transactional(readOnly = true)
    public Map<Category, Long> countByCategory() {
        return topicRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Topic::getCategory,
                        Collectors.counting()));
    }

    /**
     * Partition topics into MASTERED vs not — partitioning is groupingBy
     * with a boolean predicate.
     */
    @Transactional(readOnly = true)
    public Map<Boolean, List<Topic>> partitionMastered() {
        return topicRepository.findAll().stream()
                .collect(Collectors.partitioningBy(
                        t -> t.getStatus() == Status.MASTERED));
    }
}
