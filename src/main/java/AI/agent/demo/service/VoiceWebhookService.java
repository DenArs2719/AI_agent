package AI.agent.demo.service;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.dto.ai.CallSessionUpdates;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import AI.agent.demo.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VoiceWebhookService {
	private static final String LOCAL_DEMO_CALL_SID = "LOCAL_DEMO_CALL";

	private final CallSessionRepository callSessionRepository;
	private final AiDiagnosticService aiDiagnosticService;

	@Transactional
	public String incomingCallInstructions(String callSid) {
		CallSession session = getOrCreateSession(callSid);
		session.setCurrentStage(nextMissingStage(session));
		callSessionRepository.save(session);
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"Thanks for calling Sears Home Services. " + questionFor(session.getCurrentStage())));
	}

	@Transactional
	public String respondToCaller(String callSid, String speechResult) {
		CallSession session = getOrCreateSession(callSid);
		if (!StringUtils.hasText(speechResult)) {
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"I did not catch that. " + questionFor(session.getCurrentStage())));
		}
		AiDialogueResult aiDialogueResult = aiDiagnosticService.nextTurn(session, speechResult.trim());
		applyUpdates(session, aiDialogueResult.updates());
		session.setCurrentStage(nextMissingStage(session));
		callSessionRepository.save(session);
		if (session.getCurrentStage() == ConversationStage.READY_TO_SCHEDULE) {
			return response(say(aiDialogueResult.assistantMessage()));
		}
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"Thanks, I captured that. " + aiDialogueResult.assistantMessage()));
	}

	private CallSession getOrCreateSession(String callSid) {
		String normalizedCallSid = StringUtils.hasText(callSid) ? callSid.trim() : LOCAL_DEMO_CALL_SID;
		return callSessionRepository
				.findByCallSid(normalizedCallSid)
				.orElseGet(() -> new CallSession(normalizedCallSid));
	}

	private void applyUpdates(CallSession session, CallSessionUpdates updates) {
		if (session.getApplianceType() == null && updates.applianceType() != null) {
			session.setApplianceType(updates.applianceType());
		}
		if (!StringUtils.hasText(session.getSymptoms()) && StringUtils.hasText(updates.symptoms())) {
			session.setSymptoms(updates.symptoms());
		}
		if (!StringUtils.hasText(session.getErrorCodes()) && StringUtils.hasText(updates.errorCodes())) {
			session.setErrorCodes(updates.errorCodes());
		}
		if (!StringUtils.hasText(session.getPriorTroubleshootingSteps())
				&& StringUtils.hasText(updates.priorTroubleshootingSteps())) {
			session.setPriorTroubleshootingSteps(updates.priorTroubleshootingSteps());
		}
		if (!StringUtils.hasText(session.getZipCode()) && StringUtils.hasText(updates.zipCode())) {
			session.setZipCode(updates.zipCode());
		}
		if (!StringUtils.hasText(session.getCustomerName()) && StringUtils.hasText(updates.customerName())) {
			session.setCustomerName(updates.customerName());
		}
		if (!StringUtils.hasText(session.getAvailability()) && StringUtils.hasText(updates.availability())) {
			session.setAvailability(updates.availability());
		}
	}

	private ConversationStage nextMissingStage(CallSession session) {
		if (session.getApplianceType() == null) {
			return ConversationStage.APPLIANCE_TYPE;
		}
		if (!StringUtils.hasText(session.getSymptoms())) {
			return ConversationStage.SYMPTOMS;
		}
		if (!StringUtils.hasText(session.getErrorCodes())) {
			return ConversationStage.ERROR_CODES;
		}
		if (!StringUtils.hasText(session.getPriorTroubleshootingSteps())) {
			return ConversationStage.TROUBLESHOOTING_STEPS;
		}
		if (!StringUtils.hasText(session.getZipCode())) {
			return ConversationStage.ZIP_CODE;
		}
		if (!StringUtils.hasText(session.getCustomerName())) {
			return ConversationStage.CUSTOMER_NAME;
		}
		if (!StringUtils.hasText(session.getAvailability())) {
			return ConversationStage.AVAILABILITY;
		}
		return ConversationStage.READY_TO_SCHEDULE;
	}

	private String questionFor(ConversationStage stage) {
		return switch (stage) {
			case APPLIANCE_TYPE -> "What appliance needs service?";
			case SYMPTOMS -> "What symptoms are you seeing?";
			case ERROR_CODES -> "Do you see any error codes on the appliance? If not, say no error code.";
			case TROUBLESHOOTING_STEPS -> "What troubleshooting steps have you already tried?";
			case ZIP_CODE -> "What ZIP code is the appliance located in?";
			case CUSTOMER_NAME -> "What is your name?";
			case AVAILABILITY -> "What day and time works best for the appointment?";
			case READY_TO_SCHEDULE -> "I have enough information to look for appointment times.";
		};
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
