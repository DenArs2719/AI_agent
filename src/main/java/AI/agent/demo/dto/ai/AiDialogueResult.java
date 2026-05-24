package AI.agent.demo.dto.ai;

public record AiDialogueResult(String assistantMessage,
                               CallSessionUpdates updates,
                               boolean issueResolved,
                               boolean readyForScheduling,
                               boolean needsMoreTroubleshooting) {
	public AiDialogueResult(String assistantMessage, CallSessionUpdates updates) {
		this(assistantMessage, updates, false, false, false);
	}
}
