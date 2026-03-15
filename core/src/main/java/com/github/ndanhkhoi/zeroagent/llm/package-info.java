/**
 * LLM client abstractions and OpenAI integration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides abstraction layers for LLM (Large Language Model) interactions, enabling
 * Zero Brain to work with different LLM providers while maintaining a consistent API. The default
 * implementation supports OpenAI's Chat Completions API.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.llm.LlmClient} - Core interface for LLM interactions.
 *       Implement this interface to add support for other LLM providers.</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.llm.OpenAiChatClient} - Default implementation using
 *       the official OpenAI Java SDK for chat completions</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an OpenAI client
 * String apiKey = System.getenv("OPENAI_API_KEY");
 * LlmClient llmClient = new OpenAiChatClient(apiKey);
 *
 * // Make a simple chat request
 * ChatMessage message = new ChatMessage(
 *     ChatRole.USER,
 *     "Hello, how are you?"
 * );
 * ChatResponse response = llmClient.chat(
 *     "gpt-4o-mini",
 *     List.of(message),
 *     0.7  // temperature
 * );
 *
 * System.out.println(response.content());
 * }</pre>
 *
 * <h2>Advanced Usage</h2>
 * <pre>{@code
 * // Chat with tools (function calling)
 * List<ChatCompletionTool> tools = List.of(
 *     ChatCompletionTool.ofFunction(
 *         FunctionDefinition.builder()
 *             .name("get_weather")
 *             .description("Get weather for a location")
 *             .parameters(JsonValue.from(paramsSchema))
 *             .build()
 *     )
 * );
 *
 * ChatResponse response = llmClient.chat(
 *     model,
 *     messages,
 *     temperature,
 *     tools
 * );
 *
 * // Handle tool calls
 * if (response.requiresToolCall()) {
 *     List<ChatCompletionToolCall> toolCalls = response.toolCalls();
 *     for (ChatCompletionToolCall toolCall : toolCalls) {
 *         // Execute tool and get result
 *         String result = executeTool(toolCall);
 *         // Send result back to LLM
 *     }
 * }
 * }</pre>
 *
 * <h2>Supporting Other LLM Providers</h2>
 * <p>
 * To add support for other LLM providers (e.g., Anthropic, Google Gemini, local models):
 * </p>
 * <pre>{@code
 * public class AnthropicClient implements LlmClient {
 *     private final String apiKey;
 *
 *     public AnthropicClient(String apiKey) {
 *         this.apiKey = apiKey;
 *     }
 *
 *     @Override
 *     public ChatResponse chat(String model, List<ChatMessage> messages, double temperature) {
 *         // Call Anthropic API
 *         // Convert response to ChatResponse
 *     }
 *
 *     @Override
 *     public ChatResponse chat(String model, List<ChatMessage> messages, double temperature,
 *                             List<ChatCompletionTool> tools) {
 *         // Handle tool calling
 *     }
 * }
 * }</pre>
 *
 * <h2>Streaming Responses</h2>
 * <p>
 * For streaming responses (token-by-token generation), use the {@link com.github.ndanhkhoi.zeroagent.agent.AgentHooks}
 * callback mechanism with {@link com.github.ndanhkhoi.zeroagent.agent.AgentHooks#setOnToken(Consumer)}.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * LLM client implementations should handle network errors, rate limits, and API errors gracefully.
 * Consider implementing retry logic with exponential backoff for transient failures.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link com.github.ndanhkhoi.zeroagent.llm.LlmClient} implementations should be thread-safe for
 * concurrent use. The default {@link com.github.ndanhkhoi.zeroagent.llm.OpenAiChatClient} is thread-safe.
 * </p>
 *
 * @see com.github.ndanhkhoi.zeroagent.agent
 */
package com.github.ndanhkhoi.zeroagent.llm;
