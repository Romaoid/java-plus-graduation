package ru.practicum.event.mapper;

import ru.practicum.event.dto.CompilationDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.UserShortDto;
import ru.practicum.event.model.Compilation;
import ru.practicum.event.model.Event;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

public class CompilationMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CompilationDto toCompilationDto(Compilation compilation,
                                                  Map<Long, UserShortDto> initiators,
                                                  Map<Long, Double> ratings) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(event -> toEventShortDto(event, initiators, ratings))
                        .collect(Collectors.toList()))
                .build();
    }

    private static EventShortDto toEventShortDto(Event event,
                                                 Map<Long, UserShortDto> initiators,
                                                 Map<Long, Double> ratings) {
        Long eventId = event.getId();

        return EventShortDto.builder()
                .id(eventId)
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate().format(FORMATTER))
                .paid(event.getPaid())
                .category(CategoryMapper.toDto(event.getCategory()))
                .initiator(initiators.get(event.getInitiator()))
                .rating(ratings.getOrDefault(eventId, 0.0))
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }
}