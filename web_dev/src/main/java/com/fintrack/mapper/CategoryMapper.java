package com.fintrack.mapper;

import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
