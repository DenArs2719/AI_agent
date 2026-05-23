package AI.agent.demo.config;

import AI.agent.demo.model.Appointment;
import AI.agent.demo.model.AppointmentStatus;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.model.AvailabilitySlot;
import AI.agent.demo.model.Customer;
import AI.agent.demo.model.Technician;
import AI.agent.demo.repository.AppointmentRepository;
import AI.agent.demo.repository.CustomerRepository;
import AI.agent.demo.repository.TechnicianRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
	private final TechnicianRepository technicianRepository;
	private final CustomerRepository customerRepository;
	private final AppointmentRepository appointmentRepository;

	@Override
	public void run(String... args) {
		if (technicianRepository.count() > 0) {
			return;
		}
		List<Technician> technicians = technicianRepository.saveAll(List.of(
				technician("Ava", "Martinez", "ava.martinez@shs.example", "312-555-0101",
						new String[] { "60601", "60602", "60603" },
						new ApplianceSpecialty[] { ApplianceSpecialty.REFRIGERATOR, ApplianceSpecialty.DISHWASHER }),
				technician("Noah", "Patel", "noah.patel@shs.example", "312-555-0102",
						new String[] { "60604", "60605", "60607" },
						new ApplianceSpecialty[] { ApplianceSpecialty.WASHER, ApplianceSpecialty.DRYER }),
				technician("Mia", "Johnson", "mia.johnson@shs.example", "312-555-0103",
						new String[] { "60608", "60609", "60616" },
						new ApplianceSpecialty[] { ApplianceSpecialty.OVEN, ApplianceSpecialty.MICROWAVE }),
				technician("Ethan", "Nguyen", "ethan.nguyen@shs.example", "312-555-0104",
						new String[] { "60610", "60611", "60614" },
						new ApplianceSpecialty[] { ApplianceSpecialty.HVAC, ApplianceSpecialty.REFRIGERATOR }),
				technician("Sofia", "Williams", "sofia.williams@shs.example", "312-555-0105",
						new String[] { "60618", "60625", "60630" },
						new ApplianceSpecialty[] { ApplianceSpecialty.DISHWASHER, ApplianceSpecialty.OVEN }),
				technician("Lucas", "Brown", "lucas.brown@shs.example", "312-555-0106",
						new String[] { "60634", "60639", "60641" },
						new ApplianceSpecialty[] { ApplianceSpecialty.WASHER, ApplianceSpecialty.MICROWAVE }),
				technician("Olivia", "Garcia", "olivia.garcia@shs.example", "312-555-0107",
						new String[] { "60643", "60652", "60655" },
						new ApplianceSpecialty[] { ApplianceSpecialty.DRYER, ApplianceSpecialty.HVAC }),
				technician("Liam", "Clark", "liam.clark@shs.example", "312-555-0108",
						new String[] { "60657", "60660", "60661" },
						new ApplianceSpecialty[] { ApplianceSpecialty.REFRIGERATOR, ApplianceSpecialty.OVEN })));

		List<Customer> customers = customerRepository.saveAll(List.of(
				new Customer("Grace", "Miller", "773-555-0201", "grace.miller@example.com", "120 N State St",
						"60601"),
				new Customer("Henry", "Davis", "773-555-0202", "henry.davis@example.com", "85 W Adams St",
						"60603"),
				new Customer("Emma", "Wilson", "773-555-0203", "emma.wilson@example.com", "2400 N Lincoln Ave",
						"60614")));

		appointmentRepository.saveAll(List.of(
				new Appointment(customers.get(0), technicians.get(0), ApplianceSpecialty.REFRIGERATOR,
						at(1, 9), "Refrigerator is not cooling consistently.", AppointmentStatus.SCHEDULED),
				new Appointment(customers.get(1), technicians.get(2), ApplianceSpecialty.OVEN,
						at(2, 13), "Oven temperature is lower than selected setting.", AppointmentStatus.REQUESTED)));
	}

	private Technician technician(String firstName,
								  String lastName,
								  String email,
								  String phoneNumber,
								  String[] zipCodes,
								  ApplianceSpecialty[] specialties) {
		Technician technician = new Technician(firstName, lastName, email, phoneNumber)
				.addServiceAreas(zipCodes)
				.addSpecialties(specialties);

		technician.addAvailabilitySlot(new AvailabilitySlot(at(1, 9), at(1, 11)));
		technician.addAvailabilitySlot(new AvailabilitySlot(at(1, 13), at(1, 15)));
		technician.addAvailabilitySlot(new AvailabilitySlot(at(2, 10), at(2, 12)));
		return technician;
	}

	private LocalDateTime at(int daysFromToday, int hour) {
		return LocalDateTime.of(LocalDate.now().plusDays(daysFromToday), LocalTime.of(hour, 0));
	}
}
