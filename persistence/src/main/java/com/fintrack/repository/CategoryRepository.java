package com.fintrack.repository;

import com.fintrack.entity.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser_Username(String userUsername);

    Optional<Category> findByIdAndUser_Username(Long id, String userUsername);
}