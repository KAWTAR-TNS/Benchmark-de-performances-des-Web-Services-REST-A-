package com.benchmark.springmvc.controller;

import com.benchmark.springmvc.entity.Category;
import com.benchmark.springmvc.entity.Item;
import com.benchmark.springmvc.repository.CategoryRepository;
import com.benchmark.springmvc.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Value("${app.query.mode:optimized}")
    private String queryMode;

    @GetMapping
    public ResponseEntity<Page<Item>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Item> items;
        
        if (categoryId != null) {
            if ("optimized".equals(queryMode)) {
                items = itemRepository.findByCategoryIdOptimized(categoryId, pageable);
            } else {
                items = itemRepository.findByCategoryId(categoryId, pageable);
            }
        } else {
            if ("optimized".equals(queryMode)) {
                items = itemRepository.findAllOptimized(pageable);
            } else {
                items = itemRepository.findAll(pageable);
            }
        }
        
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item) {
        // Ensure category exists
        if (item.getCategory() != null && item.getCategory().getId() != null) {
            Category category = categoryRepository.findById(item.getCategory().getId())
                    .orElse(null);
            if (category == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Category not found"));
            }
            item.setCategory(category);
        }
        
        Item saved = itemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody Item item) {
        
        if (!itemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Ensure category exists
        if (item.getCategory() != null && item.getCategory().getId() != null) {
            Category category = categoryRepository.findById(item.getCategory().getId())
                    .orElse(null);
            if (category == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Category not found"));
            }
            item.setCategory(category);
        }
        
        item.setId(id);
        Item updated = itemRepository.save(item);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
