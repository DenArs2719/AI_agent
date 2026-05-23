package AI.agent.demo.service;

import AI.agent.demo.dto.SchedulingMatchResponse;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Technician;
import AI.agent.demo.repository.TechnicianRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulingService {
	private final TechnicianRepository technicianRepository;

	public List<SchedulingMatchResponse> findMatches(String customerZipCode,
													 ApplianceSpecialty applianceType,
													 LocalDateTime desiredStart,
													 LocalDateTime desiredEnd) {
		if (desiredEnd.isBefore(desiredStart) || desiredEnd.isEqual(desiredStart)) {
			throw new IllegalArgumentException("desiredEnd must be after desiredStart");
		}
		return technicianRepository
				.findAll()
				.stream()
				.filter(technician -> technician.getServiceAreas().contains(customerZipCode))
				.filter(technician -> technician.getSpecialties().contains(applianceType))
				.map(technician -> SchedulingMatchResponse.from(
						technician,
						matchingOpenSlots(technician, desiredStart, desiredEnd))
				)
				.filter(match -> !match.openSlots().isEmpty())
				.sorted(Comparator.comparing(SchedulingMatchResponse::technicianName))
				.toList();
	}

	private List<AvailabilitySlot> matchingOpenSlots(Technician technician,
													 LocalDateTime desiredStart,
													 LocalDateTime desiredEnd) {
		return technician
				.getAvailabilitySlots()
				.stream()
				.filter(slot -> !slot.isBooked())
				.filter(slot -> !slot.getStartsAt().isBefore(desiredStart))
				.filter(slot -> !slot.getEndsAt().isAfter(desiredEnd))
				.sorted(Comparator.comparing(AvailabilitySlot::getStartsAt))
				.toList();
	}
}
