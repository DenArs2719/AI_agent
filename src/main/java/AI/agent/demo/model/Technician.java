package AI.agent.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "technicians")
public class Technician {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String phoneNumber;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "technician_service_areas",
			joinColumns = @JoinColumn(name = "technician_id"),
			indexes = {
					@Index(name = "idx_technician_service_areas_zip_code", columnList = "zip_code"),
					@Index(name = "idx_technician_service_areas_technician_id", columnList = "technician_id")
			})
	@Column(name = "zip_code", nullable = false, length = 10)
	private Set<String> serviceAreas = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "technician_specialties",
			joinColumns = @JoinColumn(name = "technician_id"),
			indexes = {
					@Index(name = "idx_technician_specialties_specialty", columnList = "specialty"),
					@Index(name = "idx_technician_specialties_technician_id", columnList = "technician_id")
			})
	@Enumerated(EnumType.STRING)
	@Column(name = "specialty", nullable = false)
	private Set<ApplianceSpecialty> specialties = new LinkedHashSet<>();

	@OneToMany(mappedBy = "technician", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AvailabilitySlot> availabilitySlots = new ArrayList<>();

	public Technician(String firstName, String lastName, String email, String phoneNumber) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phoneNumber = phoneNumber;
	}

	public Technician addServiceAreas(String... zipCodes) {
		this.serviceAreas.addAll(List.of(zipCodes));
		return this;
	}

	public Technician addSpecialties(ApplianceSpecialty... specialties) {
		this.specialties.addAll(List.of(specialties));
		return this;
	}

	public Technician addAvailabilitySlot(AvailabilitySlot slot) {
		slot.assignTechnician(this);
		this.availabilitySlots.add(slot);
		return this;
	}
}
