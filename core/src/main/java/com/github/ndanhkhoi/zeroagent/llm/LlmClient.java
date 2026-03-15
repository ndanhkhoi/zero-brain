package com.github.ndanhkhoi.zeroagent.llm;

import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.stream.Stream;

/**
 * Abstraction over LLM providers for chat completions with streaming support.
 * <p>
 * This interface defines a contract for interacting with Large Language Model providers,
 * with a specific focus on streaming chat completions. It enables Zero Brain to work with
 * different LLM providers (OpenAI, Anthropic, Google Gemini, local models) while maintaining
 * a consistent API for agent orchestration.
 *
 * <h2>Streaming Protocol</h2>
 * The interface uses streaming to provide real-time access to LLM output, enabling:
 * <ul>
 *   <li>Token-by-token response generation for better UX</li>
 *   <li>Early detection of tool calls without waiting for complete response</li>
 *   <li>Real-time progress monitoring through lifecycle hooks</li>
 * </ul>
 *
 * <h2>Implementing for Other Providers</h2>
 * To add support for a different LLM provider:
 * <pre>{@code
 * public class AnthropicClient implements LlmClient {
 *     private final AnthropicApi api;
 *
 *     public AnthropicClient(String apiKey) {
 *         this.api = new AnthropicApi(apiKey);
 *     }
 *
 *     @Override
 *     public Stream<ChatCompletionChunk> chatStream(ChatCompletionCreateParams params) {
 *         // Convert OpenAI params to Anthropic format
 *         AnthropicRequest anthropicRequest = convertParams(params);
 *
 *         // Call Anthropic streaming API
 *         return api.streamCompletions(anthropicRequest)
 *             .map(this::convertToOpenAiChunk);
 *     }
 *
 *     private ChatCompletionChunk convertToOpenAiChunk(AnthropicChunk chunk) {
 *         // Convert Anthropic chunk to OpenAI format
 *         // This ensures compatibility with existing code
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Implementations of this interface must be thread-safe for concurrent use across
 * multiple sessions. The default {@link OpenAiChatClient} implementation is thread-safe.
 *
 * @see OpenAiChatClient
 * @see com.github.ndanhkhoi.zeroagent.agent.AgentLoop
 */
public interface LlmClient {
    /**
     * Executes a chat completion request in streaming mode.
     * <p>
     * This method initiates a streaming chat completion request and returns a stream
     * of chunks containing both text tokens and tool call deltas. The stream enables
     * real-time processing of LLM output without waiting for the complete response.
     * <p>
     * The stream may contain chunks with:
     * <ul>
     *   <li>Text content (partial words, sentences)</li>
     *   <li>Tool call names and arguments (streamed incrementally)</li>
     *   <li>Finish reasons indicating why generation stopped</li>
     * </ul>
     * <p>
     * Implementations must ensure the stream is properly closed and resources are released
     * when the stream is consumed or cancelled.
     *
     * @param params the request parameters conforming to the OpenAI Chat Completions API schema,
     *               including model, messages, temperature, tools, and other settings
     * @return a stream of {@link ChatCompletionChunk} objects containing text and tool-call deltas
     * @throws RuntimeException if the request fails or the stream cannot be established
     */
    Stream<ChatCompletionChunk> chatStream(ChatCompletionCreateParams params);
}
