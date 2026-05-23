package AI.agent.demo.service;

import AI.agent.demo.dto.SchedulingMatchResponse;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Technician;
import AI.agent.demo.repository.AvailabilitySlotRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchedulingService {
	private final AvailabilitySlotRepository availabilitySlotRepository;

	@Transactional(readOnly = true)
	public List<SchedulingMatchResponse> findMatches(String customerZipCode,
													 ApplianceSpecialty applianceType,
													 LocalDateTime desiredStart,
													 LocalDateTime desiredEnd) {
		if (desiredEnd.isBefore(desiredStart) || desiredEnd.isEqual(desiredStart)) {
			throw new IllegalArgumentException("desiredEnd must be after desiredStart");
		}
		return toMatches(availabilitySlotRepository.findMatchingOpenSlots(
				customerZipCode,
				applianceType,
				desiredStart,
				desiredEnd));
	}

	private List<SchedulingMatchResponse> toMatches(List<AvailabilitySlot> openSlots) {
		Map<Technician, List<AvailabilitySlot>> slotsByTechnician = openSlots
				.stream()
				.collect(
						LinkedHashMap::new,
						(map, slot) -> map.computeIfAbsent(slot.getTechnician(), ignored -> new ArrayList<>())
								.add(slot),
						Map::putAll);
		return slotsByTechnician
				.entrySet()
				.stream()
				.map(entry -> SchedulingMatchResponse.from(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparing(SchedulingMatchResponse::technicianName))
				.toList();
	}
}
