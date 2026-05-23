package AI.agent.demo.repository;

import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

	@EntityGraph(attributePaths = { "technician", "technician.serviceAreas", "technician.specialties" })
	Optional<AvailabilitySlot> findWithTechnicianById(Long id);

	@EntityGraph(attributePaths = { "technician", "technician.serviceAreas", "technician.specialties" })
	@Query("""
			select distinct slot
			from AvailabilitySlot slot
			join slot.technician technician
			join technician.serviceAreas serviceArea
			join technician.specialties specialty
			where serviceArea = :customerZipCode
			  and specialty = :applianceType
			  and slot.booked = false
			  and slot.startsAt >= :desiredStart
			  and slot.endsAt <= :desiredEnd
			order by technician.firstName, technician.lastName, slot.startsAt
			""")
	List<AvailabilitySlot> findMatchingOpenSlots(
			@Param("customerZipCode") String customerZipCode,
			@Param("applianceType") ApplianceSpecialty applianceType,
			@Param("desiredStart") LocalDateTime desiredStart,
			@Param("desiredEnd") LocalDateTime desiredEnd);
}
