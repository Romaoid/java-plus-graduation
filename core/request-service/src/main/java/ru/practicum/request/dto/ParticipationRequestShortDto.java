package ru.practicum.request.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequestShortDto {
    private Long event;
    private Long requester;
    private String status;
}