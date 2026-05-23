package AI.agent.demo.service;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.dto.ai.CallSessionUpdates;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.CallSession;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class OpenAiDiagnosticService implements AiDiagnosticService {

	private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";

	private final RestClient.Builder restClientBuilder;
	private final ObjectMapper objectMapper;

	@Value("${app.ai.api-key:}")
	private String apiKey;

	@Value("${app.ai.model:gpt-4o-mini}")
	private String model;

	@Override
	public AiDialogueResult nextTurn(CallSession session, String callerSpeech) {
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalStateException("OpenAI provider is enabled, but app.ai.api-key is not configured.");
		}
		try {
			String responseBody = restClientBuilder
					.build()
					.post()
					.uri(OPENAI_RESPONSES_URL)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody(session, callerSpeech))
					.retrieve()
					.body(String.class);
			return toDialogueResult(responseBody);
		}
		catch (RestClientResponseException exception) {
			log.warn("OpenAI dialogue call failed with status {} and response body: {}",
					exception.getStatusCode(),
					exception.getResponseBodyAsString());
			throw exception;
		}
		catch (RuntimeException exception) {
			log.warn("OpenAI dialogue call failed.", exception);
			throw exception;
		}
	}

	private Map<String, Object> requestBody(CallSession session, String callerSpeech) {
		return Map.of(
				"model", model,
				"instructions", systemInstructions(),
				"input", userInputJson(session, callerSpeech),
				"text", Map.of("format", responseFormat()));
	}

	private String userInputJson(CallSession session, String callerSpeech) {
		try {
			return objectMapper.writeValueAsString(userInput(session, callerSpeech));
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize dialogue input for OpenAI.", exception);
		}
	}

	private String systemInstructions() {
		return """
				You are a Sears Home Services voice diagnostic assistant.
				Your job is to extract structured fields from each caller response and ask for the next missing field.
				Follow this exact field order:
				1. applianceType
				2. symptoms
				3. errorCodes
				4. priorTroubleshootingSteps
				5. zipCode
				6. customerName
				7. availability
				Never invent appliance type, ZIP code, customer name, availability, error codes, or troubleshooting steps.
				Only fill a field when the caller clearly provided that value.
				If the current field is priorTroubleshootingSteps and the caller says they checked power, door, lid, filter, breaker, water supply, vent, thermostat, restarted it, or tried nothing, capture that answer and move on.
				Do not ask extra troubleshooting questions after priorTroubleshootingSteps has a value.
				Do not ask again for any field already present in knownSessionState.
				Ask exactly one concise question for the earliest missing field after applying updates from the caller's latest speech.
				If all fields are present after applying updates, assistantMessage must say: Thank you. I have enough information to look for matching technicians and appointment times.
				Do not make scheduling decisions, choose technicians, create appointments, or promise appointment availability.
				Return only the structured JSON requested by the schema.
				""";
	}

	private Map<String, Object> userInput(CallSession session, String callerSpeech) {
		Map<String, Object> knownSessionState = new LinkedHashMap<>();
		knownSessionState.put("applianceType", stringOrNull(session.getApplianceType()));
		knownSessionState.put("symptoms", stringOrNull(session.getSymptoms()));
		knownSessionState.put("errorCodes", stringOrNull(session.getErrorCodes()));
		knownSessionState.put("priorTroubleshootingSteps", stringOrNull(session.getPriorTroubleshootingSteps()));
		knownSessionState.put("zipCode", stringOrNull(session.getZipCode()));
		knownSessionState.put("customerName", stringOrNull(session.getCustomerName()));
		knownSessionState.put("availability", stringOrNull(session.getAvailability()));

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("currentStage", stringOrNull(session.getCurrentStage()));
		input.put("knownSessionState", knownSessionState);
		input.put("callerSpeech", StringUtils.hasText(callerSpeech) ? callerSpeech.trim() : "");
		return input;
	}

	private Map<String, Object> responseFormat() {
		return Map.of(
				"type", "json_schema",
				"name", "shs_voice_dialogue_turn",
				"strict", true,
				"schema", Map.of(
						"type", "object",
						"additionalProperties", false,
						"required", List.of("assistantMessage", "updates"),
						"properties", Map.of(
								"assistantMessage", Map.of("type", "string"),
								"updates", updatesSchema())));
	}

	private Map<String, Object> updatesSchema() {
		return Map.of(
				"type", "object",
				"additionalProperties", false,
				"required", List.of(
						"applianceType",
						"symptoms",
						"errorCodes",
						"priorTroubleshootingSteps",
						"zipCode",
						"customerName",
						"availability"),
						"properties", Map.of(
						"applianceType", Map.of(
								"type", List.of("string", "null"),
								"enum", Arrays.asList("REFRIGERATOR", "DISHWASHER", "WASHER", "DRYER", "OVEN", "MICROWAVE", "HVAC", null)),
						"symptoms", nullableStringSchema(),
						"errorCodes", nullableStringSchema(),
						"priorTroubleshootingSteps", nullableStringSchema(),
						"zipCode", nullableStringSchema(),
						"customerName", nullableStringSchema(),
						"availability", nullableStringSchema()));
	}

	private Map<String, Object> nullableStringSchema() {
		return Map.of("type", List.of("string", "null"));
	}

	private AiDialogueResult toDialogueResult(String responseBody) {
		try {
			String outputText = outputText(responseBody);
			JsonNode dialogueNode = objectMapper.readTree(outputText);
			JsonNode updatesNode = dialogueNode.path("updates");
			return new AiDialogueResult(
					dialogueNode.path("assistantMessage").asText("What appliance needs service?"),
					new CallSessionUpdates(
							applianceType(updatesNode.path("applianceType")),
							textOrNull(updatesNode.path("symptoms")),
							textOrNull(updatesNode.path("errorCodes")),
							textOrNull(updatesNode.path("priorTroubleshootingSteps")),
							textOrNull(updatesNode.path("zipCode")),
							textOrNull(updatesNode.path("customerName")),
							textOrNull(updatesNode.path("availability"))));
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("OpenAI returned an invalid dialogue response.", exception);
		}
	}

	private String outputText(String responseBody) throws JacksonException {
		JsonNode responseNode = objectMapper.readTree(responseBody);
		JsonNode outputTextNode = responseNode.path("output_text");
		if (outputTextNode.isTextual()) {
			return outputTextNode.asText();
		}
		for (JsonNode outputNode : responseNode.path("output")) {
			for (JsonNode contentNode : outputNode.path("content")) {
				JsonNode textNode = contentNode.path("text");
				if (textNode.isTextual()) {
					return textNode.asText();
				}
			}
		}
		throw new IllegalStateException("OpenAI response did not contain output text.");
	}

	private ApplianceSpecialty applianceType(JsonNode node) {
		if (!node.isTextual()) {
			return null;
		}
		return ApplianceSpecialty.valueOf(node.asText());
	}

	private String textOrNull(JsonNode node) {
		if (!node.isTextual() || !StringUtils.hasText(node.asText())) {
			return null;
		}
		return node.asText().trim();
	}

	private Object stringOrNull(Object value) {
		return value == null ? null : value.toString();
	}
}
