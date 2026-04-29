package com.example.knowledgehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Knowledge Hub — Spring Boot 3.3 + Java 21 reference app.
 *
 * <p><b>Key configuration here:</b></p>
 * <ul>
 *   <li>Virtual Threads as the default Tomcat executor (Java 21 feature)</li>
 *   <li>{@code @EnableAsync} for {@code @Async} methods to use VT executor</li>
 *   <li>{@code @ConfigurationPropertiesScan} auto-discovers config beans</li>
 * </ul>
 *
 * <p><b>Interview talking point — Virtual Threads:</b></p>
 * <blockquote>
 * "Virtual threads were finalized in Java 21. The JVM multiplexes
 * thousands of virtual threads onto a small carrier-thread pool.
 * For I/O-bound workloads — DB queries, REST calls, file I/O —
 * each request gets its own virtual thread, blocks during I/O without
 * pinning a real OS thread. We get reactive-like throughput with
 * imperative code style. Caveat: avoid synchronized blocks (they pin),
 * prefer ReentrantLock instead."
 * </blockquote>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class KnowledgeHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeHubApplication.class, args);
    }

    /**
     * Configures Spring's async executor to use Java 21 virtual threads.
     *
     * <p>Without this bean, {@code @Async} methods use Spring's default
     * SimpleAsyncTaskExecutor (creates platform threads).</p>
     *
     * <p><b>Key insight:</b> Virtual threads are essentially free —
     * each costs ~few KB of heap, vs ~1MB per platform thread.
     * Spawning 10K virtual threads is fine; 10K platform threads
     * would crash a typical JVM.</p>
     */
    @Bean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
