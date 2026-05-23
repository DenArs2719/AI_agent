package AI.agent.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "call_sessions", indexes = {
		@Index(name = "idx_call_sessions_call_sid", columnList = "call_sid", unique = true)
})
public class CallSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "call_sid", nullable = false)
	private String callSid;

	@Enumerated(EnumType.STRING)
	private ApplianceSpecialty applianceType;

	@Column(length = 1000)
	private String symptoms;

	@Column(length = 500)
	private String errorCodes;

	@Column(length = 1000)
	private String priorTroubleshootingSteps;

	@Column(length = 10)
	private String zipCode;

	private String customerName;

	@Column(length = 500)
	private String availability;

	private String callerPhoneNumber;

	private Long proposedSlotId;

	private String proposedTechnicianName;

	private Long appointmentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConversationStage currentStage;

	@Enumerated(EnumType.STRING)
	private ConversationStage stageBeforeFailure;

	@Column(length = 1000)
	private String lastErrorMessage;

	private LocalDateTime lastErrorAt;

	private int errorCount;

	public CallSession(String callSid) {
		this.callSid = callSid;
		this.currentStage = ConversationStage.APPLIANCE_TYPE;
	}
}
