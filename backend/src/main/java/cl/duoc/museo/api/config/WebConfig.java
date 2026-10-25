package cl.duoc.museo.api.config;

import cl.duoc.museo.api.dto.Dtos.ApiError;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }

    /** Errores en formato JSON simple. */
    @RestControllerAdvice
    public static class ErrorHandler {
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiError> status(ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiError(e.getStatusCode().value(), e.getReason()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
            String msg = e.getBindingResult().getFieldErrors().stream()
                    .map(f -> f.getField() + ": " + f.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(400, msg));
        }
    }
}
