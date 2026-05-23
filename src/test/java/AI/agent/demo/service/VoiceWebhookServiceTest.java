package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VoiceWebhookServiceTest {

	private final VoiceWebhookService voiceWebhookService = new VoiceWebhookService();

	@Test
	void incomingCallInstructionsAskForDiagnosticDetailsAndCollectSpeech() {
		String twiml = voiceWebhookService.incomingCallInstructions();

		assertThat(twiml).contains("<Response>");
		assertThat(twiml).contains("<Gather action=\"/voice/respond\" method=\"POST\" input=\"speech\" speechTimeout=\"auto\">");
		assertThat(twiml).contains("what appliance needs service");
		assertThat(twiml).contains("your ZIP code");
	}

	@Test
	void respondToCallerRepromptsWhenSpeechIsMissing() {
		String twiml = voiceWebhookService.respondToCaller(" ");

		assertThat(twiml).contains("I did not catch that");
		assertThat(twiml).contains("<Gather action=\"/voice/respond\" method=\"POST\" input=\"speech\" speechTimeout=\"auto\">");
	}

	@Test
	void respondToCallerEchoesSpeechAndAsksUrgencyQuestion() {
		String twiml = voiceWebhookService.respondToCaller("My refrigerator is leaking in 60601");

		assertThat(twiml).contains("I heard: My refrigerator is leaking in 60601.");
		assertThat(twiml).contains("leaking water, burning smell, or no power");
	}

	@Test
	void respondToCallerEscapesSpeechBeforeReturningXml() {
		String twiml = voiceWebhookService.respondToCaller("washer & dryer <broken>");

		assertThat(twiml).contains("washer &amp; dryer &lt;broken&gt;");
		assertThat(twiml).doesNotContain("washer & dryer <broken>");
	}
}
