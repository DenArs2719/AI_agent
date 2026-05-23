package AI.agent.demo.repository;

import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
	Optional<Appointment> findFirstByCustomerPhoneNumberAndStatusNotOrderByScheduledAtDesc(
			String phoneNumber,
			AppointmentStatus excludedStatus);
}
