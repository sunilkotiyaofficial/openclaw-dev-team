package com.example.knowledgehub.client;

import com.example.knowledgehub.service.ResourceService.ArticleMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * WebClient wrapper for external article metadata service.
 *
 * <p>Demonstrates modern Spring HTTP client patterns:</p>
 * <ul>
 *   <li>WebClient with reactive Mono response</li>
 *   <li>Timeout via {@code .timeout()} on the Mono</li>
 *   <li>Status code handling via {@code onStatus}</li>
 *   <li>Response transformation pipeline</li>
 * </ul>
 *
 * <p><b>Interview talking point — RestTemplate vs WebClient vs RestClient:</b></p>
 * <blockquote>
 * "RestTemplate is in maintenance mode. WebClient (since Spring 5) is the
 * modern reactive choice — non-blocking, runs on Netty. RestClient (Spring 6)
 * is the new synchronous client with the same fluent API as WebClient — use
 * it when you don't need reactive but want modern API. For new code, I
 * default to WebClient for async pipelines, RestClient for sync calls."
 * </blockquote>
 */
@Component
public class ExternalArticleClient {

    private final WebClient webClient;

    public ExternalArticleClient(WebClient.Builder webClientBuilder) {
        // The Builder is auto-configured by Spring with codecs, filters, etc.
        this.webClient = webClientBuilder
                .baseUrl("https://api.article-service.example.com")
                .defaultHeader("User-Agent", "knowledge-hub/1.0")
                .build();
    }

    /**
     * Fetch metadata for an article.
     *
     * <p>The full chain:</p>
     * <ol>
     *   <li>{@code .get().uri()} — issue HTTP GET</li>
     *   <li>{@code .retrieve()} — convert response to Mono</li>
     *   <li>{@code .onStatus()} — map error status codes to exceptions</li>
     *   <li>{@code .bodyToMono()} — deserialize JSON to record</li>
     *   <li>{@code .timeout()} — fail fast at 2 seconds</li>
     * </ol>
     */
    public Mono<ArticleMetadata> fetchMetadata(Long resourceId) {
        return webClient.get()
                .uri("/v1/articles/{id}/metadata", resourceId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.error(new IllegalArgumentException(
                                "Article " + resourceId + " not found")))
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> Mono.error(new RuntimeException(
                                "Article service unavailable")))
                .bodyToMono(ArticleMetadata.class)
                .timeout(Duration.ofSeconds(2));
    }
}
