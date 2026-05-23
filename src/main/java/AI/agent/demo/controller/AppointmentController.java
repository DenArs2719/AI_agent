package AI.agent.demo.controller;

import AI.agent.demo.dto.AppointmentResponse;
import AI.agent.demo.dto.CreateAppointmentRequest;
import AI.agent.demo.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppointmentController {
	private final AppointmentService appointmentService;

	@PostMapping("/appointments")
	public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody CreateAppointmentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(request));
	}

	@PostMapping("/appointments/{appointmentId}/confirm")
	public AppointmentResponse confirmAppointment(@PathVariable Long appointmentId) {
		return appointmentService.confirmAppointment(appointmentId);
	}
}
