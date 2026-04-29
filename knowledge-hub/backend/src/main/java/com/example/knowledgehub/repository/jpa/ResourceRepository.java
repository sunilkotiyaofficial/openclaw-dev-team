package com.example.knowledgehub.repository.jpa;

import com.example.knowledgehub.domain.jpa.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByTopicId(Long topicId);

    List<Resource> findByType(Resource.ResourceType type);

    long countByTopicId(Long topicId);
}
