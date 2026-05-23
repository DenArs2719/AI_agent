package AI.agent.demo.dto.ai;

import AI.agent.demo.model.ApplianceSpecialty;

public record CallSessionUpdates(ApplianceSpecialty applianceType,
                                 String symptoms,
                                 String errorCodes,
                                 String priorTroubleshootingSteps,
                                 String zipCode,
                                 String customerName,
                                 String availability) {
    public static CallSessionUpdates empty() {
        return new CallSessionUpdates(null, null, null, null, null, null, null);
    }
}
