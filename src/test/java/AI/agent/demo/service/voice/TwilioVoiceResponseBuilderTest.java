package AI.agent.demo.service.voice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TwilioVoiceResponseBuilderTest {
	private final TwilioVoiceResponseBuilder twilioVoiceResponseBuilder = new TwilioVoiceResponseBuilder();

	@Test
	void gatherReturnsTwimlWithSpeechGather() {
		String twiml = twilioVoiceResponseBuilder.gather("/voice/respond", "What appliance needs service?");

		assertThat(twiml).contains("<Response>");
		assertThat(twiml).contains("<Gather action=\"/voice/respond\" method=\"POST\" input=\"speech\" speechTimeout=\"auto\">");
		assertThat(twiml).contains("<Say>What appliance needs service?</Say>");
	}

	@Test
	void sayEscapesText() {
		String twiml = twilioVoiceResponseBuilder.say("Tom & Jane's washer");

		assertThat(twiml).contains("<Say>Tom &amp; Jane&apos;s washer</Say>");
	}
}
