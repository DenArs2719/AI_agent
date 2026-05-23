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
	public String incomingCall(
			@RequestParam(required = false, name = "CallSid") String callSid,
			@RequestParam(required = false, name = "From") String callerPhoneNumber) {
		return voiceWebhookService.incomingCallInstructions(callSid, callerPhoneNumber);
	}

	@PostMapping(value = "/voice/respond", produces = MediaType.APPLICATION_XML_VALUE)
	public String respond(
			@RequestParam(required = false, name = "CallSid") String callSid,
			@RequestParam(required = false, name = "From") String callerPhoneNumber,
			@RequestParam(required = false, name = "SpeechResult") String speechResult) {
		return voiceWebhookService.respondToCaller(callSid, callerPhoneNumber, speechResult);
	}
}
