package AI.agent.demo.service.voice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelnyxVoiceResponseBuilderTest {
	private final TelnyxVoiceResponseBuilder telnyxVoiceResponseBuilder = new TelnyxVoiceResponseBuilder();

	@Test
	void gatherReturnsTelnyxTexmlWithSpeechGather() {
		String texml = telnyxVoiceResponseBuilder.gather("/voice/respond", "What appliance needs service?");

		assertThat(texml).contains("<Response>");
		assertThat(texml).contains("<Gather action=\"/voice/respond\" input=\"speech\" speechTimeout=\"auto\"");
		assertThat(texml).contains("transcriptionEngine=\"Telnyx\"");
		assertThat(texml).contains("<Say>What appliance needs service?</Say>");
	}

	@Test
	void sayEscapesText() {
		String texml = telnyxVoiceResponseBuilder.say("Tom & Jane's washer");

		assertThat(texml).contains("<Say>Tom &amp; Jane&apos;s washer</Say>");
	}
}
