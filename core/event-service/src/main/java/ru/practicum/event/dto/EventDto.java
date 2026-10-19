package ru.practicum.event.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventDto {
    private Long id;
    private String title;
    private Long initiator;
    private Boolean requestModeration;
    private Integer participantLimit;
    private Integer confirmedRequests;
}
