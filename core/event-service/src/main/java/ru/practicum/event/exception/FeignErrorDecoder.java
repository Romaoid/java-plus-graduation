package ru.practicum.event.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder  {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ProcessingException("Товар id:%d не найден");
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
