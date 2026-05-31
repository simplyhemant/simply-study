package simply.simply_study.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, String> errors,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Instant timestamp,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String path,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Integer status,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String error
) {
    private static String getCurrentRequestPath() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                return attributes.getRequest().getRequestURI();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), getCurrentRequestPath(), 200, null);
    }

    public static <T> ApiResponse<T> success(String message, T data, int status) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), getCurrentRequestPath(), status, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, null, Instant.now(), getCurrentRequestPath(), 500, "Internal Server Error");
    }

    public static ApiResponse<Void> error(String message, HttpStatus status) {
        return new ApiResponse<>(false, message, null, null, Instant.now(), getCurrentRequestPath(), status.value(), status.getReasonPhrase());
    }

    public static ApiResponse<Void> validationError(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, errors, Instant.now(), getCurrentRequestPath(), 400, "Bad Request");
    }

    public static ApiResponse<Void> validationError(String message, Map<String, String> errors, HttpStatus status) {
        return new ApiResponse<>(false, message, null, errors, Instant.now(), getCurrentRequestPath(), status.value(), status.getReasonPhrase());
    }
}
