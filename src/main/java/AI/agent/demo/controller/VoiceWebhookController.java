package AI.agent.demo.controller;

import AI.agent.demo.service.VoiceWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VoiceWebhookController {
	private final VoiceWebhookService voiceWebhookService;

	@PostMapping(value = "/voice/incoming", produces = MediaType.APPLICATION_XML_VALUE)
	public String incomingCall() {
		return voiceWebhookService.incomingCallInstructions();
	}

	@PostMapping(value = "/voice/respond", produces = MediaType.APPLICATION_XML_VALUE)
	public String respond(@RequestParam(required = false, name = "SpeechResult") String speechResult) {
		return voiceWebhookService.respondToCaller(speechResult);
	}
}
