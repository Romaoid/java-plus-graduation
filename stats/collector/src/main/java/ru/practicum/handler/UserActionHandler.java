package ru.practicum.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.sender.ActionSender;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class UserActionHandler {
    private final ActionSender actionSender;

    public void handle(UserActionProto action) {
        log.debug("Отправляю: {}",action);

        actionSender.sendAction(build(action));
    }

    private UserActionAvro build(UserActionProto action) {
        ActionTypeAvro typeAvro = switch (action.getActionType().getNumber()) {
            case 0 -> ActionTypeAvro.VIEW;
            case 1 -> ActionTypeAvro.REGISTER;
            case 2 -> ActionTypeAvro.LIKE;
            default -> throw new IllegalStateException("Unexpected value: " + action.getActionType().getNumber());
        };

        Instant instant = Instant.ofEpochSecond(action.getTimestamp().getSeconds(), action.getTimestamp().getNanos());

        return UserActionAvro.newBuilder()
                .setUserId(action.getUserId())
                .setEventId(action.getEventId())
                .setActionType(typeAvro)
                .setTimestamp(instant)
                .build();
    }

}
