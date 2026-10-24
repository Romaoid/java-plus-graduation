package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dao.CommentRepository;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.model.Comment;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public Comment saveComment(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format("Комментарий %d не найден", commentId));
        }

        clearAnswerLinks(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<Comment> getCommentsAdmin(String text, Long eventId, Long authorId, int from, int size) {
        log.info("Получение комментариев администратором по фильтрам");

        return commentRepository.findCommentsAdmin(text, eventId, authorId, from, size);
    }

    @Override
    public List<Comment> getCommentsPublic(Long eventId, int from, int size) {
        log.info("Публичное получение комментариев для события с id={}", eventId);

        return commentRepository.findAllByEventId(eventId, from, size);
    }

    @Override
    public Comment getById(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
    }

    private void clearAnswerLinks(long commentId) {
        List<Comment> answers = commentRepository.findAnswersByCommentId(commentId);

        if (!answers.isEmpty()) {
            for (Comment comment : answers) {
                comment.setParentComment(null);
            }

            commentRepository.saveAll(answers);
        }
    }
}