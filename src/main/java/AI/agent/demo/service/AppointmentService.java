package AI.agent.demo.service;

import AI.agent.demo.dto.AppointmentResponse;
import AI.agent.demo.dto.CreateAppointmentRequest;
import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Customer;
import AI.agent.demo.model.Technician;
import AI.agent.demo.repository.AppointmentRepository;
import AI.agent.demo.repository.AvailabilitySlotRepository;
import AI.agent.demo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        AvailabilitySlot slot = availabilitySlotRepository
                .findWithTechnicianById(request.availabilitySlotId())
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found"));
        if (slot.isBooked()) {
            throw new IllegalArgumentException("Availability slot is already booked");
        }
        Technician technician = slot.getTechnician();
        if (!technician.getSpecialties().contains(request.applianceType())) {
            throw new IllegalArgumentException("Technician does not service this appliance type");
        }
        if (!technician.getServiceAreas().contains(request.customerZipCode())) {
            throw new IllegalArgumentException("Technician does not service this ZIP code");
        }
        Customer customer = customerRepository.save(new Customer(
                request.customerFirstName(),
                request.customerLastName(),
                request.customerPhoneNumber(),
                request.customerEmail(),
                request.customerStreetAddress(),
                request.customerZipCode())
        );
        slot.markBooked();
        Appointment appointment = appointmentRepository.save(new Appointment(
                customer,
                technician,
                slot,
                request.applianceType(),
                request.issueDescription(),
                AppointmentStatus.REQUESTED)
        );
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse confirmAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Canceled appointments cannot be confirmed");
        }
        appointment.confirm();
        return AppointmentResponse.from(appointment);
    }

}
