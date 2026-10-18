package ru.practicum.event.service;

import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;
import ru.practicum.event.model.Category;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);

    CategoryDto createCategory(NewCategoryDto request);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto request);

    Category getCategoryByIdOrElseThrow(Long id);
}