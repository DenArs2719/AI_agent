package AI.agent.demo.repository;

import AI.agent.demo.model.CallSession;
import AI.agent.demo.model.ConversationStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {

	Optional<CallSession> findByCallSid(String callSid);

	@Query("""
			select session
			from CallSession session
			where session.callerPhoneNumber = :phoneNumber
			  and session.callSid <> :currentCallSid
			  and session.appointmentId is null
			  and session.currentStage not in :excludedStages
			order by session.id desc
			""")
	List<CallSession> findIncompleteSessionsByCallerPhoneNumber(
			String phoneNumber,
			String currentCallSid,
			Collection<ConversationStage> excludedStages,
			Pageable pageable);

	default Optional<CallSession> findLatestIncompleteSessionByCallerPhoneNumber(
			String phoneNumber,
			String currentCallSid,
			Collection<ConversationStage> excludedStages) {
		return findIncompleteSessionsByCallerPhoneNumber(
				phoneNumber,
				currentCallSid,
				excludedStages,
				PageRequest.of(0, 1))
				.stream()
				.findFirst();
	}
}
