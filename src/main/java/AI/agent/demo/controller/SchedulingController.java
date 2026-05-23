package AI.agent.demo.controller;

import AI.agent.demo.dto.SchedulingMatchResponse;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.service.SchedulingService;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SchedulingController {
    private final SchedulingService schedulingService;

    @GetMapping("/scheduling/matches")
    public List<SchedulingMatchResponse> findMatches(@RequestParam String customerZipCode,
                                                     @RequestParam ApplianceSpecialty applianceType,
                                                     @RequestParam LocalDateTime desiredStart,
                                                     @RequestParam LocalDateTime desiredEnd) {
        return schedulingService.findMatches(customerZipCode, applianceType, desiredStart, desiredEnd);
    }
}
