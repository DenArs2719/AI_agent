package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import AI.agent.demo.dto.AppointmentResponse;
import AI.agent.demo.dto.CreateAppointmentRequest;
import AI.agent.demo.dto.SchedulingMatchResponse;
import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.dto.ai.CallSessionUpdates;
import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import AI.agent.demo.model.Customer;
import AI.agent.demo.repository.AppointmentRepository;
import AI.agent.demo.repository.CallSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoiceWebhookServiceTest {
	@Mock
	private CallSessionRepository callSessionRepository;

	@Mock
	private AiDiagnosticService aiDiagnosticService;

	@Mock
	private SchedulingService schedulingService;

	@Mock
	private AppointmentService appointmentService;

	@Mock
	private TroubleshootingScriptService troubleshootingScriptService;

	@Mock
	private AppointmentRepository appointmentRepository;

	@InjectMocks
	private VoiceWebhookService voiceWebhookService;

	@Test
	void incomingCallInstructionsAskForDiagnosticDetailsAndCollectSpeech() {
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.empty());
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.incomingCallInstructions("CA123", "+17735550101");

		assertThat(twiml).contains("<Response>");
		assertThat(twiml).contains("<Gather action=\"/voice/respond\" method=\"POST\" input=\"speech\" speechTimeout=\"auto\">");
		assertThat(twiml).contains("What appliance needs service?");

		ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
		verify(callSessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().getCallSid()).isEqualTo("CA123");
		assertThat(sessionCaptor.getValue().getCallerPhoneNumber()).isEqualTo("+17735550101");
		assertThat(sessionCaptor.getValue().getCurrentStage()).isEqualTo(ConversationStage.APPLIANCE_TYPE);
	}

	@Test
	void incomingCallInstructionsLinksExistingAppointmentForReturningCaller() {
		Appointment appointment = mock(Appointment.class);
		Customer customer = mock(Customer.class);
		when(callSessionRepository.findByCallSid("CA_RETURN")).thenReturn(Optional.empty());
		when(appointmentRepository.findFirstByCustomerPhoneNumberAndStatusNotOrderByScheduledAtDesc(
				"+17735550101",
				AppointmentStatus.CANCELED))
				.thenReturn(Optional.of(appointment));
		when(appointment.getId()).thenReturn(501L);
		when(appointment.getCustomer()).thenReturn(customer);
		when(appointment.getApplianceSpecialty()).thenReturn(ApplianceSpecialty.REFRIGERATOR);
		when(customer.getFirstName()).thenReturn("Jane");
		when(customer.getLastName()).thenReturn("Smith");
		when(customer.getZipCode()).thenReturn("60601");
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.incomingCallInstructions("CA_RETURN", "+17735550101");

		assertThat(twiml).contains("I found an existing appointment for this phone number");
		ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
		verify(callSessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().getAppointmentId()).isEqualTo(501L);
		assertThat(sessionCaptor.getValue().getCustomerName()).isEqualTo("Jane Smith");
		assertThat(sessionCaptor.getValue().getApplianceType()).isEqualTo(ApplianceSpecialty.REFRIGERATOR);
		assertThat(sessionCaptor.getValue().getZipCode()).isEqualTo("60601");
		assertThat(sessionCaptor.getValue().getCurrentStage()).isEqualTo(ConversationStage.RETURNING_CALLER);
	}

	@Test
	void respondToCallerStartsNewIssueForReturningCallerWhenRequested() {
		CallSession session = new CallSession("CA_RETURN");
		session.setAppointmentId(501L);
		session.setCallerPhoneNumber("+17735550101");
		session.setCustomerName("Jane Smith");
		session.setApplianceType(ApplianceSpecialty.REFRIGERATOR);
		session.setZipCode("60601");
		session.setCurrentStage(ConversationStage.RETURNING_CALLER);
		when(callSessionRepository.findByCallSid("CA_RETURN")).thenReturn(Optional.of(session));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA_RETURN", "+17735550101", "new issue");

		assertThat(session.getAppointmentId()).isNull();
		assertThat(session.getCustomerName()).isNull();
		assertThat(session.getApplianceType()).isNull();
		assertThat(session.getZipCode()).isNull();
		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.APPLIANCE_TYPE);
		assertThat(twiml).contains("No problem");
		assertThat(twiml).contains("What appliance needs service?");
	}

	@Test
	void respondToCallerAcknowledgesExistingAppointmentForReturningCaller() {
		CallSession session = new CallSession("CA_RETURN");
		session.setAppointmentId(501L);
		session.setCurrentStage(ConversationStage.RETURNING_CALLER);
		when(callSessionRepository.findByCallSid("CA_RETURN")).thenReturn(Optional.of(session));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA_RETURN", null, "existing appointment");

		assertThat(session.getAppointmentId()).isEqualTo(501L);
		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.APPOINTMENT_CONFIRMED);
		assertThat(twiml).contains("I found your existing appointment");
		assertThat(twiml).doesNotContain("<Gather");
	}

	@Test
	void respondToCallerRepromptsForCurrentStageWhenSpeechIsMissing() {
		CallSession session = new CallSession("CA123");
		session.setCurrentStage(ConversationStage.ZIP_CODE);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, " ");

		assertThat(twiml).contains("I did not catch that");
		assertThat(twiml).contains("What ZIP code is the appliance located in?");
	}

	@Test
	void respondToCallerCapturesKnownFieldsAndDoesNotAskForThemAgain() {
		CallSession session = new CallSession("CA123");
		session.setCurrentStage(ConversationStage.APPLIANCE_TYPE);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(aiDiagnosticService.nextTurn(session, "My refrigerator is leaking in 60601"))
				.thenReturn(new AiDialogueResult("Do you see any error codes on the appliance?",
						new CallSessionUpdates(
								ApplianceSpecialty.REFRIGERATOR,
								"My refrigerator is leaking in 60601",
								null,
								null,
								"60601",
								null,
								null)));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, "My refrigerator is leaking in 60601");

		assertThat(session.getApplianceType()).isEqualTo(ApplianceSpecialty.REFRIGERATOR);
		assertThat(session.getSymptoms()).isEqualTo("My refrigerator is leaking in 60601");
		assertThat(session.getZipCode()).isEqualTo("60601");
		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.ERROR_CODES);
		assertThat(twiml).contains("Do you see any error codes");
		assertThat(twiml).doesNotContain("What appliance needs service?");
		assertThat(twiml).doesNotContain("What ZIP code");
	}

	@Test
	void respondToCallerCapturesEachStageAndMovesToNextMissingField() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.WASHER);
		session.setSymptoms("Washer makes a loud noise");
		session.setErrorCodes("No error code");
		session.setCurrentStage(ConversationStage.TROUBLESHOOTING_STEPS);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(aiDiagnosticService.nextTurn(session, "I checked the power and restarted it"))
				.thenReturn(new AiDialogueResult("What ZIP code is the appliance located in?",
						new CallSessionUpdates(
								null,
								null,
								null,
								"I checked the power and restarted it",
								null,
								null,
								null)));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, "I checked the power and restarted it");

		assertThat(session.getPriorTroubleshootingSteps()).isEqualTo("I checked the power and restarted it");
		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.ZIP_CODE);
		assertThat(twiml).contains("What ZIP code is the appliance located in?");
	}

	@Test
	void respondToCallerWalksCallerThroughSafeTroubleshootingChecks() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.REFRIGERATOR);
		session.setSymptoms("Refrigerator is leaking");
		session.setCurrentStage(ConversationStage.ERROR_CODES);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(aiDiagnosticService.nextTurn(session, "No error code"))
				.thenReturn(new AiDialogueResult("Troubleshooting question from AI",
						new CallSessionUpdates(null, null, "No error code", null, null, null, null)));
		when(troubleshootingScriptService.getScript(ApplianceSpecialty.REFRIGERATOR))
				.thenReturn(new AI.agent.demo.dto.TroubleshootingScript(
						ApplianceSpecialty.REFRIGERATOR,
						"Refrigerator basic checks",
						List.of(
								"Confirm the refrigerator is plugged in.",
								"Make sure refrigerator and freezer doors are fully closed.",
								"Confirm vents inside the refrigerator are not blocked.")));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, "No error code");

		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.TROUBLESHOOTING_STEPS);
		assertThat(twiml).contains("Please try these safe checks");
		assertThat(twiml).contains("Confirm the refrigerator is plugged in.");
		assertThat(twiml).contains("After that, tell me what you tried");
	}

	@Test
	void respondToCallerRecordsFailureAndReturnsRetryTwiMLWhenDialogueFails() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.REFRIGERATOR);
		session.setCurrentStage(ConversationStage.SYMPTOMS);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(aiDiagnosticService.nextTurn(session, "It is making a loud noise"))
				.thenThrow(new IllegalStateException("OpenAI timeout"));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, "It is making a loud noise");

		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.FAILED);
		assertThat(session.getStageBeforeFailure()).isEqualTo(ConversationStage.SYMPTOMS);
		assertThat(session.getErrorCount()).isEqualTo(1);
		assertThat(session.getLastErrorMessage()).contains("OpenAI timeout");
		assertThat(session.getLastErrorAt()).isNotNull();
		assertThat(twiml).contains("I am sorry, I had trouble processing that answer");
		assertThat(twiml).contains("What symptoms are you seeing?");
		assertThat(twiml).contains("<Gather");
	}

	@Test
	void respondToCallerProposesSlotWhenAllRequiredFieldsAreCaptured() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.DRYER);
		session.setSymptoms("Dryer has no heat");
		session.setErrorCodes("No error code");
		session.setPriorTroubleshootingSteps("Cleaned the lint filter");
		session.setZipCode("60601");
		session.setCustomerName("Jane Smith");
		session.setCurrentStage(ConversationStage.AVAILABILITY);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(aiDiagnosticService.nextTurn(session, "Tomorrow morning"))
				.thenReturn(new AiDialogueResult(
						"Thank you. I have enough information to look for matching technicians and appointment times.",
						new CallSessionUpdates(null, null, null, null, null, null, "Tomorrow morning")));
		when(schedulingService.findMatches(any(), any(), any(), any()))
				.thenReturn(List.of(match()));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, "Tomorrow morning");

		assertThat(session.getAvailability()).isEqualTo("Tomorrow morning");
		assertThat(session.getProposedSlotId()).isEqualTo(101L);
		assertThat(session.getProposedTechnicianName()).isEqualTo("Ava Martinez");
		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.SLOT_CONFIRMATION);
		assertThat(twiml).contains("I found an appointment with Ava Martinez");
		assertThat(twiml).contains("Would you like me to book and confirm this appointment?");
	}

	@Test
	void respondToCallerUsesRequestedEveningWindowForScheduling() {
		CallSession session = readyForSchedulingSession();
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(aiDiagnosticService.nextTurn(session, "Tomorrow evening"))
				.thenReturn(new AiDialogueResult(
						"Thank you. I have enough information to look for matching technicians and appointment times.",
						new CallSessionUpdates(null, null, null, null, null, null, "Tomorrow evening")));
		when(schedulingService.findMatches(any(), any(), any(), any()))
				.thenReturn(List.of(match()));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		voiceWebhookService.respondToCaller("CA123", null, "Tomorrow evening");

		verify(schedulingService).findMatches(
				eq("60601"),
				eq(ApplianceSpecialty.DRYER),
				eq(LocalDateTime.of(LocalDateTime.now().toLocalDate().plusDays(1), java.time.LocalTime.of(17, 0))),
				eq(LocalDateTime.of(LocalDateTime.now().toLocalDate().plusDays(1), java.time.LocalTime.of(21, 0))));
	}

	@Test
	void respondToCallerCreatesAndConfirmsAppointmentWhenCallerAcceptsProposedSlot() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.DRYER);
		session.setSymptoms("Dryer has no heat");
		session.setErrorCodes("No error code");
		session.setPriorTroubleshootingSteps("Cleaned the lint filter");
		session.setZipCode("60601");
		session.setCustomerName("Jane Smith");
		session.setAvailability("Tomorrow morning");
		session.setCallerPhoneNumber("+17735550101");
		session.setProposedSlotId(101L);
		session.setProposedTechnicianName("Ava Martinez");
		session.setCurrentStage(ConversationStage.SLOT_CONFIRMATION);
		when(callSessionRepository.findByCallSid("CA123")).thenReturn(Optional.of(session));
		when(appointmentService.createAppointment(any(CreateAppointmentRequest.class)))
				.thenReturn(appointmentResponse(AppointmentStatus.REQUESTED));
		when(appointmentService.confirmAppointment(501L))
				.thenReturn(appointmentResponse(AppointmentStatus.CONFIRMED));
		when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String twiml = voiceWebhookService.respondToCaller("CA123", null, "yes please confirm");

		assertThat(session.getAppointmentId()).isEqualTo(501L);
		assertThat(session.getCurrentStage()).isEqualTo(ConversationStage.APPOINTMENT_CONFIRMED);
		assertThat(twiml).contains("Your appointment is confirmed with Ava Martinez");
		assertThat(twiml).doesNotContain("<Gather");
	}

	private SchedulingMatchResponse match() {
		return new SchedulingMatchResponse(
				1L,
				"Ava Martinez",
				"312-555-0101",
				List.of("60601"),
				List.of(ApplianceSpecialty.DRYER),
				List.of(new SchedulingMatchResponse.OpenSlotResponse(
						101L,
						LocalDateTime.of(2026, 5, 24, 9, 0),
						LocalDateTime.of(2026, 5, 24, 11, 0))));
	}

	private CallSession readyForSchedulingSession() {
		CallSession session = new CallSession("CA123");
		session.setApplianceType(ApplianceSpecialty.DRYER);
		session.setSymptoms("Dryer has no heat");
		session.setErrorCodes("No error code");
		session.setPriorTroubleshootingSteps("Cleaned the lint filter");
		session.setZipCode("60601");
		session.setCustomerName("Jane Smith");
		session.setCurrentStage(ConversationStage.AVAILABILITY);
		return session;
	}

	private AppointmentResponse appointmentResponse(AppointmentStatus status) {
		return new AppointmentResponse(
				501L,
				status,
				ApplianceSpecialty.DRYER,
				LocalDateTime.of(2026, 5, 24, 9, 0),
				1L,
				"Ava Martinez",
				201L,
				"Jane Smith");
	}
}
