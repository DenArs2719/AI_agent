package AI.agent.demo.dto;

import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Technician;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record SchedulingMatchResponse(Long technicianId,
                                      String technicianName,
                                      String phoneNumber,
                                      List<String> serviceAreas,
                                      List<ApplianceSpecialty> specialties,
                                      List<OpenSlotResponse> openSlots) {
    public static SchedulingMatchResponse from(Technician technician, List<AvailabilitySlot> openSlots) {
        return new SchedulingMatchResponse(
                technician.getId(),
                technician.getFirstName() + " " + technician.getLastName(),
                technician.getPhoneNumber(),
                technician.getServiceAreas().stream().sorted().toList(),
                technician.getSpecialties().stream().sorted(Comparator.comparing(Enum::name)).toList(),
                openSlots.stream()
                        .sorted(Comparator.comparing(AvailabilitySlot::getStartsAt))
                        .map(OpenSlotResponse::from)
                        .toList());
    }

    public record OpenSlotResponse(Long slotId, LocalDateTime startsAt, LocalDateTime endsAt) {
        public static OpenSlotResponse from(AvailabilitySlot slot) {
            return new OpenSlotResponse(slot.getId(), slot.getStartsAt(), slot.getEndsAt());
        }
    }
}
