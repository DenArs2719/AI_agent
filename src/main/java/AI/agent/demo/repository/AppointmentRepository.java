package AI.agent.demo.repository;

import AI.agent.demo.model.Appointment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
	@Query("""
			select appointment
			from Appointment appointment
			join fetch appointment.customer
			where appointment.customer.phoneNumber = :phoneNumber
			  and appointment.status <> AI.agent.demo.model.AppointmentStatus.CANCELED
			order by appointment.scheduledAt desc
			""")
	List<Appointment> findActiveAppointmentsByCustomerPhoneNumber(String phoneNumber, Pageable pageable);

	default Optional<Appointment> findLatestActiveAppointmentByCustomerPhoneNumber(String phoneNumber) {
		return findActiveAppointmentsByCustomerPhoneNumber(phoneNumber, PageRequest.of(0, 1))
				.stream()
				.findFirst();
	}
}
