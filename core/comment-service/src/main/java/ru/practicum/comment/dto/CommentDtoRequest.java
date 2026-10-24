package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDtoRequest {
    private Long answerTo;

    @NotBlank
    @Size(min = 1, max = 500)
    private String text;
}