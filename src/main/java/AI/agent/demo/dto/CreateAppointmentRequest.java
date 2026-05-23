package AI.agent.demo.dto;

import AI.agent.demo.model.ApplianceSpecialty;

public record CreateAppointmentRequest(Long availabilitySlotId,
                                       ApplianceSpecialty applianceType,
                                       String issueDescription,
                                       String customerFirstName,
                                       String customerLastName,
                                       String customerPhoneNumber,
                                       String customerEmail,
                                       String customerStreetAddress,
                                       String customerZipCode) {
}
