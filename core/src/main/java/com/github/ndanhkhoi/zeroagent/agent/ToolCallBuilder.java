package com.github.ndanhkhoi.zeroagent.agent;

/**
 * A mutable builder for accumulating tool call data during streaming responses.
 *
 * <p>During LLM streaming, tool calls are delivered incrementally across multiple chunks.
 * This builder accumulates the partial data (id, name, arguments) as they arrive,
 * allowing reconstruction of complete tool calls after the stream finishes.
 *
 * <h2>Usage Flow</h2>
 * <ol>
 *   <li>Create instance via {@link java.util.Map#computeIfAbsent} using chunk index</li>
 *   <li>Populate {@code id} when present in chunk delta</li>
 *   <li>Populate {@code name} when present in chunk delta</li>
 *   <li>Append to {@code argumentsBuilder} as argument chunks arrive</li>
 *   <li>Build final {@link com.openai.models.chat.completions.ChatCompletionMessageToolCall} from accumulated data</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // First chunk with id and name
 * ToolCallBuilder tcb = new ToolCallBuilder();
 * tcb.id = "call_abc123";
 * tcb.name = "search";
 *
 * // Subsequent chunks with argument fragments
 * tcb.argumentsBuilder.append("{\"query\":");
 * tcb.argumentsBuilder.append("\"weather\"}");
 *
 * // Final result: {"query":"weather"}
 * }</pre>
 *
 * @see com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall
 * @see com.openai.models.chat.completions.ChatCompletionMessageToolCall
 */
class ToolCallBuilder {
    /** The tool call identifier, set when the first chunk arrives with an id. */
    String id;

    /** The function name to call, set when the first chunk arrives with a function name. */
    String name;

    /**
     * Accumulates the function arguments JSON string across multiple chunks.
     * Arguments are delivered fragment-by-fragment and must be concatenated.
     */
    StringBuilder argumentsBuilder = new StringBuilder();
}
