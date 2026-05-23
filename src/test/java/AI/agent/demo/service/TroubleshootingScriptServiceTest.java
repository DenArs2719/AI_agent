package AI.agent.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import AI.agent.demo.dto.TroubleshootingScript;
import AI.agent.demo.model.ApplianceSpecialty;
import org.junit.jupiter.api.Test;

class TroubleshootingScriptServiceTest {

	private final TroubleshootingScriptService troubleshootingScriptService = new TroubleshootingScriptService();

	@Test
	void getAllScriptsIncludesRequiredApplianceTypes() {
		assertThat(troubleshootingScriptService.getAllScripts())
				.extracting("applianceType")
				.containsExactlyInAnyOrder(
						ApplianceSpecialty.WASHER,
						ApplianceSpecialty.DRYER,
						ApplianceSpecialty.REFRIGERATOR,
						ApplianceSpecialty.DISHWASHER,
						ApplianceSpecialty.OVEN,
						ApplianceSpecialty.HVAC);
	}

	@Test
	void getScriptReturnsSafeBasicChecksOnly() {
		TroubleshootingScript script = troubleshootingScriptService.getScript(ApplianceSpecialty.DISHWASHER);

		assertThat(script.safeChecks()).hasSizeGreaterThanOrEqualTo(5);
		assertThat(script.safeChecks()).anyMatch(check -> check.contains("door"));
		assertThat(script.safeChecks()).anyMatch(check -> check.contains("water supply"));
		assertThat(script.safeChecks()).anyMatch(check -> check.contains("error code"));
		assertThat(script.safeChecks()).noneMatch(check -> check.toLowerCase().contains("remove a panel"));
		assertThat(script.safeChecks()).noneMatch(check -> check.toLowerCase().contains("repair wiring"));
	}

	@Test
	void getScriptRejectsUnsupportedApplianceType() {
		assertThatThrownBy(() -> troubleshootingScriptService.getScript(ApplianceSpecialty.MICROWAVE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("No troubleshooting script configured for MICROWAVE");
	}
}
