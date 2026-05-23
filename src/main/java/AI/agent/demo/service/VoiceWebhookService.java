package AI.agent.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VoiceWebhookService {
	public String incomingCallInstructions() {
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"Thanks for calling Sears Home Services. Tell me what appliance needs service, what problem you are seeing, and your ZIP code."));
	}

	public String respondToCaller(String speechResult) {
		if (!StringUtils.hasText(speechResult)) {
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"I did not catch that. Please say the appliance type, the issue, and your ZIP code."));
		}
		return response(
				say("I heard: " + speechResult.trim() + ".")
						+ gather(
								"/voice/respond",
								"speech",
								"auto",
								"Please confirm any urgent symptoms, such as leaking water, burning smell, or no power. You can also say no urgent symptoms."));
	}

	private String response(String body) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<Response>
				%s
				</Response>
				""".formatted(body);
	}

	private String gather(String action, String input, String speechTimeout, String prompt) {
		return """
				<Gather action="%s" method="POST" input="%s" speechTimeout="%s">
				%s
				</Gather>
				""".formatted(action, input, speechTimeout, say(prompt));
	}

	private String say(String text) {
		return "<Say>" + escapeXml(text) + "</Say>";
	}

	private String escapeXml(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}
