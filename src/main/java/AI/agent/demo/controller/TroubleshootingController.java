package AI.agent.demo.controller;

import AI.agent.demo.dto.TroubleshootingScript;
import AI.agent.demo.model.ApplianceSpecialty;
import AI.agent.demo.service.TroubleshootingScriptService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/troubleshooting")
@RequiredArgsConstructor
public class TroubleshootingController {

	private final TroubleshootingScriptService troubleshootingScriptService;

	@GetMapping
	public List<TroubleshootingScript> getAllScripts() {
		return troubleshootingScriptService.getAllScripts();
	}

	@GetMapping("/{applianceType}")
	public TroubleshootingScript getScript(@PathVariable ApplianceSpecialty applianceType) {
		return troubleshootingScriptService.getScript(applianceType);
	}
}
