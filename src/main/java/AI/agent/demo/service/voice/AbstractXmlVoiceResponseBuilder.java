package AI.agent.demo.service.voice;

abstract class AbstractXmlVoiceResponseBuilder implements VoiceResponseBuilder {
	protected String response(String body) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<Response>
				%s
				</Response>
				""".formatted(body);
	}

	protected String escapeXml(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}
