package com.github.ndanhkhoi.zeroagent.agent;

import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Lifecycle hooks for intercepting and observing agent execution events.
 * <p>
 * This class provides a callback mechanism for monitoring and reacting to events
 * during agent execution, including token streaming, tool calls, errors, and completion.
 * Hooks are useful for:
 * <ul>
 *   <li>Streaming responses to users in real-time</li>
 *   <li>Logging and monitoring agent behavior</li>
 *   <li>Debugging tool calls and responses</li>
 *   <li>Custom error handling and retry logic</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AgentHooks hooks = new AgentHooks();
 *
 * // Stream tokens to user
 * hooks.setOnToken(token -> System.out.print(token));
 *
 * // Log tool calls
 * hooks.setOnToolCall((name, args) -> logger.info("Calling tool: {} with {}", name, args));
 *
 * // Handle errors
 * hooks.setOnOnError(error -> logger.error("Agent error", error));
 *
 * // Notify on completion
 * hooks.setOnComplete(() -> logger.info("Agent execution complete"));
 * }</pre>
 *
 * @see AgentLoop
 */
public class AgentHooks {
    /**
     * Creates a new AgentHooks instance with default no-op callbacks.
     *
     * <p>All hooks are initialized with no-op implementations:
     * <ul>
     *   <li>{@code onThinking} - does nothing</li>
     *   <li>{@code onToolCall} - does nothing</li>
     *   <li>{@code onToolResult} - does nothing</li>
     *   <li>{@code onToken} - does nothing</li>
     *   <li>{@code onComplete} - does nothing</li>
     *   <li>{@code onError} - does nothing</li>
     * </ul>
     *
     * <p>Use setter methods to customize specific hooks:
     * <pre>{@code
     * AgentHooks hooks = new AgentHooks();
     * hooks.setOnToken(token -> System.out.print(token));
     * hooks.setOnComplete(() -> System.out.println("Done"));
     * }</pre>
     */
    public AgentHooks() {}

    private Runnable onThinking = () -> {};
    private BiConsumer<String, String> onToolCall = (name, args) -> {};
    private BiConsumer<String, ToolResult> onToolResult = (name, res) -> {};
    private Consumer<String> onToken = t -> {};
    private Runnable onComplete = () -> {};
    private Consumer<Throwable> onError = e -> {};

    /**
     * Sets the callback to invoke when the agent starts thinking (before LLM call).
     *
     * @param onThinking the callback to invoke when agent starts thinking, or null to reset
     */
    public void setOnThinking(Runnable onThinking) {
        if (onThinking != null) {
            this.onThinking = onThinking;
        }
    }

    /**
     * Sets the callback to invoke when a tool is called.
     *
     * @param onToolCall the callback receiving tool name and arguments, or null to reset
     */
    public void setOnToolCall(BiConsumer<String, String> onToolCall) {
        if (onToolCall != null) {
            this.onToolCall = onToolCall;
        }
    }

    /**
     * Sets the callback to invoke when a tool result is received.
     *
     * @param onToolResult the callback receiving tool name and result, or null to reset
     */
    public void setOnToolResult(BiConsumer<String, ToolResult> onToolResult) {
        if (onToolResult != null) {
            this.onToolResult = onToolResult;
        }
    }

    /**
     * Sets the callback to invoke for each streamed token.
     *
     * @param onToken the callback receiving each token, or null to reset
     */
    public void setOnToken(Consumer<String> onToken) {
        if (onToken != null) {
            this.onToken = onToken;
        }
    }

    /**
     * Sets the callback to invoke when agent execution completes successfully.
     *
     * @param onComplete the callback to invoke on completion, or null to reset
     */
    public void setOnComplete(Runnable onComplete) {
        if (onComplete != null) {
            this.onComplete = onComplete;
        }
    }

    /**
     * Sets the callback to invoke when an error occurs during agent execution.
     *
     * @param onError the callback receiving the error, or null to reset
     */
    public void setOnError(Consumer<Throwable> onError) {
        if (onError != null) {
            this.onError = onError;
        }
    }

    /**
     * Returns the callback for when the agent starts thinking.
     *
     * @return the thinking callback, never null
     */
    public Runnable getOnThinking() {
        return onThinking;
    }

    /**
     * Returns the callback for when a tool is called.
     *
     * @return the tool call callback receiving tool name and arguments, never null
     */
    public BiConsumer<String, String> getOnToolCall() {
        return onToolCall;
    }

    /**
     * Returns the callback for when a tool result is received.
     *
     * @return the tool result callback receiving tool name and result, never null
     */
    public BiConsumer<String, ToolResult> getOnToolResult() {
        return onToolResult;
    }

    /**
     * Returns the callback for streamed tokens.
     *
     * @return the token callback, never null
     */
    public Consumer<String> getOnToken() {
        return onToken;
    }

    /**
     * Returns the callback for successful completion.
     *
     * @return the completion callback, never null
     */
    public Runnable getOnComplete() {
        return onComplete;
    }

    /**
     * Returns the callback for error handling.
     *
     * @return the error callback, never null
     */
    public Consumer<Throwable> getOnError() {
        return onError;
    }
}
