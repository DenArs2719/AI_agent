package AI.agent.demo.service.voice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.voice", name = "provider", havingValue = "telnyx")
public class TelnyxVoiceResponseBuilder extends AbstractXmlVoiceResponseBuilder {
	@Override
	public String gather(String action, String prompt) {
		return response("""
				<Gather action="%s" input="speech" speechTimeout="auto" transcriptionEngine="Telnyx" language="en-US">
				%s
				</Gather>
				""".formatted(action, sayVerb(prompt)));
	}

	@Override
	public String say(String prompt) {
		return response(sayVerb(prompt));
	}

	private String sayVerb(String prompt) {
		return "<Say>" + escapeXml(prompt) + "</Say>";
	}
}
