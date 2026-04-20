package com.fintrack.service;

import com.fintrack.entity.Category;
import com.fintrack.entity.User;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Category> getCategories(String username) {
        return categoryRepository.findByUser_Username(username);
    }

    @Transactional(readOnly = true)
    public Category getById(Long id, String username) {
        return categoryRepository.findByIdAndUser_Username(id, username)
            .orElseThrow(() -> new
                    NoSuchElementException("Category not found with id: %d"
                    .formatted(id)));
    }

    public Category create(String name, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException(
                    "User not found with username: %s"
            ));

        Category category = new Category();
        category.setUser(user);
        category.setName(name);

        return categoryRepository.save(category);
    }


    public Category update(Long id, String name, String username) {
        Category category = categoryRepository.findByIdAndUser_Username(id, username)
                .orElseThrow(() -> new NoSuchElementException
                        ("Category not found with id: %d for username: %s".formatted(id, username)));
        category.setName(name);

        return categoryRepository.save(category);
    }

    @Transactional
    public void remove(Long id, String username) {
        Category category = getById(id, username);
        categoryRepository.delete(category);
    }
}
