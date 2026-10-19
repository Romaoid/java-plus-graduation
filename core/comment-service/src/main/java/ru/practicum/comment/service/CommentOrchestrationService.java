package ru.practicum.comment.service;

import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import jdk.jfr.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentDtoRequest;
import ru.practicum.comment.dto.EventDto;
import ru.practicum.comment.dto.UserShortDto;
import ru.practicum.comment.exception.ConflictException;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.exception.ProcessingException;
import ru.practicum.comment.feign.EventClient;
import ru.practicum.comment.feign.UserClient;
import ru.practicum.comment.logger.Loggable;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentOrchestrationService {

    private final CommentService commentService;
    private final UserClient userClient;
    private final CacheManager cacheManager;
    private final EventClient eventClient;

    public CommentDto addComment(Long userId, Long eventId, CommentDtoRequest requestDto) {
        log.info("Пользователь с id={} создает комментарий к событию с id={}", userId, eventId);
        UserShortDto user = getUser(userId);
        EventDto event = getEventById(eventId);

        Comment parentComment = null;

        if (requestDto.getAnswerTo() != null) {
            parentComment = commentService.getById(requestDto.getAnswerTo());

            if (!parentComment.getEventId().equals(eventId)) {
                throw new NotFoundException("Комментарий с id=" + requestDto.getAnswerTo() + " для ответа не найден");
            }
        }

        Comment comment = Comment.builder()
                .text(requestDto.getText())
                .authorId(user.getId())
                .eventId(event.getId())
                .parentComment(parentComment)
                .creationDate(LocalDateTime.now())
                .build();

        return CommentMapper.toCommentDto(commentService.saveComment(comment), event, user);
    }


    public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentDtoRequest updateDto) {
        log.info("Пользователь с id={} обновляет комментарий с id={} для события с id={}", userId, commentId, eventId);

        Comment comment = commentService.getById(commentId);

        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId +
                    " не был найден в событии id = " + comment.getEventId());
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор может обновить комментарий.");
        }

        UserShortDto user = getUser(userId);
        EventDto event = getEventById(eventId);

        comment.setText(updateDto.getText());
        return CommentMapper.toCommentDto(commentService.saveComment(comment), event, user);
    }

    public void deleteComment(Long userId, Long eventId, Long commentId) {
        log.info("Пользователь с id={} удаляет комментарий с id={} для события с id={}", userId, commentId, eventId);

        Comment comment = commentService.getById(commentId);

        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId +
                    " не был найден в событии id = " + comment.getEventId());
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор комментария может его удалить.");
        }

        commentService.deleteComment(commentId);
    }

    public void deleteCommentAdmin(Long commentId) {
        log.info("Администратор удаляет комментарий с id={}", commentId);

        commentService.deleteComment(commentId);
    }


    public List<CommentDto> getCommentsAdmin(String text, Long eventId, Long authorId, int from, int size) {
        log.info("Получение комментариев администратором по фильтрам");

        List<Comment> comments = commentService.getCommentsAdmin(text, eventId, authorId, from, size);

        return getDtos(comments);
    }


    public List<CommentDto> getCommentsPublic(Long eventId, int from, int size) {
        log.info("Публичное получение комментариев для события с id={}", eventId);

        List<Comment> comments = commentService.getCommentsPublic(eventId, from, size);

        if (comments.isEmpty()) {
            EventDto event = getEventById(eventId);
            return Collections.emptyList();
        }

        return getDtos(comments);
    }

    @Loggable
    @Cacheable(value = "users", key = "#userId")
    private UserShortDto getUser(Long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("User id:%d not found".formatted(userId));
        } catch (FeignException.ServiceUnavailable exception) {
            throw new ProcessingException("Сервис users временно недоступен");
        }
    }

    @Loggable
    private List<UserShortDto> getUsers(Set<Long> ids) {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache("users");

        if (springCache == null) {
            return getUsersFromFeign(ids);
        }

        Cache<Object, Object> nativeCache = springCache.getNativeCache();
        Map<Object, Object> cachedMap = nativeCache.getAllPresent(ids);

        Set<Long> missingIds = ids.stream()
                .filter(id -> !cachedMap.containsKey(id))
                .collect(Collectors.toSet());

        List<UserShortDto> result = new ArrayList<>();
        cachedMap.values().forEach(v -> result.add((UserShortDto) v));

        if (!missingIds.isEmpty()) {
            List<UserShortDto> fetched = getUsersFromFeign(missingIds);
            result.addAll(fetched);
            Map<Object, Object> toPut = fetched.stream()
                    .collect(Collectors.toMap(
                            UserShortDto::getId,
                            UserShortDto -> UserShortDto));
            nativeCache.putAll(toPut);
        }

        return result;
    }

    private List<UserShortDto> getUsersFromFeign(Set<Long> ids) {
        try {
            return userClient.getUsersByIds(ids);
        } catch (FeignException.ServiceUnavailable exception) {
            throw new ProcessingException("Сервис users временно недоступен");
        }
    }

    @Cacheable(value = "events", key = "#id")
    private EventDto getEventById(long id) {
        try {
            return eventClient.getEventById(id);
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            throw new NotFoundException("Field: eventId. Error: event не найден. Value: " + id);
        } catch (FeignException.ServiceUnavailable e) {
            throw new ProcessingException("Event service is unavailable now, please try later.");
        }
    }

    @Loggable
    private List<EventDto> getEvents(Set<Long> ids) {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache("events");

        if (springCache == null) {
            return getEventsFromFeign(ids);
        }

        Cache<Object, Object> nativeCache = springCache.getNativeCache();
        Map<Object, Object> cachedMap = nativeCache.getAllPresent(ids);

        Set<Long> missingIds = ids.stream()
                .filter(id -> !cachedMap.containsKey(id))
                .collect(Collectors.toSet());

        List<EventDto> result = new ArrayList<>();
        cachedMap.values().forEach(v -> result.add((EventDto) v));

        if (!missingIds.isEmpty()) {
            List<EventDto> fetched = getEventsFromFeign(missingIds);
            result.addAll(fetched);
            Map<Object, Object> toPut = fetched.stream()
                    .collect(Collectors.toMap(
                            EventDto::getId,
                            EventDto -> EventDto));
            nativeCache.putAll(toPut);
        }

        return result;
    }

    private List<EventDto> getEventsFromFeign(Set<Long> ids) {
        try {
            return eventClient.getEventsByIds(ids);
        } catch (FeignException.ServiceUnavailable exception) {
            throw new ProcessingException("Сервис events временно недоступен");
        }
    }

    private List<CommentDto> getDtos(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        if (comments.size() == 1) {
            Comment comment = comments.getFirst();
            UserShortDto user = getUser(comment.getAuthorId());
            EventDto event = getEventById(comment.getEventId());

            return List.of(
                    CommentMapper.toCommentDto(comment, event, user)
            );
        }

        Map<Long,UserShortDto> usersMap =  getUsersMapByComments(comments);
        Map<Long,EventDto> eventsMap = getEventsMapByComments(comments);


        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(
                        comment,
                        eventsMap.get(comment.getEventId()),
                        usersMap.get(comment.getAuthorId())
                ))
                .toList();
    }

    private Map<Long,UserShortDto> getUsersMapByComments(List<Comment> comments) {
        Set<Long> userIds = comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet());
        Map<Long,UserShortDto> usersMap;

        if (userIds.size() == 1) {
            UserShortDto user = getUser(userIds.stream().findFirst().get());
            usersMap = Map.of(user.getId(), user);
        } else {
            List<UserShortDto> users = getUsers(userIds);
            usersMap = users.stream().collect(Collectors.toMap(
                    UserShortDto::getId,
                    UserShortDto -> UserShortDto)
            );
        }

        return usersMap;
    }

    private Map<Long,EventDto> getEventsMapByComments(List<Comment> comments) {
        Set<Long> eventIds = comments.stream().map(Comment::getEventId).collect(Collectors.toSet());
        Map<Long,EventDto> eventsMap;


        if (eventIds.size() == 1) {
            EventDto event = getEventById(eventIds.stream().findAny().get());
            eventsMap = Map.of(event.getId(), event);
        } else {
            List<EventDto> events = getEvents(eventIds);
            eventsMap = events.stream().collect(Collectors.toMap(
                    EventDto::getId,
                    EventDto -> EventDto)
            );
        }

        return eventsMap;
    }
}