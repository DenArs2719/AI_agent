package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import AI.agent.demo.dto.AppointmentResponse;
import AI.agent.demo.dto.CreateAppointmentRequest;
import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Customer;
import AI.agent.demo.model.Technician;
import AI.agent.demo.repository.AppointmentRepository;
import AI.agent.demo.repository.AvailabilitySlotRepository;
import AI.agent.demo.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

	@Mock
	private AvailabilitySlotRepository availabilitySlotRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private AppointmentRepository appointmentRepository;

	@InjectMocks
	private AppointmentService appointmentService;

	@Test
	void createAppointmentCreatesCustomerBooksSlotAndReturnsRequestedAppointment() {
		Technician technician = technician(ApplianceSpecialty.REFRIGERATOR, "60601");
		AvailabilitySlot slot = slot();
		technician.addAvailabilitySlot(slot);

		when(availabilitySlotRepository.findWithTechnicianById(10L)).thenReturn(Optional.of(slot));
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(0);
			customer.setId(20L);
			return customer;
		});
		when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
			Appointment appointment = invocation.getArgument(0);
			appointment.setId(30L);
			return appointment;
		});

		AppointmentResponse response = appointmentService.createAppointment(validRequest());

		assertThat(response.appointmentId()).isEqualTo(30L);
		assertThat(response.status()).isEqualTo(AppointmentStatus.REQUESTED);
		assertThat(response.technicianId()).isEqualTo(1L);
		assertThat(response.customerId()).isEqualTo(20L);
		assertThat(slot.isBooked()).isTrue();

		ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).save(customerCaptor.capture());
		assertThat(customerCaptor.getValue().getZipCode()).isEqualTo("60601");

		ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
		verify(appointmentRepository).save(appointmentCaptor.capture());
		assertThat(appointmentCaptor.getValue().getAvailabilitySlot()).isSameAs(slot);
		assertThat(appointmentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
	}

	@Test
	void createAppointmentRejectsMissingAvailabilitySlot() {
		when(availabilitySlotRepository.findWithTechnicianById(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> appointmentService.createAppointment(validRequest()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Availability slot not found");

		verify(customerRepository, never()).save(any());
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void createAppointmentRejectsAlreadyBookedSlot() {
		Technician technician = technician(ApplianceSpecialty.REFRIGERATOR, "60601");
		AvailabilitySlot slot = slot();
		technician.addAvailabilitySlot(slot);
		slot.markBooked();

		when(availabilitySlotRepository.findWithTechnicianById(10L)).thenReturn(Optional.of(slot));

		assertThatThrownBy(() -> appointmentService.createAppointment(validRequest()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Availability slot is already booked");

		verify(customerRepository, never()).save(any());
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void createAppointmentRejectsUnsupportedApplianceType() {
		Technician technician = technician(ApplianceSpecialty.DRYER, "60601");
		AvailabilitySlot slot = slot();
		technician.addAvailabilitySlot(slot);

		when(availabilitySlotRepository.findWithTechnicianById(10L)).thenReturn(Optional.of(slot));

		assertThatThrownBy(() -> appointmentService.createAppointment(validRequest()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Technician does not service this appliance type");

		assertThat(slot.isBooked()).isFalse();
		verify(customerRepository, never()).save(any());
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void createAppointmentRejectsUnsupportedZipCode() {
		Technician technician = technician(ApplianceSpecialty.REFRIGERATOR, "60602");
		AvailabilitySlot slot = slot();
		technician.addAvailabilitySlot(slot);

		when(availabilitySlotRepository.findWithTechnicianById(10L)).thenReturn(Optional.of(slot));

		assertThatThrownBy(() -> appointmentService.createAppointment(validRequest()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Technician does not service this ZIP code");

		assertThat(slot.isBooked()).isFalse();
		verify(customerRepository, never()).save(any());
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void confirmAppointmentChangesRequestedAppointmentToConfirmed() {
		Appointment appointment = appointment(AppointmentStatus.REQUESTED);
		when(appointmentRepository.findById(30L)).thenReturn(Optional.of(appointment));

		AppointmentResponse response = appointmentService.confirmAppointment(30L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
	}

	@Test
	void confirmAppointmentIsIdempotentForAlreadyConfirmedAppointment() {
		Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
		when(appointmentRepository.findById(30L)).thenReturn(Optional.of(appointment));

		AppointmentResponse response = appointmentService.confirmAppointment(30L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
	}

	@Test
	void confirmAppointmentRejectsCanceledAppointment() {
		when(appointmentRepository.findById(30L)).thenReturn(Optional.of(appointment(AppointmentStatus.CANCELED)));

		assertThatThrownBy(() -> appointmentService.confirmAppointment(30L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Canceled appointments cannot be confirmed");
	}

	@Test
	void confirmAppointmentRejectsMissingAppointment() {
		when(appointmentRepository.findById(30L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> appointmentService.confirmAppointment(30L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Appointment not found");
	}

	private CreateAppointmentRequest validRequest() {
		return new CreateAppointmentRequest(
				10L,
				ApplianceSpecialty.REFRIGERATOR,
				"Refrigerator is not cooling.",
				"John",
				"Smith",
				"773-555-1111",
				"john.smith@example.com",
				"10 N State St",
				"60601");
	}

	private Technician technician(ApplianceSpecialty specialty, String zipCode) {
		Technician technician = new Technician("Ava", "Martinez", "ava@example.com", "312-555-0101")
				.addServiceAreas(zipCode)
				.addSpecialties(specialty);
		technician.setId(1L);
		return technician;
	}

	private AvailabilitySlot slot() {
		AvailabilitySlot slot = new AvailabilitySlot(
				LocalDateTime.of(2026, 5, 24, 9, 0),
				LocalDateTime.of(2026, 5, 24, 11, 0));
		slot.setId(10L);
		return slot;
	}

	private Appointment appointment(AppointmentStatus status) {
		Technician technician = technician(ApplianceSpecialty.REFRIGERATOR, "60601");
		Customer customer = new Customer("John", "Smith", "773-555-1111", "john.smith@example.com",
				"10 N State St", "60601");
		customer.setId(20L);
		Appointment appointment = new Appointment(customer, technician, ApplianceSpecialty.REFRIGERATOR,
				LocalDateTime.of(2026, 5, 24, 9, 0), "Refrigerator is not cooling.", status);
		appointment.setId(30L);
		return appointment;
	}
}
