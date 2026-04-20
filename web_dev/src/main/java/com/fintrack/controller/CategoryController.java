package com.fintrack.controller;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.Category;
import com.fintrack.mapper.CategoryMapper;
import com.fintrack.security.AuthenticatedUser;
import com.fintrack.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;
    private final CategoryMapper mapper;
    private final AuthenticatedUser authenticatedUser;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        String username = authenticatedUser.getUsername();
        List<CategoryResponse> categories = service.getCategories(username)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        String username = authenticatedUser.getUsername();
        Category category = service.getById(id, username);
        return ResponseEntity.ok(mapper.toResponse(category));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        String username = authenticatedUser.getUsername();
        Category category = service.create(request.getName(), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                               @Valid @RequestBody CategoryRequest request) {
        String username = authenticatedUser.getUsername();
        Category category = service.update(id, request.getName(), username);
        return ResponseEntity.ok(mapper.toResponse(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CategoryResponse> deleteCategory(@PathVariable Long id) {
        String username = authenticatedUser.getUsername();
        service.remove(id, username);
        return ResponseEntity.status(204).build();
    }
}
