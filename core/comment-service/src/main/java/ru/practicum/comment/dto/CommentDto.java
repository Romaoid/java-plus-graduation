package ru.practicum.comment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private Long answerTo;
    private String name;
    private String text;
    private LocalDateTime createdOn;
}