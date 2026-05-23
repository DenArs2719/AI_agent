package AI.agent.demo.config;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestErrorHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiError(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), Instant.now()));
	}

	public record ApiError(int status, String message, Instant timestamp) {
	}
}
