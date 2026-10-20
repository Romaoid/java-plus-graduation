package ru.practicum.comment.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentOrchestrationService {

    private final CommentService commentService;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Loggable
    public CommentDto addComment(Long userId, Long eventId, CommentDtoRequest requestDto) {
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

    @Loggable
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentDtoRequest updateDto) {
        Comment comment = commentService.getById(commentId);

        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId +
                    " не был найден в событии id = " + comment.getEventId());
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор может обновить комментарий.");
        }

        comment.setText(updateDto.getText());

        UserShortDto user;
        try {
            user = getUser(userId);
        } catch (FeignException | ProcessingException e) {
            user = userServiceDegradation(userId);
        }

        EventDto event;
        try {
            event = getEventById(eventId);
        } catch (ProcessingException e) {
            return CommentMapper.toDegradedDto(comment, user);
        }
        return CommentMapper.toCommentDto(commentService.saveComment(comment), event, user);
    }

    @Loggable
    public void deleteComment(Long userId, Long eventId, Long commentId) {
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

    @Loggable
    public void deleteCommentAdmin(Long commentId) {
        commentService.deleteComment(commentId);
    }

    @Loggable
    public List<CommentDto> getCommentsAdmin(String text, Long eventId, Long authorId, int from, int size) {
        List<Comment> comments = commentService.getCommentsAdmin(text, eventId, authorId, from, size);

        return getDtos(comments);
    }

    @Loggable
    public List<CommentDto> getCommentsPublic(Long eventId, int from, int size) {
        List<Comment> comments = commentService.getCommentsPublic(eventId, from, size);

        return getDtos(comments);
    }

    @Loggable
    public UserShortDto getUser(Long userId) {
            return userClient.getUserById(userId);
    }

    @Loggable
    public List<UserShortDto> getUsers(Set<Long> ids) {
        return userClient.getUsersByIds(ids);
    }

    public EventDto getEventById(long id) {
            return eventClient.getEventById(id);
    }

    @Loggable
    public List<EventDto> getEvents(Set<Long> ids) {
            return eventClient.getEventsByIds(ids);
    }

    private List<CommentDto> getDtos(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserShortDto> usersMap = getUsersMapByComments(comments);
        Map<Long, EventDto> eventsMap = getEventsMapByComments(comments);

        return comments.stream()
                .map(comment -> {
                    UserShortDto user = usersMap.get(comment.getAuthorId());
                    EventDto event = eventsMap.get(comment.getEventId());
                    return event == null
                            ? CommentMapper.toDegradedDto(comment, user)
                            : CommentMapper.toCommentDto(comment, event, user);
                })
                .toList();
    }

    private Map<Long,UserShortDto> getUsersMapByComments(List<Comment> comments) {
        Set<Long> userIds = comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet());

        try {
            if (userIds.size() == 1) {
                UserShortDto user = getUser(userIds.iterator().next());
                return Map.of(user.getId(), user);
            }
            return getUsers(userIds).stream()
                    .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
        } catch (ProcessingException | FeignException e) {
            return userIds.stream()
                    .collect(Collectors.toMap(Function.identity(), this::userServiceDegradation));
        }
    }

    private Map<Long,EventDto> getEventsMapByComments(List<Comment> comments) {
        Set<Long> eventIds = comments.stream().map(Comment::getEventId).collect(Collectors.toSet());

        try {
            if (eventIds.size() == 1) {
                EventDto event = getEventById(eventIds.iterator().next());
                return Map.of(event.getId(), event);
            }
            return getEvents(eventIds).stream()
                    .collect(Collectors.toMap(EventDto::getId, Function.identity()));
        } catch (ProcessingException | FeignException e) {
            return Collections.emptyMap();
        }
    }

    private UserShortDto userServiceDegradation(long userId) {
        return UserShortDto.builder()
                .id(userId)
                .name("User" + userId)
                .build();
    }
}