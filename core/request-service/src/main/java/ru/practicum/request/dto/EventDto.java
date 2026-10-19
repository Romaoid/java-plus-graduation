package ru.practicum.request.dto;

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
    private Boolean requestModeration;
    private Integer participantLimit;
    private Integer confirmedRequests;
}
