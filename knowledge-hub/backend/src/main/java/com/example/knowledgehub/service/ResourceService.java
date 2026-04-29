package com.example.knowledgehub.service;

import com.example.knowledgehub.client.ExternalArticleClient;
import com.example.knowledgehub.domain.jpa.Resource;
import com.example.knowledgehub.exception.ResourceNotFoundException;
import com.example.knowledgehub.repository.jpa.ResourceRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Resource service — demonstrates {@code WebClient} with Resilience4j.
 *
 * <p>Patterns demonstrated:</p>
 * <ul>
 *   <li>{@code @CircuitBreaker} — fast-fail when downstream is unhealthy</li>
 *   <li>{@code @Retry} — handle transient failures with backoff</li>
 *   <li>{@code @Bulkhead} — limit concurrent calls (resource isolation)</li>
 *   <li>{@code @TimeLimiter} — fail-fast on slow downstreams</li>
 *   <li>Fallback methods — graceful degradation on failure</li>
 * </ul>
 *
 * <p><b>Interview talking point — Resilience4j patterns layered:</b></p>
 * <blockquote>
 * "These patterns compose. Time Limiter fails the call after N ms; Retry
 * triggers on the failure (with backoff); after K failures, Circuit Breaker
 * opens for a cooldown. Bulkhead limits how many of these chains can run
 * concurrently — protects the JVM thread pool from one slow downstream
 * exhausting it. The fallback method runs when ALL of these protections
 * trigger — always provide a fallback for graceful UX."
 * </blockquote>
 */
@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;
    private final ExternalArticleClient articleClient;

    public ResourceService(ResourceRepository resourceRepository,
                           ExternalArticleClient articleClient) {
        this.resourceRepository = resourceRepository;
        this.articleClient = articleClient;
    }

    // ═══ Standard CRUD ═════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Resource findById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }

    @Transactional
    public Resource create(Resource resource) {
        return resourceRepository.save(resource);
    }

    // ═══ Resilient external call with WebClient + Resilience4j ═════════

    /**
     * Enrich a resource with external article metadata.
     *
     * <p>Annotations stack from outside-in:</p>
     * <ol>
     *   <li>{@code @CircuitBreaker} (outermost) — fail fast if circuit is open</li>
     *   <li>{@code @Retry} — retry transient failures (max 3 attempts)</li>
     *   <li>{@code @TimeLimiter} — bound max wait time per call</li>
     *   <li>{@code @Bulkhead} — limit concurrent in-flight calls</li>
     * </ol>
     *
     * <p>The {@code fallbackMethod} runs when any of these protections trigger.
     * The fallback signature must match the original + a Throwable parameter.</p>
     */
    @CircuitBreaker(name = "articleService", fallbackMethod = "enrichFallback")
    @Retry(name = "articleService")
    @TimeLimiter(name = "articleService")
    @Bulkhead(name = "articleService", type = Bulkhead.Type.SEMAPHORE)
    public CompletableFuture<EnrichedResource> enrichWithExternalMetadata(Long resourceId) {
        log.info("Enriching resource {} via external service", resourceId);

        return articleClient.fetchMetadata(resourceId)
                .toFuture()
                .thenApply(metadata -> {
                    Resource resource = findById(resourceId);
                    return new EnrichedResource(resource, metadata);
                });
    }

    /**
     * Fallback method — same signature + Throwable.
     *
     * <p>Triggered when:</p>
     * <ul>
     *   <li>Circuit breaker is OPEN</li>
     *   <li>All retries exhausted</li>
     *   <li>Time limit exceeded</li>
     *   <li>Bulkhead semaphore unavailable</li>
     * </ul>
     */
    public CompletableFuture<EnrichedResource> enrichFallback(Long resourceId, Throwable t) {
        log.warn("Enrichment fallback for resource {}: {}", resourceId, t.getMessage());
        Resource resource = findById(resourceId);
        return CompletableFuture.completedFuture(
                new EnrichedResource(resource, ArticleMetadata.unavailable()));
    }

    public record EnrichedResource(Resource resource, ArticleMetadata metadata) {}

    public record ArticleMetadata(
            String title,
            String summary,
            int wordCount,
            int estimatedReadMinutes,
            boolean available
    ) {
        public static ArticleMetadata unavailable() {
            return new ArticleMetadata(null, "Metadata unavailable", 0, 0, false);
        }
    }
}
