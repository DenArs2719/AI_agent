package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import org.junit.jupiter.api.Test;

class DemoAiDiagnosticServiceTest {

	private final DemoAiDiagnosticService aiDiagnosticService = new DemoAiDiagnosticService();

	@Test
	void nextTurnExtractsStructuredUpdatesFromNaturalSpeech() {
		CallSession session = new CallSession("CA123");
		session.setCurrentStage(ConversationStage.APPLIANCE_TYPE);

		AiDialogueResult result = aiDiagnosticService.nextTurn(session, "My refrigerator is leaking in 60601");

		assertThat(result.updates().applianceType()).isEqualTo(ApplianceSpecialty.REFRIGERATOR);
		assertThat(result.updates().symptoms()).isEqualTo("My refrigerator is leaking in 60601");
		assertThat(result.updates().zipCode()).isEqualTo("60601");
		assertThat(result.assistantMessage()).contains("error codes");
	}

	@Test
	void nextTurnDoesNotReturnAlreadyCapturedFieldsAgain() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.WASHER);
		session.setSymptoms("Washer makes noise");
		session.setZipCode("60601");
		session.setCurrentStage(ConversationStage.ERROR_CODES);

		AiDialogueResult result = aiDiagnosticService.nextTurn(session, "No error code");

		assertThat(result.updates().applianceType()).isNull();
		assertThat(result.updates().symptoms()).isNull();
		assertThat(result.updates().zipCode()).isNull();
		assertThat(result.updates().errorCodes()).isEqualTo("No error code");
		assertThat(result.assistantMessage()).contains("troubleshooting");
	}

	@Test
	void nextTurnProducesReadyMessageWhenAllFieldsWillBeCaptured() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.DRYER);
		session.setSymptoms("Dryer has no heat");
		session.setErrorCodes("No error code");
		session.setPriorTroubleshootingSteps("Cleaned the lint filter");
		session.setZipCode("60601");
		session.setCustomerName("Jane Smith");
		session.setCurrentStage(ConversationStage.AVAILABILITY);

		AiDialogueResult result = aiDiagnosticService.nextTurn(session, "Tomorrow morning");

		assertThat(result.updates().availability()).isEqualTo("Tomorrow morning");
		assertThat(result.assistantMessage()).contains("matching technicians");
	}
}
