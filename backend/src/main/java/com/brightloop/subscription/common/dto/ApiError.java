package com.brightloop.subscription.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error response body returned for EVERY error by GlobalExceptionHandler.
 *
 * @JsonInclude(NON_NULL) — omits "fieldErrors" from the JSON payload when
 * there are no field-level validation errors (keeps non-validation error
 * responses clean).
 *
 * Assumptions:
 *  - 'path' is populated from HttpServletRequest.getRequestURI().
 *  - 'fieldErrors' key = field name, value = validation message.
 *  - 'timestamp' is an ISO-8601 instant (UTC).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private Map<String, String> fieldErrors;

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path);
    }

    public static ApiError withFieldErrors(int status, String error, String message,
                                           String path, Map<String, String> fieldErrors) {
        ApiError apiError = new ApiError(status, error, message, path);
        apiError.fieldErrors = fieldErrors;
        return apiError;
    }

    // ---- Getters (required for Jackson serialisation) ----

    public Instant getTimestamp()               { return timestamp; }
    public int getStatus()                      { return status; }
    public String getError()                    { return error; }
    public String getMessage()                  { return message; }
    public String getPath()                     { return path; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
