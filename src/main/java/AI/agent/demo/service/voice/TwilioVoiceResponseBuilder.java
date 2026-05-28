package AI.agent.demo.service.voice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.voice", name = "provider", havingValue = "twilio", matchIfMissing = true)
public class TwilioVoiceResponseBuilder extends AbstractXmlVoiceResponseBuilder {
	@Override
	public String gather(String action, String prompt) {
		return response("""
				<Gather action="%s" method="POST" input="speech" speechTimeout="auto">
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
