package AI.agent.demo.repository;

import AI.agent.demo.model.CallSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {

	Optional<CallSession> findByCallSid(String callSid);
}
