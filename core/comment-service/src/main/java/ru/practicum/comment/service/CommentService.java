package ru.practicum.comment.service;

import ru.practicum.comment.model.Comment;

import java.util.List;

public interface CommentService {

    void deleteComment(Long commentId);

    List<Comment> getCommentsAdmin(String text, Long eventId, Long authorId, int from, int size);

    List<Comment> getCommentsPublic(Long eventId, int from, int size);

    Comment getById(long commentId);

    Comment saveComment(Comment comment);
}