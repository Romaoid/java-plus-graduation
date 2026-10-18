package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dao.CompilationRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.exception.NotFoundException;
import ru.practicum.event.logger.Loggable;
import ru.practicum.event.mapper.CompilationMapper;
import ru.practicum.event.model.Compilation;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.QCompilation;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventService eventService;
    private final UserService userService;
    private final StatClient statClient;

    @Loggable
    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto newCompilationDto) {
        Set<Event> event = new HashSet<>();

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            event = new HashSet<>(eventService.findAllByIdIn(newCompilationDto.getEvents()));
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .events(event)
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);

        return getDto(savedCompilation);
    }

    @Loggable
    @Override
    @Transactional
    public void delete(Long compId) {

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
    }

    @Loggable
    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка id=" + compId + " не найдена"));

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventService.findAllByIdIn(updateCompilationRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return getDto(updatedCompilation);
    }

    @Loggable
    @Override
    public List<CompilationDto> getCompilations(CompilationsGetParams params) {
        int from = params.getFrom();
        int size = params.getSize();

        Pageable pageable = PageRequest.of(0, from + size);
        BooleanExpression predicate = buildPredicate(params);
        Page<Compilation> page = compilationRepository.findAll(predicate, pageable);

        List<Compilation> compilations = page.getContent().stream()
                .skip(from)
                .limit(size)
                .toList();

        Set<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .collect(Collectors.toSet());

        List<String> uris = allEventIds.stream()
                .map(e -> "/events/" + e)
                .toList();
        Map<Long, Long> viewsByEventId = getStatsByUris(uris);

        Map<Long, UserShortDto> initiatorsMap = getUsersByEventSet(compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .collect(Collectors.toSet())
        );

        return compilations.stream()
                .map(comp -> CompilationMapper.toCompilationDto(comp, initiatorsMap, viewsByEventId))
                .collect(Collectors.toList());
    }

    @Loggable
    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return getDto(compilation);
    }

    private BooleanExpression buildPredicate(CompilationsGetParams params) {
        QCompilation compilation = QCompilation.compilation;
        BooleanExpression predicate = null;

        if (params.getPinned() != null) {
            predicate = compilation.pinned.eq(params.getPinned());
        }

        return predicate == null ? compilation.isNotNull() : predicate;
    }

    private Map<Long, Long> getStatsByUris(List<String> uris) {

        List<ViewStatsDto> stats = statClient.getStat(LocalDateTime.MIN, LocalDateTime.MAX, uris, false);

        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> viewsMap = new LinkedHashMap<>();
        for (ViewStatsDto dto : stats) {
            String uri = dto.getUri();
            Long id = Long.parseLong(
                    uri.substring(
                            uri.lastIndexOf('/') + 1));
            viewsMap.put(id, dto.getHits());
        }

        return viewsMap;
    }

    private CompilationDto getDto(Compilation compilation) {
        Set<Long> allEventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        List<String> uris = allEventIds.stream()
                .map(e -> "/events/" + e)
                .toList();
        Map<Long, Long> viewsByEventId = getStatsByUris(uris);

        Map<Long, UserShortDto> initiatorsMap = getUsersByEventSet(new HashSet<>(compilation.getEvents()));

        return CompilationMapper.toCompilationDto(compilation, initiatorsMap, viewsByEventId);
    }

    private Map<Long, UserShortDto> getUsersByEventSet(Set<Event> events) {
        Set<Long> ids = events.stream()
                .map(Event::getInitiator)
                .collect(Collectors.toSet());

        return userService.getUsers(ids)
                .stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        userShortDto -> userShortDto
                ));
    }
}