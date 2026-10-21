package ru.practicum.request.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Configuration
public class FeignHeaderConfig {
    private final static String HEADER_NAME_X_SOURCE_SERVICE = "X-Source-Service";
    private final static String HEADER_NAME_X_REQUEST_ID = "X-Source-Service";
    private final static String HEADER_VALUE_X_SOURCE_SERVICE = "request-service";

    @Bean
    public RequestInterceptor sourceServiceHeaderInterceptor() {
        return template -> {
            String requestId = getCurrentRequestId();
            template.header(HEADER_NAME_X_SOURCE_SERVICE, HEADER_VALUE_X_SOURCE_SERVICE);
            template.header(HEADER_NAME_X_REQUEST_ID, requestId);
        };
    }

    private String getCurrentRequestId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return UUID.randomUUID().toString();
        }

        HttpServletRequest request = attributes.getRequest();
        String requestId = request.getHeader(HEADER_NAME_X_REQUEST_ID);

        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return requestId;
    }
}
