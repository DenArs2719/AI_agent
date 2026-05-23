package AI.agent.demo.service;

import AI.agent.demo.dto.TroubleshootingScript;
import AI.agent.demo.model.ApplianceSpecialty;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TroubleshootingScriptService {
	private final Map<ApplianceSpecialty, TroubleshootingScript> scripts;

	public TroubleshootingScriptService() {
		this.scripts = buildScripts();
	}

	public TroubleshootingScript getScript(ApplianceSpecialty applianceType) {
		TroubleshootingScript script = scripts.get(applianceType);
		if (script == null) {
			throw new IllegalArgumentException("No troubleshooting script configured for " + applianceType);
		}
		return script;
	}

	public List<TroubleshootingScript> getAllScripts() {
		return scripts.values().stream().toList();
	}

	private Map<ApplianceSpecialty, TroubleshootingScript> buildScripts() {
		Map<ApplianceSpecialty, TroubleshootingScript> safeChecks = new EnumMap<>(ApplianceSpecialty.class);
		safeChecks.put(ApplianceSpecialty.WASHER, new TroubleshootingScript(
				ApplianceSpecialty.WASHER,
				"Washer basic checks",
				List.of(
						"Confirm the washer is plugged in and the outlet has power.",
						"Check the breaker and reset it only if it has tripped.",
						"Make sure the lid or door is fully closed.",
						"Confirm hot and cold water supply valves are open.",
						"Check for an error code on the display and write it down.",
						"Check that the load is balanced and not blocking the door or lid.")));
		safeChecks.put(ApplianceSpecialty.DRYER, new TroubleshootingScript(
				ApplianceSpecialty.DRYER,
				"Dryer basic checks",
				List.of(
						"Confirm the dryer is plugged in and the outlet has power.",
						"Check the breaker and reset it only if it has tripped.",
						"Make sure the dryer door is fully closed.",
						"Clean the lint filter.",
						"Check for an error code on the display and write it down.",
						"Confirm the vent path is not visibly blocked.")));
		safeChecks.put(ApplianceSpecialty.REFRIGERATOR, new TroubleshootingScript(
				ApplianceSpecialty.REFRIGERATOR,
				"Refrigerator basic checks",
				List.of(
						"Confirm the refrigerator is plugged in and the outlet has power.",
						"Check the breaker and reset it only if it has tripped.",
						"Make sure refrigerator and freezer doors are fully closed.",
						"Check whether the temperature controls were changed recently.",
						"Confirm vents inside the refrigerator are not blocked by food.",
						"Check for an error code on the display and write it down.")));
		safeChecks.put(ApplianceSpecialty.DISHWASHER, new TroubleshootingScript(
				ApplianceSpecialty.DISHWASHER,
				"Dishwasher basic checks",
				List.of(
						"Confirm the dishwasher has power.",
						"Check the breaker and reset it only if it has tripped.",
						"Make sure the door is fully latched.",
						"Confirm the water supply valve is open.",
						"Clean the dishwasher filter if it is accessible without tools.",
						"Check for an error code on the display and write it down.")));
		safeChecks.put(ApplianceSpecialty.OVEN, new TroubleshootingScript(
				ApplianceSpecialty.OVEN,
				"Oven basic checks",
				List.of(
						"Confirm the oven has power.",
						"Check the breaker and reset it only if it has tripped.",
						"Make sure the oven door is fully closed.",
						"Check whether the control lock or delay start is enabled.",
						"Check for an error code on the display and write it down.",
						"Do not attempt to inspect gas lines or internal wiring.")));
		safeChecks.put(ApplianceSpecialty.HVAC, new TroubleshootingScript(
				ApplianceSpecialty.HVAC,
				"HVAC basic checks",
				List.of(
						"Confirm the thermostat is on and set to the desired mode.",
						"Check the breaker and reset it only if it has tripped.",
						"Replace or check the air filter if it is safely accessible.",
						"Confirm supply and return vents are open and not blocked.",
						"Check for an error code on the thermostat or unit display and write it down.",
						"Do not open panels or inspect wiring.")));
		return Map.copyOf(safeChecks);
	}
}
