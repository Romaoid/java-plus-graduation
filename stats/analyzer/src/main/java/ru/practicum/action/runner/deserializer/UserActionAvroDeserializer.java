package ru.practicum.action.runner.deserializer;

import ru.practicum.deserializer.BaseAvroDeserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;


public class UserActionAvroDeserializer extends BaseAvroDeserializer<UserActionAvro> {

    public UserActionAvroDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}