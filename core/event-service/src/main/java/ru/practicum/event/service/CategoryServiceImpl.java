package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dao.CategoryRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;
import ru.practicum.event.exception.ConflictException;
import ru.practicum.event.exception.NotFoundException;
import ru.practicum.event.logger.Loggable;
import ru.practicum.event.mapper.CategoryMapper;
import ru.practicum.event.model.Category;

import java.util.List;

@Service
@Loggable
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        Pageable pageable = PageRequest.of(0, from + size);

        Page<Category> page = categoryRepository.findAll(pageable);

        return page.getContent().stream()
                .skip(from)
                .limit(size)
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Категория с именем " + request.getName() + " уже существует");
        }

        Category category = CategoryMapper.toCategory(request);
        Category savedCategory = categoryRepository.save(category);

        return CategoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryByIdOrElseThrow(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Нельзя удалить категорию, с которой связаны события");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto request) {
        Category category = getCategoryByIdOrElseThrow(catId);

        if (categoryRepository.existsByNameAndIdNot(request.getName(), catId)) {
            throw new ConflictException("Категория с именем " + request.getName() + " уже существует");
        }

        category.setName(request.getName());

        return CategoryMapper.toDto(category);
    }

    @Override
    public Category getCategoryByIdOrElseThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));
    }
}