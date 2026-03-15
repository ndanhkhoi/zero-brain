package com.github.ndanhkhoi.zeroagent.memory;

import com.openai.models.chat.completions.ChatCompletionMessageParam;
import java.util.List;

/**
 * Persistence layer for managing conversational history with session-based isolation.
 * <p>
 * This interface defines the contract for storing and retrieving chat messages,
 * enabling agents to maintain context across multiple interactions. Implementations
 * can use various storage backends (in-memory, Redis, database, file-based) depending
 * on requirements for persistence, performance, and scalability.
 *
 * <h2>Session Isolation</h2>
 * Messages are stored and retrieved per session, ensuring that different conversations
 * remain isolated. Each session has its own message history that grows over time.
 *
 * <h2>Thread Safety</h2>
 * Implementations must be thread-safe for concurrent access across multiple sessions.
 * The default {@link InMemoryStore} is thread-safe.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Memory memory = new InMemoryStore();
 *
 * // Store messages
 * memory.addMessage("session-1", userMessage);
 * memory.addMessage("session-1", assistantMessage);
 *
 * // Retrieve history
 * List<ChatCompletionMessageParam> history = memory.getMessages("session-1");
 *
 * // Clear session (e.g., user requests fresh start)
 * memory.clear("session-1");
 * }</pre>
 *
 * @see InMemoryStore
 * @see com.github.ndanhkhoi.zeroagent.agent.AgentLoop
 */
public interface Memory {
    /**
     * Appends a new message to the conversation history for the specified session.
     * <p>
     * Messages are stored in chronological order and retrieved in the same order.
     * The message can be of any type: user, assistant, system, or tool.
     *
     * @param sessionId the unique conversation identifier
     * @param message the message to store (user, assistant, system, or tool message)
     */
    void addMessage(String sessionId, ChatCompletionMessageParam message);

    /**
     * Retrieves the complete conversation history for the specified session.
     * <p>
     * Returns all messages in chronological order, including user messages,
     * assistant responses, system prompts, and tool calls/results. This history
     * provides the context needed for the agent to continue the conversation.
     *
     * @param sessionId the unique conversation identifier
     * @return the chronological list of messages for this session, or an empty list
     *         if the session has no history
     */
    List<ChatCompletionMessageParam> getMessages(String sessionId);

    /**
     * Permanently deletes all conversation history for the specified session.
     * <p>
     * This is useful when a user requests a fresh start or when a session expires.
     * After clearing, {@link #getMessages(String)} will return an empty list for this session.
     *
     * @param sessionId the unique conversation identifier to clear
     */
    void clear(String sessionId);
}
