package ru.practicum.comment.mapper;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.EventDto;
import ru.practicum.comment.dto.UserShortDto;
import ru.practicum.comment.model.Comment;

public class CommentMapper {
    public static CommentDto toCommentDto(Comment comment, EventDto event, UserShortDto user) {
        Long parentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;

        String displayName = user.getName();

        if (comment.getAuthorId().equals(event.getInitiator())) {
            displayName = event.getTitle();
        }

        return CommentDto.builder()
                .id(comment.getId())
                .answerTo(parentId)
                .name(displayName)
                .text(comment.getText())
                .createdOn(comment.getCreationDate())
                .build();
    }

    public static CommentDto toDegradedDto(Comment comment, UserShortDto user) {
        Long parentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;

        return CommentDto.builder()
                .id(comment.getId())
                .answerTo(parentId)
                .name(user.getName())
                .text(comment.getText())
                .createdOn(comment.getCreationDate())
                .build();
    }
}