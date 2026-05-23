package AI.agent.demo.dto;

import AI.agent.demo.model.ApplianceSpecialty;
import java.util.List;

public record TroubleshootingScript(
		ApplianceSpecialty applianceType,
		String title,
		List<String> safeChecks) {
}
