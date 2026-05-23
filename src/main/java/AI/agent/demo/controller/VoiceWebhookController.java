package AI.agent.demo.controller;

import AI.agent.demo.service.VoiceWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VoiceWebhookController {
	private final VoiceWebhookService voiceWebhookService;

	@PostMapping(value = "/voice/incoming", produces = MediaType.APPLICATION_XML_VALUE)
	public String incomingCall(
			@RequestParam(required = false, name = "CallSid") String callSid,
			@RequestParam(required = false, name = "From") String callerPhoneNumber) {
		log.info("Received Twilio incoming call webhook. callSid={}, from={}", callSid, callerPhoneNumber);
		return voiceWebhookService.incomingCallInstructions(callSid, callerPhoneNumber);
	}

	@PostMapping(value = "/voice/respond", produces = MediaType.APPLICATION_XML_VALUE)
	public String respond(
			@RequestParam(required = false, name = "CallSid") String callSid,
			@RequestParam(required = false, name = "From") String callerPhoneNumber,
			@RequestParam(required = false, name = "SpeechResult") String speechResult) {
		log.info("Received Twilio speech webhook. callSid={}, from={}, speechPresent={}",
				callSid,
				callerPhoneNumber,
				speechResult != null && !speechResult.isBlank());
		return voiceWebhookService.respondToCaller(callSid, callerPhoneNumber, speechResult);
	}
}
