package ru.practicum.deserializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.exception.DeserializationException;

public class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {
    private final DecoderFactory decoderFactory;
    private final DatumReader<UserActionAvro> reader;

    public UserActionAvroDeserializer() {
        this(UserActionAvro.getClassSchema());
    }

    public UserActionAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public UserActionAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        try {
            if (data != null) {
                BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
                return this.reader.read(null, decoder);
            }
            return null;
        } catch (Exception e) {
            throw new DeserializationException("Ошибка десереализации данных из топика [" + topic + "]", e);
        }
    }
}
