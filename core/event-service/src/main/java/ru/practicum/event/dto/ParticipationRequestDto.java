package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipationRequestDto {
    private Long event;
    private Long requester;
    private RequestStatus status;

    public enum RequestStatus {
        NONREQUESTED,
        REJECTED,
        PENDING,
        CONFIRMED,
        CANCELED
    }
}