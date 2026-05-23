package AI.agent.demo.service;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.dto.ai.CallSessionUpdates;
import AI.agent.demo.dto.AppointmentResponse;
import AI.agent.demo.dto.CreateAppointmentRequest;
import AI.agent.demo.dto.SchedulingMatchResponse;
import AI.agent.demo.dto.TroubleshootingScript;
import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import AI.agent.demo.repository.AppointmentRepository;
import AI.agent.demo.repository.CallSessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceWebhookService {
	private static final String LOCAL_DEMO_CALL_SID = "LOCAL_DEMO_CALL";

	private final CallSessionRepository callSessionRepository;
	private final AiDiagnosticService aiDiagnosticService;
	private final SchedulingService schedulingService;
	private final AppointmentService appointmentService;
	private final TroubleshootingScriptService troubleshootingScriptService;
	private final AppointmentRepository appointmentRepository;

	@Transactional
	public String incomingCallInstructions(String callSid, String callerPhoneNumber) {
		CallSession session = getOrCreateSession(callSid);
		captureCallerPhoneNumber(session, callerPhoneNumber);
		if (isNewSession(session) && linkExistingAppointment(session)) {
			callSessionRepository.save(session);
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"Thanks for calling Sears Home Services. " + questionFor(session)));
		}
		session.setCurrentStage(nextMissingStage(session));
		callSessionRepository.save(session);
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"Thanks for calling Sears Home Services. " + questionFor(session)));
	}

	@Transactional
	public String respondToCaller(String callSid, String callerPhoneNumber, String speechResult) {
		CallSession session = getOrCreateSession(callSid);
		captureCallerPhoneNumber(session, callerPhoneNumber);
		if (!StringUtils.hasText(speechResult)) {
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"I did not catch that. " + questionFor(session)));
		}
		if (session.getCurrentStage() == ConversationStage.SLOT_CONFIRMATION) {
			return handleSlotConfirmation(session, speechResult.trim());
		}
		if (session.getCurrentStage() == ConversationStage.RETURNING_CALLER) {
			return handleReturningCaller(session, speechResult.trim());
		}
		AiDialogueResult aiDialogueResult;
		try {
			aiDialogueResult = aiDiagnosticService.nextTurn(session, speechResult.trim());
		}
		catch (RuntimeException exception) {
			return handleDialogueFailure(session, exception);
		}
		applyUpdates(session, aiDialogueResult.updates());
		session.setCurrentStage(nextMissingStage(session));
		callSessionRepository.save(session);
		if (session.getCurrentStage() == ConversationStage.READY_TO_SCHEDULE) {
			return proposeAppointmentSlot(session);
		}
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				questionFor(session)));
	}

	private String handleDialogueFailure(CallSession session, RuntimeException exception) {
		log.warn("Voice dialogue failed for callSid={}", session.getCallSid(), exception);
		ConversationStage failedStage = session.getCurrentStage();
		session.setStageBeforeFailure(failedStage);
		session.setCurrentStage(ConversationStage.FAILED);
		session.setLastErrorMessage(errorMessageFor(exception));
		session.setLastErrorAt(LocalDateTime.now());
		session.setErrorCount(session.getErrorCount() + 1);
		callSessionRepository.save(session);
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"I am sorry, I had trouble processing that answer. " + questionFor(failedStage, session)));
	}

	private CallSession getOrCreateSession(String callSid) {
		String normalizedCallSid = StringUtils.hasText(callSid) ? callSid.trim() : LOCAL_DEMO_CALL_SID;
		return callSessionRepository
				.findByCallSid(normalizedCallSid)
				.orElseGet(() -> new CallSession(normalizedCallSid));
	}

	private void captureCallerPhoneNumber(CallSession session, String callerPhoneNumber) {
		if (!StringUtils.hasText(session.getCallerPhoneNumber()) && StringUtils.hasText(callerPhoneNumber)) {
			session.setCallerPhoneNumber(callerPhoneNumber.trim());
		}
	}

	private boolean isNewSession(CallSession session) {
		return session.getId() == null
				&& session.getAppointmentId() == null
				&& session.getCurrentStage() == ConversationStage.APPLIANCE_TYPE;
	}

	private boolean linkExistingAppointment(CallSession session) {
		if (!StringUtils.hasText(session.getCallerPhoneNumber())) {
			return false;
		}
		return appointmentRepository
				.findFirstByCustomerPhoneNumberAndStatusNotOrderByScheduledAtDesc(
						session.getCallerPhoneNumber(),
						AppointmentStatus.CANCELED)
				.map(appointment -> {
					session.setAppointmentId(appointment.getId());
					session.setCustomerName(customerNameFor(appointment));
					session.setApplianceType(appointment.getApplianceSpecialty());
					session.setZipCode(appointment.getCustomer().getZipCode());
					session.setCurrentStage(ConversationStage.RETURNING_CALLER);
					return true;
				})
				.orElse(false);
	}

	private String customerNameFor(Appointment appointment) {
		return appointment.getCustomer().getFirstName() + " " + appointment.getCustomer().getLastName();
	}

	private String proposeAppointmentSlot(CallSession session) {
		AvailabilityWindow availabilityWindow = availabilityWindowFor(session.getAvailability());
		List<SchedulingMatchResponse> matches = schedulingService.findMatches(
				session.getZipCode(),
				session.getApplianceType(),
				availabilityWindow.start(),
				availabilityWindow.end());
		if (matches.isEmpty()) {
			session.setAvailability(null);
			session.setCurrentStage(ConversationStage.AVAILABILITY);
			callSessionRepository.save(session);
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"I could not find an open appointment for that time. What other day and time works for you?"));
		}
		SchedulingMatchResponse match = matches.get(0);
		SchedulingMatchResponse.OpenSlotResponse openSlot = match.openSlots().get(0);
		session.setProposedSlotId(openSlot.slotId());
		session.setProposedTechnicianName(match.technicianName());
		session.setCurrentStage(ConversationStage.SLOT_CONFIRMATION);
		callSessionRepository.save(session);
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"I found an appointment with " + match.technicianName() + " starting at "
						+ openSlot.startsAt() + ". Would you like me to book and confirm this appointment?"));
	}

	private String handleSlotConfirmation(CallSession session, String speech) {
		if (isNegativeConfirmation(speech)) {
			session.setProposedSlotId(null);
			session.setProposedTechnicianName(null);
			session.setAvailability(null);
			session.setCurrentStage(ConversationStage.AVAILABILITY);
			callSessionRepository.save(session);
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"No problem. What other day and time works for the appointment?"));
		}
		if (!isPositiveConfirmation(speech)) {
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"Please say yes to confirm this appointment, or no to look for another time."));
		}
		AppointmentResponse createdAppointment = appointmentService.createAppointment(createAppointmentRequest(session));
		AppointmentResponse confirmedAppointment = appointmentService.confirmAppointment(createdAppointment.appointmentId());
		session.setAppointmentId(confirmedAppointment.appointmentId());
		session.setCurrentStage(ConversationStage.APPOINTMENT_CONFIRMED);
		callSessionRepository.save(session);
		return response(say("Your appointment is confirmed with " + confirmedAppointment.technicianName()
				+ " at " + confirmedAppointment.scheduledAt() + ". Thank you for calling Sears Home Services."));
	}

	private String handleReturningCaller(CallSession session, String speech) {
		if (isNewIssueIntent(speech)) {
			startNewIssue(session);
			callSessionRepository.save(session);
			return response(gather(
					"/voice/respond",
					"speech",
					"auto",
					"No problem. " + questionFor(session)));
		}
		if (isExistingAppointmentIntent(speech)) {
			session.setCurrentStage(ConversationStage.APPOINTMENT_CONFIRMED);
			callSessionRepository.save(session);
			return response(say("I found your existing appointment.The appointment remains confirmed. "
					+ "Thank you for calling Sears Home Services."));
		}
		return response(gather(
				"/voice/respond",
				"speech",
				"auto",
				"Please say existing appointment if you are calling about that appointment, or new issue to start a new service request."));
	}

	private void startNewIssue(CallSession session) {
		session.setAppointmentId(null);
		session.setApplianceType(null);
		session.setSymptoms(null);
		session.setErrorCodes(null);
		session.setPriorTroubleshootingSteps(null);
		session.setZipCode(null);
		session.setCustomerName(null);
		session.setAvailability(null);
		session.setProposedSlotId(null);
		session.setProposedTechnicianName(null);
		session.setCurrentStage(ConversationStage.APPLIANCE_TYPE);
	}

	private CreateAppointmentRequest createAppointmentRequest(CallSession session) {
		String[] nameParts = splitName(session.getCustomerName());
		return new CreateAppointmentRequest(
				session.getProposedSlotId(),
				session.getApplianceType(),
				issueDescription(session),
				nameParts[0],
				nameParts[1],
				StringUtils.hasText(session.getCallerPhoneNumber()) ? session.getCallerPhoneNumber() : "unknown",
				"voice-customer@example.com",
				"Address captured by voice follow-up",
				session.getZipCode());
	}

	private String issueDescription(CallSession session) {
		return "Symptoms: " + session.getSymptoms()
				+ "; error codes: " + session.getErrorCodes()
				+ "; troubleshooting: " + session.getPriorTroubleshootingSteps();
	}

	private String[] splitName(String customerName) {
		if (!StringUtils.hasText(customerName)) {
			return new String[] { "Voice", "Customer" };
		}
		String[] parts = customerName.trim().split("\\s+", 2);
		if (parts.length == 1) {
			return new String[] { parts[0], "Customer" };
		}
		return parts;
	}

	private AvailabilityWindow availabilityWindowFor(String availability) {
		LocalDate today = LocalDate.now();
		String normalizedAvailability = availability == null ? "" : availability.toLowerCase(Locale.ROOT);
		LocalDate targetDate = today.plusDays(1);
		if (normalizedAvailability.contains("after tomorrow")) {
			targetDate = today.plusDays(2);
		}
		LocalTime startTime = startTimeForAvailability(normalizedAvailability);
		LocalTime endTime = endTimeForAvailability(normalizedAvailability);
		return new AvailabilityWindow(
				LocalDateTime.of(targetDate, startTime),
				LocalDateTime.of(targetDate, endTime));
	}

	private LocalTime startTimeForAvailability(String normalizedAvailability) {
		if (normalizedAvailability.contains("morning")) {
			return LocalTime.of(8, 0);
		}
		if (normalizedAvailability.contains("afternoon")) {
			return LocalTime.of(12, 0);
		}
		if (normalizedAvailability.contains("evening")) {
			return LocalTime.of(17, 0);
		}
		return LocalTime.of(8, 0);
	}

	private LocalTime endTimeForAvailability(String normalizedAvailability) {
		if (normalizedAvailability.contains("morning")) {
			return LocalTime.of(12, 0);
		}
		if (normalizedAvailability.contains("afternoon")) {
			return LocalTime.of(17, 0);
		}
		if (normalizedAvailability.contains("evening")) {
			return LocalTime.of(21, 0);
		}
		return LocalTime.of(18, 0);
	}

	private boolean isPositiveConfirmation(String speech) {
		String normalizedSpeech = speech.toLowerCase(Locale.ROOT);
		return normalizedSpeech.contains("yes")
				|| normalizedSpeech.contains("confirm")
				|| normalizedSpeech.contains("book it")
				|| normalizedSpeech.contains("that works");
	}

	private boolean isNegativeConfirmation(String speech) {
		String normalizedSpeech = speech.toLowerCase(Locale.ROOT);
		return normalizedSpeech.contains("no")
				|| normalizedSpeech.contains("another")
				|| normalizedSpeech.contains("different");
	}

	private boolean isExistingAppointmentIntent(String speech) {
		String normalizedSpeech = speech.toLowerCase(Locale.ROOT);
		return normalizedSpeech.contains("existing")
				|| normalizedSpeech.contains("appointment")
				|| normalizedSpeech.contains("same")
				|| normalizedSpeech.contains("yes");
	}

	private boolean isNewIssueIntent(String speech) {
		String normalizedSpeech = speech.toLowerCase(Locale.ROOT);
		return normalizedSpeech.contains("new")
				|| normalizedSpeech.contains("different issue")
				|| normalizedSpeech.contains("another issue")
				|| normalizedSpeech.contains("new service");
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
		if (session.getAppointmentId() != null) {
			return ConversationStage.APPOINTMENT_CONFIRMED;
		}
		if (session.getCurrentStage() == ConversationStage.FAILED
				&& session.getStageBeforeFailure() != null) {
			return session.getStageBeforeFailure();
		}
		if (session.getProposedSlotId() != null) {
			return ConversationStage.SLOT_CONFIRMATION;
		}
		return ConversationStage.READY_TO_SCHEDULE;
	}

	private String questionFor(CallSession session) {
		return questionFor(session.getCurrentStage(), session);
	}

	private String questionFor(ConversationStage stage, CallSession session) {
		return switch (stage) {
			case APPLIANCE_TYPE -> "What appliance needs service?";
			case SYMPTOMS -> "What symptoms are you seeing?";
			case ERROR_CODES -> "Do you see any error codes on the appliance? If not, say no error code.";
			case TROUBLESHOOTING_STEPS -> troubleshootingQuestionFor(session);
			case ZIP_CODE -> "What ZIP code is the appliance located in?";
			case CUSTOMER_NAME -> "What is your name?";
			case AVAILABILITY -> "What day and time works best for the appointment?";
			case READY_TO_SCHEDULE -> "I have enough information to look for appointment times.";
			case RETURNING_CALLER -> "I found an existing appointment for this phone number. Are you calling about that appointment or a new appliance issue?";
			case SLOT_CONFIRMATION -> "Would you like to confirm the proposed appointment?";
			case APPOINTMENT_CONFIRMED -> "Your appointment is confirmed.";
			case FAILED -> "I had trouble with the last response. "
					+ questionFor(session.getStageBeforeFailure() == null
							? ConversationStage.APPLIANCE_TYPE
							: session.getStageBeforeFailure(), session);
			case ABANDONED -> "This call session has ended.";
		};
	}

	private String errorMessageFor(RuntimeException exception) {
		String message = exception.getMessage();
		if (!StringUtils.hasText(message)) {
			return exception.getClass().getSimpleName();
		}
		return message.length() > 1000 ? message.substring(0, 1000) : message;
	}

	private String troubleshootingQuestionFor(CallSession session) {
		TroubleshootingScript script = troubleshootingScriptService.getScript(session.getApplianceType());
		return "Please try these safe checks if you have not already: "
				+ String.join(" ", script.safeChecks().stream().limit(3).toList())
				+ " After that, tell me what you tried and whether the issue is still happening.";
	}

	private record AvailabilityWindow(LocalDateTime start, LocalDateTime end) {
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
