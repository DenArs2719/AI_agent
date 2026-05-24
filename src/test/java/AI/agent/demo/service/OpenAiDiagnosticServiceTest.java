package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class OpenAiDiagnosticServiceTest {

	@Test
	void nextTurnCallsOpenAiAndMapsStructuredUpdates() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		OpenAiDiagnosticService openAiDiagnosticService =
				new OpenAiDiagnosticService(restClientBuilder, new ObjectMapper());
		ReflectionTestUtils.setField(openAiDiagnosticService, "apiKey", "test-api-key");
		ReflectionTestUtils.setField(openAiDiagnosticService, "model", "gpt-4o-mini");
		CallSession session = new CallSession("CA123");
		session.setCurrentStage(ConversationStage.APPLIANCE_TYPE);
		String responseBody = """
				{
				  "output_text": "{\\"assistantMessage\\":\\"Do you see any error codes?\\",\\"issueResolved\\":false,\\"readyForScheduling\\":false,\\"needsMoreTroubleshooting\\":true,\\"updates\\":{\\"applianceType\\":\\"REFRIGERATOR\\",\\"symptoms\\":\\"My refrigerator is leaking\\",\\"errorCodes\\":null,\\"priorTroubleshootingSteps\\":null,\\"zipCode\\":\\"60601\\",\\"customerName\\":null,\\"availability\\":null}}"
				}
				""";
		mockServer.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-api-key"))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		AiDialogueResult result = openAiDiagnosticService.nextTurn(session, "My refrigerator is leaking in 60601");

		assertThat(result.assistantMessage()).isEqualTo("Do you see any error codes?");
		assertThat(result.updates().applianceType()).isEqualTo(ApplianceSpecialty.REFRIGERATOR);
		assertThat(result.updates().symptoms()).isEqualTo("My refrigerator is leaking");
		assertThat(result.updates().zipCode()).isEqualTo("60601");
		assertThat(result.issueResolved()).isFalse();
		assertThat(result.readyForScheduling()).isFalse();
		assertThat(result.needsMoreTroubleshooting()).isTrue();
		mockServer.verify();
	}

	@Test
	void nextTurnFailsFastWhenApiKeyIsMissing() {
		OpenAiDiagnosticService openAiDiagnosticService =
				new OpenAiDiagnosticService(RestClient.builder(), new ObjectMapper());
		ReflectionTestUtils.setField(openAiDiagnosticService, "apiKey", "");
		CallSession session = new CallSession("CA123");

		assertThatThrownBy(() -> openAiDiagnosticService.nextTurn(session, "My washer is noisy"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("api-key");
	}

	@Test
	void nextTurnSurfacesOpenAiErrors() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		OpenAiDiagnosticService openAiDiagnosticService =
				new OpenAiDiagnosticService(restClientBuilder, new ObjectMapper());
		ReflectionTestUtils.setField(openAiDiagnosticService, "apiKey", "test-api-key");
		ReflectionTestUtils.setField(openAiDiagnosticService, "model", "gpt-4o-mini");
		CallSession session = new CallSession("CA123");
		session.setCurrentStage(ConversationStage.APPLIANCE_TYPE);
		mockServer.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withBadRequest().body("{\"error\":{\"message\":\"bad request\"}}"));

		assertThatThrownBy(() -> openAiDiagnosticService.nextTurn(session, "My refrigerator is leaking in 60601"))
				.isInstanceOf(HttpClientErrorException.BadRequest.class);
		mockServer.verify();
	}
}
