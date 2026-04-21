package com.example.inventory.repository;
import com.example.inventory.model.InventoryItem;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
public interface InventoryRepository extends ReactiveMongoRepository<InventoryItem, String> {}
