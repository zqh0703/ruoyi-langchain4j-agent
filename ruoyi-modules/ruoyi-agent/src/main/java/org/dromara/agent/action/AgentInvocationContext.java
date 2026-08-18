package org.dromara.agent.action;

import org.springframework.stereotype.Component;

/**
 * Conversation identifiers shared by tool recording and action preparation.
 */
@Component
public class AgentInvocationContext {

    private final ThreadLocal<Context> holder = new ThreadLocal<>();

    public Scope open(Long sessionId, Long agentId, Long runLogId) {
        Context previous = holder.get();
        holder.set(new Context(sessionId, agentId, runLogId));
        return () -> {
            if (previous == null) {
                holder.remove();
            } else {
                holder.set(previous);
            }
        };
    }

    public Context require() {
        Context context = holder.get();
        if (context == null) {
            throw new AgentActionException("MISSING_INVOCATION_CONTEXT", "Write actions require an Agent conversation context");
        }
        return context;
    }

    public Context current() {
        return holder.get();
    }

    public record Context(Long sessionId, Long agentId, Long runLogId) {
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
