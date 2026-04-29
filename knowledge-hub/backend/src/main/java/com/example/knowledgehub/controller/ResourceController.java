package com.example.knowledgehub.controller;

import com.example.knowledgehub.domain.jpa.Resource;
import com.example.knowledgehub.service.ResourceService;
import com.example.knowledgehub.service.ResourceService.EnrichedResource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Resource controller — demonstrates resilient external API integration.
 *
 * <p>The {@code /enrich} endpoint shows graceful degradation: if the
 * downstream article service is down, the response still arrives with
 * fallback metadata.</p>
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<Resource> findAll() {
        return resourceService.findAll();
    }

    @GetMapping("/{id}")
    public Resource findById(@PathVariable Long id) {
        return resourceService.findById(id);
    }

    @PostMapping
    public Resource create(@Valid @RequestBody Resource resource) {
        return resourceService.create(resource);
    }

    /** Resilient enrichment via external API — circuit breaker protected. */
    @GetMapping("/{id}/enrich")
    public CompletableFuture<EnrichedResource> enrich(@PathVariable Long id) {
        return resourceService.enrichWithExternalMetadata(id);
    }
}
