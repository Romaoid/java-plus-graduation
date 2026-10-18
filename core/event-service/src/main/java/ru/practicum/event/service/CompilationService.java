package ru.practicum.event.service;


import ru.practicum.event.dto.CompilationDto;
import ru.practicum.event.dto.CompilationsGetParams;
import ru.practicum.event.dto.NewCompilationDto;
import ru.practicum.event.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto save(NewCompilationDto newCompilationDto);

    void delete(Long compId);

    CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest);

    List<CompilationDto> getCompilations(CompilationsGetParams params);

    CompilationDto getCompilationById(Long compId);
}