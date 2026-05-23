package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Technician;
import AI.agent.demo.repository.TechnicianRepository;
import AI.agent.demo.dto.SchedulingMatchResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

	private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 5, 24, 8, 0);
	private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 5, 24, 18, 0);

	@Mock
	private TechnicianRepository technicianRepository;

	@InjectMocks
	private SchedulingService schedulingService;

	@Test
	void findMatchesReturnsEligibleTechniciansWithOpenSlotsSortedByTechnicianName() {
		Technician zed = technician("Zed", "Taylor", "60601", ApplianceSpecialty.REFRIGERATOR);
		AvailabilitySlot lateSlot = slot(14, 16);
		AvailabilitySlot earlySlot = slot(9, 11);
		zed.addAvailabilitySlot(lateSlot).addAvailabilitySlot(earlySlot);

		Technician ava = technician("Ava", "Stone", "60601", ApplianceSpecialty.REFRIGERATOR);
		AvailabilitySlot avaSlot = slot(10, 12);
		ava.addAvailabilitySlot(avaSlot);

		when(technicianRepository.findAll()).thenReturn(List.of(zed, ava));

		List<SchedulingMatchResponse> matches = schedulingService.findMatches("60601", ApplianceSpecialty.REFRIGERATOR,
				WINDOW_START, WINDOW_END);

		assertThat(matches).hasSize(2);
		assertThat(matches).extracting("technicianName").containsExactly("Ava Stone", "Zed Taylor");
		assertThat(matches.get(1).openSlots()).extracting("slotId")
				.containsExactly(earlySlot.getId(), lateSlot.getId());
	}

	@Test
	void findMatchesFiltersOutZipSpecialtyBookedAndOutOfWindowSlots() {
		Technician wrongZip = technician("Wrong", "Zip", "60602", ApplianceSpecialty.REFRIGERATOR)
				.addAvailabilitySlot(slot(9, 11));
		Technician wrongSpecialty = technician("Wrong", "Specialty", "60601", ApplianceSpecialty.DRYER)
				.addAvailabilitySlot(slot(9, 11));
		AvailabilitySlot bookedSlot = slot(9, 11);
		bookedSlot.markBooked();
		Technician bookedOnly = technician("Booked", "Only", "60601", ApplianceSpecialty.REFRIGERATOR)
				.addAvailabilitySlot(bookedSlot);
		Technician outsideWindow = technician("Outside", "Window", "60601", ApplianceSpecialty.REFRIGERATOR)
				.addAvailabilitySlot(slot(7, 8));

		when(technicianRepository.findAll()).thenReturn(List.of(wrongZip, wrongSpecialty, bookedOnly, outsideWindow));

		List<SchedulingMatchResponse> matches = schedulingService.findMatches("60601", ApplianceSpecialty.REFRIGERATOR,
				WINDOW_START, WINDOW_END);

		assertThat(matches).isEmpty();
	}

	@Test
	void findMatchesIncludesSlotThatExactlyMatchesDesiredWindowBoundaries() {
		Technician technician = technician("Boundary", "Tech", "60601", ApplianceSpecialty.OVEN);
		AvailabilitySlot boundarySlot = new AvailabilitySlot(WINDOW_START, WINDOW_END);
		technician.addAvailabilitySlot(boundarySlot);

		when(technicianRepository.findAll()).thenReturn(List.of(technician));

		List<SchedulingMatchResponse> matches = schedulingService.findMatches("60601", ApplianceSpecialty.OVEN,
				WINDOW_START, WINDOW_END);

		assertThat(matches).hasSize(1);
		assertThat(matches.get(0).openSlots()).hasSize(1);
		assertThat(matches.get(0).openSlots().get(0).startsAt()).isEqualTo(WINDOW_START);
		assertThat(matches.get(0).openSlots().get(0).endsAt()).isEqualTo(WINDOW_END);
	}

	@Test
	void findMatchesRejectsInvalidDateWindow() {
		assertThatThrownBy(() -> schedulingService.findMatches("60601", ApplianceSpecialty.HVAC, WINDOW_END, WINDOW_END))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("desiredEnd must be after desiredStart");

		assertThatThrownBy(() -> schedulingService.findMatches("60601", ApplianceSpecialty.HVAC, WINDOW_END, WINDOW_START))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("desiredEnd must be after desiredStart");
	}

	private Technician technician(String firstName, String lastName, String zipCode, ApplianceSpecialty specialty) {
		Technician technician = new Technician(firstName, lastName, firstName.toLowerCase() + "@example.com", "555-0100")
				.addServiceAreas(zipCode)
				.addSpecialties(specialty);
		technician.setId((long) Math.abs((firstName + lastName).hashCode()));
		return technician;
	}

	private AvailabilitySlot slot(int startHour, int endHour) {
		AvailabilitySlot slot = new AvailabilitySlot(
				LocalDateTime.of(2026, 5, 24, startHour, 0),
				LocalDateTime.of(2026, 5, 24, endHour, 0));
		slot.setId((long) (startHour * 100 + endHour));
		return slot;
	}
}
