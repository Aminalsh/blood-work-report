package io.everyonecodes.spring_module.dto;

import java.time.Instant;

public record ApiErrorResponse(Instant timestamp,
                               int status,
                               String error,
                               String message,
                               String path
) {





}
