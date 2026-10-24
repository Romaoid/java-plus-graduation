package ru.practicum.request.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder  {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 500) {
            return new ProcessingException("Event service is unavailable now, please try later.");
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
