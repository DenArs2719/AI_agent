package AI.agent.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestErrorHandlerTest {

	@Test
	void handleBadRequestReturnsStandardErrorBody() {
		RestErrorHandler handler = new RestErrorHandler();

		ResponseEntity<RestErrorHandler.ApiError> response = handler.handleBadRequest(
				new IllegalArgumentException("Invalid request"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(400);
		assertThat(response.getBody().message()).isEqualTo("Invalid request");
		assertThat(response.getBody().timestamp()).isNotNull();
	}
}
