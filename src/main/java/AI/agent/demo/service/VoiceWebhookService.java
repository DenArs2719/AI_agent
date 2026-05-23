package AI.agent.demo.service;

import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import AI.agent.demo.repository.CallSessionRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VoiceWebhookService {

	private static final String LOCAL_DEMO_CALL_SID = "LOCAL_DEMO_CALL";
	private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("\\b\\d{5}\\b");

	private final CallSessionRepository callSessionRepository;

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
		captureSpeech(session, speechResult.trim());
		session.setCurrentStage(nextMissingStage(session));
		callSessionRepository.save(session);
		if (session.getCurrentStage() == ConversationStage.READY_TO_SCHEDULE) {
			return response(say("Thank you. I have the appliance, symptoms, ZIP code, your name, and availability. "
					+ "Next I will look for matching technicians and appointment times."));
		}
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"Thanks, I captured that. " + questionFor(session.getCurrentStage())));
	}

	private CallSession getOrCreateSession(String callSid) {
		String normalizedCallSid = StringUtils.hasText(callSid) ? callSid.trim() : LOCAL_DEMO_CALL_SID;
		return callSessionRepository
				.findByCallSid(normalizedCallSid)
				.orElseGet(() -> new CallSession(normalizedCallSid));
	}

	private void captureSpeech(CallSession session, String speech) {
		switch (session.getCurrentStage()) {
			case APPLIANCE_TYPE -> parseApplianceType(speech).ifPresent(session::setApplianceType);
			case SYMPTOMS -> session.setSymptoms(speech);
			case ERROR_CODES -> session.setErrorCodes(speech);
			case TROUBLESHOOTING_STEPS -> session.setPriorTroubleshootingSteps(speech);
			case ZIP_CODE -> parseZipCode(speech).ifPresent(session::setZipCode);
			case CUSTOMER_NAME -> session.setCustomerName(speech);
			case AVAILABILITY -> session.setAvailability(speech);
			case READY_TO_SCHEDULE -> {
			}
		}
		captureOpportunisticFields(session, speech);
	}

	private void captureOpportunisticFields(CallSession session, String speech) {
		if (session.getApplianceType() == null) {
			parseApplianceType(speech).ifPresent(session::setApplianceType);
		}
		if (!StringUtils.hasText(session.getZipCode())) {
			parseZipCode(speech).ifPresent(session::setZipCode);
		}
		if (!StringUtils.hasText(session.getSymptoms()) && looksLikeSymptom(speech)) {
			session.setSymptoms(speech);
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

	private Optional<ApplianceSpecialty> parseApplianceType(String speech) {
		String normalizedSpeech = speech.toLowerCase(Locale.ROOT);
		if (normalizedSpeech.contains("fridge") || normalizedSpeech.contains("refrigerator")) {
			return Optional.of(ApplianceSpecialty.REFRIGERATOR);
		}
		if (normalizedSpeech.contains("dishwasher")) {
			return Optional.of(ApplianceSpecialty.DISHWASHER);
		}
		if (normalizedSpeech.contains("washer") || normalizedSpeech.contains("washing machine")) {
			return Optional.of(ApplianceSpecialty.WASHER);
		}
		if (normalizedSpeech.contains("dryer")) {
			return Optional.of(ApplianceSpecialty.DRYER);
		}
		if (normalizedSpeech.contains("oven") || normalizedSpeech.contains("stove")) {
			return Optional.of(ApplianceSpecialty.OVEN);
		}
		if (normalizedSpeech.contains("microwave")) {
			return Optional.of(ApplianceSpecialty.MICROWAVE);
		}
		if (normalizedSpeech.contains("hvac") || normalizedSpeech.contains("air conditioner")
				|| normalizedSpeech.contains("furnace")) {
			return Optional.of(ApplianceSpecialty.HVAC);
		}
		return Optional.empty();
	}

	private Optional<String> parseZipCode(String speech) {
		Matcher matcher = ZIP_CODE_PATTERN.matcher(speech);
		if (matcher.find()) {
			return Optional.of(matcher.group());
		}
		return Optional.empty();
	}

	private boolean looksLikeSymptom(String speech) {
		String normalizedSpeech = speech.toLowerCase(Locale.ROOT);
		return normalizedSpeech.contains("leak")
				|| normalizedSpeech.contains("not cooling")
				|| normalizedSpeech.contains("no power")
				|| normalizedSpeech.contains("noise")
				|| normalizedSpeech.contains("smell")
				|| normalizedSpeech.contains("broken")
				|| normalizedSpeech.contains("not working");
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
