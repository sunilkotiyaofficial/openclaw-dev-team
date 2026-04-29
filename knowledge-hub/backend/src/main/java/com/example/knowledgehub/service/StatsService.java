package com.example.knowledgehub.service;

import com.example.knowledgehub.domain.jpa.Category;
import com.example.knowledgehub.domain.jpa.Topic;
import com.example.knowledgehub.repository.jpa.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Stats service — DEDICATED to Java Streams + Collections interview demos.
 *
 * <p><b>Use this file as a reference card for typical interview questions:</b></p>
 * <ul>
 *   <li>Stream collectors (groupingBy, partitioningBy, counting, averagingInt)</li>
 *   <li>Reducing operations (sum, max, custom reducer)</li>
 *   <li>flatMap for nested collections</li>
 *   <li>Stream.iterate, Stream.generate</li>
 *   <li>HashMap vs TreeMap vs ConcurrentHashMap</li>
 *   <li>Collection sorting (Comparator chaining)</li>
 * </ul>
 *
 * <p>Each method is small and self-contained — perfect to reference during
 * interview preparation or live demos.</p>
 */
@Service
public class StatsService {

    private final TopicRepository topicRepository;

    public StatsService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    // ═══ STREAM PATTERNS ═══════════════════════════════════════════════

    /**
     * Pattern 1: Filter + Map + Collect to List (most common stream)
     */
    @Transactional(readOnly = true)
    public List<String> p0TopicNames() {
        return topicRepository.findAll().stream()
                .filter(t -> t.getPriority().name().equals("P0"))
                .map(Topic::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Pattern 2: groupingBy with downstream collector
     * Result: Map<Category, Long> with count per category
     */
    @Transactional(readOnly = true)
    public Map<Category, Long> countByCategory() {
        return topicRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Topic::getCategory,
                        Collectors.counting()));
    }

    /**
     * Pattern 3: groupingBy + summarizing (count + min + max + avg in one pass)
     */
    @Transactional(readOnly = true)
    public Map<Category, IntSummaryStatistics> describeNamesByCategory() {
        return topicRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Topic::getCategory,
                        Collectors.summarizingInt(t -> t.getName().length())));
    }

    /**
     * Pattern 4: partitioningBy (binary split — Map<Boolean, List<T>>)
     */
    @Transactional(readOnly = true)
    public Map<Boolean, List<Topic>> partitionMastered() {
        return topicRepository.findAll().stream()
                .collect(Collectors.partitioningBy(
                        t -> t.getStatus().name().equals("MASTERED")));
    }

    /**
     * Pattern 5: toMap with merge function (handle duplicate keys)
     */
    @Transactional(readOnly = true)
    public Map<String, Topic> firstByCategoryString() {
        return topicRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getCategory().name(),
                        Function.identity(),
                        (existing, replacement) -> existing));  // keep first
    }

    /**
     * Pattern 6: Reduce — custom reducer that picks "longest" name
     */
    @Transactional(readOnly = true)
    public Optional<Topic> longestNameTopic() {
        return topicRepository.findAll().stream()
                .reduce((a, b) -> a.getName().length() >= b.getName().length() ? a : b);
    }

    /**
     * Pattern 7: flatMap — flatten nested collections
     * Get all distinct resource URLs across all topics
     */
    @Transactional(readOnly = true)
    public Set<String> allResourceUrls() {
        return topicRepository.findAll().stream()
                .flatMap(t -> t.getResources().stream())
                .map(r -> r.getUrl())
                .collect(Collectors.toSet());
    }

    /**
     * Pattern 8: Sorted with multi-level Comparator
     */
    @Transactional(readOnly = true)
    public List<Topic> topicsSortedByPriorityThenName() {
        return topicRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(Topic::getPriority)
                        .thenComparing(Topic::getName))
                .toList();  // Java 16+ unmodifiable list
    }

    /**
     * Pattern 9: Stream.iterate for sequence generation
     * Returns Fibonacci-like priority weights
     */
    public List<Integer> generateFibonacci(int n) {
        return Stream.iterate(new int[]{0, 1}, fib -> new int[]{fib[1], fib[0] + fib[1]})
                .limit(n)
                .map(fib -> fib[0])
                .toList();
    }

    /**
     * Pattern 10: Parallel stream — careful! Only for CPU-bound ops on large data
     */
    @Transactional(readOnly = true)
    public long countLongNamesParallel() {
        return topicRepository.findAll().parallelStream()
                .filter(t -> t.getName().length() > 30)
                .count();
    }

    // ═══ COLLECTION PATTERNS ════════════════════════════════════════════

    /**
     * HashMap — unordered, O(1) average lookup, default choice
     */
    public Map<Long, Topic> topicsById() {
        return topicRepository.findAll().stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
    }

    /**
     * TreeMap — sorted by key, O(log n) lookup, useful for range queries
     */
    public TreeMap<String, Topic> topicsByNameSorted() {
        return topicRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Topic::getName,
                        Function.identity(),
                        (a, b) -> a,
                        TreeMap::new));
    }

    /**
     * LinkedHashMap — preserves insertion order, useful for ordered output
     */
    public LinkedHashMap<Long, String> orderedTopicNames() {
        return topicRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Topic::getId,
                        Topic::getName,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /**
     * ConcurrentHashMap — for multi-threaded access without external sync.
     * O(1) lookup, lock striping internally for high concurrency.
     */
    public ConcurrentHashMap<Long, Topic> sharedTopicCache() {
        ConcurrentHashMap<Long, Topic> cache = new ConcurrentHashMap<>();
        topicRepository.findAll().forEach(t -> cache.put(t.getId(), t));
        return cache;
    }

    // ═══ STREAM EXPLAINER (interview answer ready) ═══════════════════════

    /**
     * Pattern 11: Demonstrates Stream is LAZY — operations don't execute
     * until a terminal operation (collect, count, forEach) is invoked.
     *
     * <p>Filter + map + filter run as ONE pass through the data, not three.
     * This is why streams are efficient even with chained operations.</p>
     */
    public List<String> streamLazyDemo() {
        return Stream.of("a", "bb", "ccc", "dddd", "eeeee")
                .filter(s -> s.length() > 1)         // intermediate (lazy)
                .map(String::toUpperCase)            // intermediate (lazy)
                .filter(s -> s.startsWith("C"))      // intermediate (lazy)
                .collect(Collectors.toList());        // terminal — NOW executes
    }

    /**
     * Pattern 12: IntStream — primitive stream avoids boxing overhead
     */
    public int sumOfSquares(int n) {
        return IntStream.rangeClosed(1, n)
                .map(i -> i * i)
                .sum();
    }
}
