package AI.agent.demo.service;

import AI.agent.demo.dto.ai.AiDialogueResult;
import AI.agent.demo.model.CallSession;

public interface AiDiagnosticService {
	AiDialogueResult nextTurn(CallSession session, String callerSpeech);
}
