package AI.agent.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "call_sessions")
public class CallSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConversationStage currentStage;

	public CallSession(String callSid) {
		this.callSid = callSid;
		this.currentStage = ConversationStage.APPLIANCE_TYPE;
	}
}
