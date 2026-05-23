package AI.agent.demo.repository;

import AI.agent.demo.model.AvailabilitySlot;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

	@EntityGraph(attributePaths = { "technician", "technician.serviceAreas", "technician.specialties" })
	Optional<AvailabilitySlot> findWithTechnicianById(Long id);
}
