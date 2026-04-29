package com.example.knowledgehub.repository.mongo;

import com.example.knowledgehub.domain.mongo.Note;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Spring Data MongoDB repository for Note.
 *
 * <p>Returns {@code Mono<T>} for single results, {@code Flux<T>} for collections.
 * The whole stack is non-blocking — Netty event loop, MongoDB reactive driver,
 * Project Reactor.</p>
 *
 * <p><b>Interview talking point — Reactive vs Blocking:</b></p>
 * <blockquote>
 * "Reactive shines for high-fanout I/O — many small operations to many
 * downstreams. Single-thread Netty event loop handles thousands of
 * concurrent connections. Blocking with Virtual Threads handles the same
 * load with simpler code. My rule: reactive for streaming/SSE/WebSocket;
 * Virtual Threads for CRUD APIs. Both achieve high concurrency, just
 * different programming models."
 * </blockquote>
 */
@Repository
public interface NoteRepository extends ReactiveMongoRepository<Note, String> {

    // Derived queries — work the same as JPA repos
    Flux<Note> findByTopicId(Long topicId);

    Flux<Note> findByTagsContaining(String tag);

    Mono<Long> countByTopicId(Long topicId);

    // MongoDB query language via @Query
    // Find notes containing a phrase in title OR content (case-insensitive)
    @Query("""
            { $or: [
                { 'title': { $regex: ?0, $options: 'i' } },
                { 'content': { $regex: ?0, $options: 'i' } }
            ] }
            """)
    Flux<Note> searchByTextRegex(String pattern);

    // Find notes with at least N versions (rich edit history)
    @Query("{ 'versions': { $exists: true }, $expr: { $gte: [{ $size: '$versions' }, ?0] } }")
    Flux<Note> findWithVersionsAtLeast(int minVersions);
}
