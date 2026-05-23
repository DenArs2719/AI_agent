package AI.agent.demo.dto;

import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import AI.agent.demo.model.ApplianceSpecialty;

import java.time.LocalDateTime;

public record AppointmentResponse(Long appointmentId,
                                  AppointmentStatus status,
                                  ApplianceSpecialty applianceSpecialty,
                                  LocalDateTime scheduledAt,
                                  Long technicianId,
                                  String technicianName,
                                  Long customerId,
                                  String customerName) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getStatus(),
                appointment.getApplianceSpecialty(),
                appointment.getScheduledAt(),
                appointment.getTechnician().getId(),
                appointment.getTechnician().getFirstName() + " " + appointment.getTechnician().getLastName(),
                appointment.getCustomer().getId(),
                appointment.getCustomer().getFirstName() + " " + appointment.getCustomer().getLastName());
    }
}
