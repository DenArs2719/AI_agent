package AI.agent.demo.service;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.dto.ai.CallSessionUpdates;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DemoAiDiagnosticService implements AiDiagnosticService {

	private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("\\b\\d{5}\\b");

	@Override
	public AiDialogueResult nextTurn(CallSession session, String callerSpeech) {
		String speech = callerSpeech == null ? "" : callerSpeech.trim();
		CallSessionUpdates callSessionUpdates = updatesFor(session, speech);
		String assistantMessage = assistantMessageFor(nextMissingStage(session, callSessionUpdates));
		return new AiDialogueResult(assistantMessage, callSessionUpdates);
	}

	private CallSessionUpdates updatesFor(CallSession session, String speech) {
		ApplianceSpecialty applianceType = null;
		String symptoms = null;
		String errorCodes = null;
		String priorTroubleshootingSteps = null;
		String zipCode = null;
		String customerName = null;
		String availability = null;
		switch (session.getCurrentStage()) {
			case APPLIANCE_TYPE -> applianceType = parseApplianceType(speech).orElse(null);
			case SYMPTOMS -> symptoms = speech;
			case ERROR_CODES -> errorCodes = speech;
			case TROUBLESHOOTING_STEPS -> priorTroubleshootingSteps = speech;
			case ZIP_CODE -> zipCode = parseZipCode(speech).orElse(null);
			case CUSTOMER_NAME -> customerName = speech;
			case AVAILABILITY -> availability = speech;
			case READY_TO_SCHEDULE -> {
			}
		}
		if (session.getApplianceType() == null && applianceType == null) {
			applianceType = parseApplianceType(speech).orElse(null);
		}
		if (!StringUtils.hasText(session.getZipCode()) && !StringUtils.hasText(zipCode)) {
			zipCode = parseZipCode(speech).orElse(null);
		}
		if (!StringUtils.hasText(session.getSymptoms()) && !StringUtils.hasText(symptoms) && looksLikeSymptom(speech)) {
			symptoms = speech;
		}
		return new CallSessionUpdates(
				applianceType,
				blankToNull(symptoms),
				blankToNull(errorCodes),
				blankToNull(priorTroubleshootingSteps),
				blankToNull(zipCode),
				blankToNull(customerName),
				blankToNull(availability));
	}

	private ConversationStage nextMissingStage(CallSession session, CallSessionUpdates updates) {
		if (session.getApplianceType() == null && updates.applianceType() == null) {
			return ConversationStage.APPLIANCE_TYPE;
		}
		if (!StringUtils.hasText(session.getSymptoms()) && !StringUtils.hasText(updates.symptoms())) {
			return ConversationStage.SYMPTOMS;
		}
		if (!StringUtils.hasText(session.getErrorCodes()) && !StringUtils.hasText(updates.errorCodes())) {
			return ConversationStage.ERROR_CODES;
		}
		if (!StringUtils.hasText(session.getPriorTroubleshootingSteps())
				&& !StringUtils.hasText(updates.priorTroubleshootingSteps())) {
			return ConversationStage.TROUBLESHOOTING_STEPS;
		}
		if (!StringUtils.hasText(session.getZipCode()) && !StringUtils.hasText(updates.zipCode())) {
			return ConversationStage.ZIP_CODE;
		}
		if (!StringUtils.hasText(session.getCustomerName()) && !StringUtils.hasText(updates.customerName())) {
			return ConversationStage.CUSTOMER_NAME;
		}
		if (!StringUtils.hasText(session.getAvailability()) && !StringUtils.hasText(updates.availability())) {
			return ConversationStage.AVAILABILITY;
		}
		return ConversationStage.READY_TO_SCHEDULE;
	}

	private String assistantMessageFor(ConversationStage stage) {
		return switch (stage) {
			case APPLIANCE_TYPE -> "What appliance needs service?";
			case SYMPTOMS -> "What symptoms are you seeing?";
			case ERROR_CODES -> "Do you see any error codes on the appliance? If not, say no error code.";
			case TROUBLESHOOTING_STEPS -> "What troubleshooting steps have you already tried?";
			case ZIP_CODE -> "What ZIP code is the appliance located in?";
			case CUSTOMER_NAME -> "What is your name?";
			case AVAILABILITY -> "What day and time works best for the appointment?";
			case READY_TO_SCHEDULE -> "Thank you. I have enough information to look for matching technicians and appointment times.";
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
				|| normalizedSpeech.contains("not working")
				|| normalizedSpeech.contains("no heat");
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value : null;
	}
}
