package org.dromara.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j declarative Agent contract.
 */
public interface AgentAssistant {

    /**
     * Sends a message within an independent conversation memory.
     */
    String chat(@MemoryId Long sessionId, @UserMessage String message);

}
