package ru.practicum.comment.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class EventDto {
    private Long id;
    private Long initiator;
    private String title;
}
