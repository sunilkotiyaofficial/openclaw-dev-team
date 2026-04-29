package com.example.knowledgehub.repository.jpa;

import com.example.knowledgehub.domain.jpa.Category;
import com.example.knowledgehub.domain.jpa.Priority;
import com.example.knowledgehub.domain.jpa.Status;
import com.example.knowledgehub.domain.jpa.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Topic.
 *
 * <p>Demonstrates four query styles:</p>
 * <ol>
 *   <li><b>Inherited from JpaRepository</b> — save, findById, findAll, deleteById</li>
 *   <li><b>Derived query methods</b> — Spring parses the method name</li>
 *   <li><b>JPQL with @Query</b> — complex queries with named parameters</li>
 *   <li><b>Native SQL with @Query(nativeQuery=true)</b> — when JPQL isn't enough</li>
 * </ol>
 *
 * <p><b>Interview talking point — Spring Data JPA query options:</b></p>
 * <blockquote>
 * "Derived methods are great for simple queries — `findByStatus(Status status)` —
 * Spring parses the name and generates JPQL. For anything beyond ~3 conditions,
 * I prefer @Query with JPQL — explicit, refactor-safe with named params.
 * Native SQL is escape hatch for window functions, CTEs, or DB-specific
 * features. Avoid premature use — JPQL covers 95% of cases."
 * </blockquote>
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    // ═══ 1. Derived queries ════════════════════════════════════════════
    // Spring parses method name and auto-generates JPQL

    List<Topic> findByCategory(Category category);

    List<Topic> findByCategoryAndStatus(Category category, Status status);

    List<Topic> findByPriorityOrderByCreatedAtDesc(Priority priority);

    Page<Topic> findByStatusIn(List<Status> statuses, Pageable pageable);

    long countByStatus(Status status);

    boolean existsByName(String name);

    // ═══ 2. JPQL with @Query ═══════════════════════════════════════════
    // For complex queries with named parameters

    @Query("""
            SELECT t FROM Topic t
            WHERE (:category IS NULL OR t.category = :category)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:nameFilter IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :nameFilter, '%')))
            ORDER BY t.priority ASC, t.name ASC
            """)
    Page<Topic> searchTopics(
            @Param("category") Category category,
            @Param("priority") Priority priority,
            @Param("nameFilter") String nameFilter,
            Pageable pageable);

    /**
     * Bulk update — @Modifying required for UPDATE/DELETE JPQL.
     * @Transactional needed because this mutates state outside service layer.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Topic t SET t.status = :newStatus WHERE t.category = :category AND t.status = :oldStatus")
    int bulkUpdateStatus(
            @Param("category") Category category,
            @Param("oldStatus") Status oldStatus,
            @Param("newStatus") Status newStatus);

    // ═══ 3. EntityGraph — solves N+1 problem ═══════════════════════════
    // Without this, looping over topics + accessing resources = N+1 queries.
    // EntityGraph adds a JOIN FETCH transparently for this single call.

    @EntityGraph(attributePaths = {"resources"})
    @Query("SELECT t FROM Topic t WHERE t.id = :id")
    Optional<Topic> findByIdWithResources(@Param("id") Long id);

    @EntityGraph(attributePaths = {"resources"})
    List<Topic> findAllByCategory(Category category);

    // ═══ 4. Native SQL — escape hatch for DB-specific features ════════
    // Use when JPQL can't express the query (e.g., window functions)

    @Query(value = """
            SELECT category, COUNT(*) AS topic_count
            FROM topics
            GROUP BY category
            ORDER BY topic_count DESC
            """, nativeQuery = true)
    List<Object[]> countByCategoryNative();

    // ═══ 5. Projections — return only the fields you need ═════════════
    // Avoids loading the whole entity. Good for list views.

    interface TopicSummary {
        Long getId();
        String getName();
        Category getCategory();
        Status getStatus();
    }

    List<TopicSummary> findByStatusOrderByName(Status status);
}
