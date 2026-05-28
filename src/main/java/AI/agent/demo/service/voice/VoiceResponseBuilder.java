package AI.agent.demo.service.voice;

public interface VoiceResponseBuilder {
	String gather(String action, String prompt);

	String say(String prompt);
}
