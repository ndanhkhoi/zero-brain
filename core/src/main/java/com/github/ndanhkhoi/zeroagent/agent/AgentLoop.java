package com.github.ndanhkhoi.zeroagent.agent;

import com.github.ndanhkhoi.zeroagent.concurrent.SessionLockManager;
import com.github.ndanhkhoi.zeroagent.llm.LlmClient;
import com.github.ndanhkhoi.zeroagent.memory.Memory;
import com.github.ndanhkhoi.zeroagent.skill.Skill;
import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import com.github.ndanhkhoi.zeroagent.tool.ToolRouter;
import com.openai.models.chat.completions.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The core orchestrator that manages the multi-turn conversational agent loop.
 *
 * <p>AgentLoop coordinates the complete reasoning process, including streaming responses from the LLM,
 * managing conversation context and memory, executing tool calls, and dispatching lifecycle events
 * to registered hooks. It supports both text-only and multimodal (vision) inputs, handles iterative
 * tool calling, and maintains conversation history across sessions.
 *
 * <h2>Core Responsibilities</h2>
 * <ul>
 *   <li>Stream responses from LLM token-by-token</li>
 *   <li>Execute tools when requested by LLM</li>
 *   <li>Maintain conversation history in memory</li>
 *   <li>Dispatch lifecycle events (thinking, tool calls, completion)</li>
 *   <li>Enforce session-based locking for thread safety</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create dependencies
 * LlmClient llmClient = new OpenAiChatClient(apiKey);
 * List<Tool> tools = List.of(new CurrentTimeTool(), new JavaScriptTool());
 * ToolRouter toolRouter = new ToolRouter(tools);
 * Memory memory = new InMemoryStore();
 * AgentConfig config = new AgentConfig("gpt-4o-mini", 0.7, 10, "You are a helpful assistant");
 * AgentHooks hooks = new AgentHooks();
 * SessionLockManager lockManager = new PerSessionLockManager();
 *
 * // Create agent loop
 * AgentLoop loop = new AgentLoop(
 *     llmClient, toolRouter, memory, config, hooks, lockManager
 * );
 *
 * // Run agent
 * AgentResponse response = loop.chat("session-123", "What time is it?");
 * System.out.println(response.answer());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe when used with a {@link SessionLockManager}. Multiple threads can
 * safely execute agents for different sessions concurrently, while messages for the same session
 * are processed sequentially through session-based locking.
 *
 * <h2>Iteration Limits</h2>
 * The agent will execute up to {@link AgentConfig#maxIterations()} iterations. Each iteration consists
 * of one LLM call and any tool executions it requests. If the limit is reached, a
 * {@link RuntimeException} is thrown.
 *
 * @see AgentConfig
 * @see AgentHooks
 * @see AgentResponse
 * @see com.github.ndanhkhoi.zeroagent.tool.Tool
 * @see com.github.ndanhkhoi.zeroagent.concurrent.SessionLockManager
 */
public class AgentLoop {
    private static final String DATA_URL_FORMAT = "data:%s;base64,%s";
    private static final String ERROR_PREFIX = "Error: ";

    private final LlmClient llmClient;
    private final ToolRouter toolRouter;
    private final Memory memory;
    private final List<Skill> skills;
    private final AgentConfig config;
    private final AgentHooks hooks;
    private final SessionLockManager lockManager;

    /**
     * Creates a new AgentLoop with all required dependencies.
     *
     * @param llmClient the LLM client for making chat completion requests
     * @param toolRouter the tool router for dispatching tool calls to Java implementations
     * @param memory the memory store for maintaining conversation history
     * @param skills the list of available skills for the agent to use
     * @param config the agent configuration (model, temperature, max iterations, system prompt)
     * @param hooks the lifecycle hooks for monitoring agent execution events
     * @param lockManager the session lock manager for ensuring sequential processing per session
     */
    public AgentLoop(
            LlmClient llmClient,
            ToolRouter toolRouter,
            Memory memory,
            List<Skill> skills,
            AgentConfig config,
            AgentHooks hooks,
            SessionLockManager lockManager) {
        this.llmClient = llmClient;
        this.toolRouter = toolRouter;
        this.memory = memory;
        this.skills = skills;
        this.config = config;
        this.hooks = hooks;
        this.lockManager = lockManager;
    }

    /**
     * Executes the conversational loop with a text-only prompt.
     * <p>
     * This method acquires a lock for the session, adds the user message to memory,
     * and begins the iterative reasoning process. The loop continues until the LLM
     * stops requesting tool calls or the maximum iteration count is reached.
     *
     * @param sessionId a unique identifier mapping the conversation to memory state
     * @param userMessageText the text prompt from the user
     * @return the final accumulated response after the LLM completes its thought process
     * @throws RuntimeException if max iterations is reached or an error occurs during streaming
     * @see #chat(String, String, List)
     */
    public AgentResponse chat(String sessionId, String userMessageText) {
        return chat(sessionId, userMessageText, null);
    }

    /**
     * Executes the conversational loop with text and image inputs (multimodal/vision support).
     * <p>
     * Images are encoded as base64 and included in the message content using OpenAI's vision format.
     * Supported MIME types include: image/png, image/jpeg, image/gif, image/webp.
     * <p>
     * All images are provided to the LLM alongside the text prompt, enabling tasks like
     * image analysis, chart interpretation, document OCR, and visual reasoning.
     *
     * @param sessionId a unique identifier mapping the conversation to memory state
     * @param userMessageText the text prompt from the user
     * @param images the list of visual files with supported MIME types (png, jpeg, gif, webp)
     * @return the final accumulated response after the LLM completes its thought process
     * @throws RuntimeException if max iterations is reached, an error occurs during streaming,
     *         or an image fails to read
     * @see ImageInput
     */
    public AgentResponse chat(String sessionId, String userMessageText, List<ImageInput> images) {
        lockManager.lock(sessionId);
        try {
            ChatCompletionUserMessageParam userMsg = buildUserMessage(userMessageText, images);
            memory.addMessage(sessionId, ChatCompletionMessageParam.ofUser(userMsg));

            List<ChatCompletionMessageParam> messages = buildMessages(sessionId);
            return executeAgentLoop(sessionId, messages);
        } finally {
            lockManager.unlock(sessionId);
        }
    }

    /**
     * Builds a user message from text and optional image inputs.
     *
     * @param text the text content of the message
     * @param images optional list of images to include
     * @return the constructed user message parameter
     */
    private ChatCompletionUserMessageParam buildUserMessage(String text, List<ImageInput> images) {
        if (images == null || images.isEmpty()) {
            return buildTextOnlyMessage(text);
        }
        return buildMultimodalMessage(text, images);
    }

    /**
     * Builds a text-only user message.
     *
     * @param text the text content
     * @return a simple text message
     */
    private ChatCompletionUserMessageParam buildTextOnlyMessage(String text) {
        return ChatCompletionUserMessageParam.builder().content(text).build();
    }

    /**
     * Builds a multimodal message with text and images.
     * Images are encoded as base64 data URLs.
     *
     * @param text the text content
     * @param images the list of images to include
     * @return a multimodal message with text and image parts
     */
    private ChatCompletionUserMessageParam buildMultimodalMessage(String text, List<ImageInput> images) {
        List<ChatCompletionContentPart> parts = new ArrayList<>();
        parts.add(createTextPart(text));
        parts.addAll(createImageParts(images));

        return ChatCompletionUserMessageParam.builder()
                .content(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(parts))
                .build();
    }

    /**
     * Creates a text content part.
     *
     * @param text the text content
     * @return a text content part
     */
    private static ChatCompletionContentPart createTextPart(String text) {
        return ChatCompletionContentPart.ofText(
                ChatCompletionContentPartText.builder().text(text).build());
    }

    /**
     * Creates image content parts from image inputs.
     * Images are read and encoded as base64 data URLs.
     *
     * @param images the list of images
     * @return list of image content parts
     * @throws RuntimeException if an image fails to read
     */
    private List<ChatCompletionContentPart> createImageParts(List<ImageInput> images) {
        List<ChatCompletionContentPart> parts = new ArrayList<>();
        for (ImageInput img : images) {
            String base64 = encodeImageToBase64(img);
            String dataUrl = DATA_URL_FORMAT.formatted(img.mimeType(), base64);
            parts.add(createImagePart(dataUrl));
        }
        return parts;
    }

    /**
     * Encodes an image to base64 string.
     *
     * @param img the image input
     * @return base64 encoded image data
     * @throws RuntimeException if image stream fails to read
     */
    private static String encodeImageToBase64(ImageInput img) {
        try (var imageStream = img.stream()) {
            return Base64.getEncoder().encodeToString(imageStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image stream", e);
        }
    }

    /**
     * Creates an image content part from a data URL.
     *
     * @param dataUrl the base64 data URL
     * @return an image content part
     */
    private static ChatCompletionContentPart createImagePart(String dataUrl) {
        return ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
                .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                        .url(dataUrl)
                        .build())
                .build());
    }

    /**
     * Builds the complete message list including system prompt and conversation history.
     *
     * @param sessionId the session identifier
     * @return list of message parameters
     */
    private List<ChatCompletionMessageParam> buildMessages(String sessionId) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(buildSystemPrompt()));
        messages.addAll(memory.getMessages(sessionId));
        return messages;
    }

    /**
     * Executes the main agent loop, iterating through LLM responses and tool calls.
     *
     * @param sessionId the session identifier
     * @param messages the message history
     * @return the final agent response
     */
    private AgentResponse executeAgentLoop(String sessionId, List<ChatCompletionMessageParam> messages) {
        int toolCallsExecuted = 0;
        int iterations = 0;

        while (iterations < config.maxIterations()) {
            iterations++;
            hooks.getOnThinking().run();

            ChatCompletionCreateParams params = buildCompletionParams(messages);
            StreamResult streamResult = processLlmStream(params);

            List<ChatCompletionMessageToolCall> executedToolCalls = buildToolCalls(streamResult);
            ChatCompletionAssistantMessageParam assistantMsg =
                    buildAssistantMessage(streamResult.content(), executedToolCalls);

            messages.add(ChatCompletionMessageParam.ofAssistant(assistantMsg));
            memory.addMessage(sessionId, ChatCompletionMessageParam.ofAssistant(assistantMsg));

            if (executedToolCalls.isEmpty()) {
                hooks.getOnComplete().run();
                return new AgentResponse(streamResult.content(), toolCallsExecuted, iterations);
            }

            toolCallsExecuted += executeToolCalls(sessionId, messages, executedToolCalls);
        }

        RuntimeException ex = new RuntimeException("Max iterations reached (" + config.maxIterations() + ")");
        hooks.getOnError().accept(ex);
        throw ex;
    }

    /**
     * Builds chat completion parameters from configuration.
     *
     * @param messages the message history
     * @return configured completion parameters
     */
    private ChatCompletionCreateParams buildCompletionParams(List<ChatCompletionMessageParam> messages) {
        ChatCompletionCreateParams.Builder paramsBuilder =
                ChatCompletionCreateParams.builder().model(config.model()).messages(messages);

        if (config.temperature() != null) {
            paramsBuilder.temperature(config.temperature());
        }

        if (!toolRouter.getSdkTools().isEmpty()) {
            paramsBuilder.tools(toolRouter.getSdkTools());
        }

        return paramsBuilder.build();
    }

    /**
     * Processes the LLM response stream and extracts content and tool calls.
     *
     * @param params the completion parameters
     * @return stream result with content and tool call builders
     */
    private StreamResult processLlmStream(ChatCompletionCreateParams params) {
        StringBuilder textBuilder = new StringBuilder();
        Map<Integer, ToolCallBuilder> toolCallsBuilderMap = new HashMap<>();

        try (Stream<ChatCompletionChunk> stream = llmClient.chatStream(params)) {
            stream.forEach(chunk -> processChunk(chunk, textBuilder, toolCallsBuilderMap));
        } catch (Exception e) {
            hooks.getOnError().accept(e);
            throw new RuntimeException("Error during LLM stream", e);
        }

        return new StreamResult(textBuilder.toString(), toolCallsBuilderMap);
    }

    /**
     * Processes a single chunk from the LLM stream.
     *
     * @param chunk the chunk to process
     * @param textBuilder accumulates text content
     * @param toolCallsBuilderMap accumulates tool call data
     */
    private void processChunk(
            ChatCompletionChunk chunk, StringBuilder textBuilder, Map<Integer, ToolCallBuilder> toolCallsBuilderMap) {
        var choice = chunk.choices().get(0);
        var delta = choice.delta();

        delta.content().ifPresent(text -> {
            textBuilder.append(text);
            hooks.getOnToken().accept(text);
        });

        delta.toolCalls().ifPresent(toolCalls -> processToolCalls(toolCalls, toolCallsBuilderMap));
    }

    /**
     * Processes tool calls from a chunk delta.
     *
     * @param toolCalls the list of tool call deltas
     * @param toolCallsBuilderMap the map accumulating tool call data
     */
    private static void processToolCalls(
            List<ChatCompletionChunk.Choice.Delta.ToolCall> toolCalls,
            Map<Integer, ToolCallBuilder> toolCallsBuilderMap) {
        for (ChatCompletionChunk.Choice.Delta.ToolCall tc : toolCalls) {
            int index = (int) tc.index();
            ToolCallBuilder tcb = toolCallsBuilderMap.computeIfAbsent(index, k -> new ToolCallBuilder());

            tc.id().ifPresent(id -> tcb.id = id);
            tc.function().ifPresent(fn -> {
                fn.name().ifPresent(name -> tcb.name = name);
                fn.arguments().ifPresent(args -> tcb.argumentsBuilder.append(args));
            });
        }
    }

    /**
     * Builds completed tool calls from the accumulated builder data.
     *
     * @param streamResult the stream result containing tool call builders
     * @return list of completed tool calls
     */
    private static List<ChatCompletionMessageToolCall> buildToolCalls(StreamResult streamResult) {
        List<ChatCompletionMessageToolCall> executedToolCalls = new ArrayList<>();
        for (ToolCallBuilder tcb : streamResult.toolCalls().values()) {
            ChatCompletionMessageToolCall toolCall =
                    ChatCompletionMessageToolCall.ofFunction(ChatCompletionMessageFunctionToolCall.builder()
                            .id(tcb.id)
                            .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                                    .name(tcb.name)
                                    .arguments(tcb.argumentsBuilder.toString())
                                    .build())
                            .build());
            executedToolCalls.add(toolCall);
        }
        return executedToolCalls;
    }

    /**
     * Builds an assistant message from content and tool calls.
     *
     * @param content the text content
     * @param toolCalls the list of tool calls
     * @return the assistant message parameter
     */
    private static ChatCompletionAssistantMessageParam buildAssistantMessage(
            String content, List<ChatCompletionMessageToolCall> toolCalls) {
        ChatCompletionAssistantMessageParam.Builder builder = ChatCompletionAssistantMessageParam.builder();
        if (!content.isEmpty()) {
            builder.content(content);
        }
        if (!toolCalls.isEmpty()) {
            builder.toolCalls(toolCalls);
        }
        return builder.build();
    }

    /**
     * Executes tool calls and adds their results to the message history.
     *
     * @param sessionId the session identifier
     * @param messages the message history to append results to
     * @param toolCalls the tool calls to execute
     * @return count of executed tool calls
     */
    private int executeToolCalls(
            String sessionId,
            List<ChatCompletionMessageParam> messages,
            List<ChatCompletionMessageToolCall> toolCalls) {
        int executedCount = 0;
        for (ChatCompletionMessageToolCall tc : toolCalls) {
            if (!tc.isFunction()) {
                continue;
            }
            String toolName = tc.asFunction().function().name();
            String args = tc.asFunction().function().arguments();

            hooks.getOnToolCall().accept(toolName, args);
            ToolResult result = toolRouter.dispatch(toolName, args);
            hooks.getOnToolResult().accept(toolName, result);

            String resultStr = formatToolResult(result);
            ChatCompletionToolMessageParam toolMsg = ChatCompletionToolMessageParam.builder()
                    .toolCallId(tc.asFunction().id())
                    .content(resultStr)
                    .build();

            messages.add(ChatCompletionMessageParam.ofTool(toolMsg));
            memory.addMessage(sessionId, ChatCompletionMessageParam.ofTool(toolMsg));
            executedCount++;
        }
        return executedCount;
    }

    /**
     * Formats a tool result, prefixing error messages.
     *
     * @param result the tool result
     * @return formatted result string
     */
    private static String formatToolResult(ToolResult result) {
        return result.success() ? result.output() : ERROR_PREFIX + result.output();
    }

    private ChatCompletionSystemMessageParam buildSystemPrompt() {
        String prompt = config.systemPrompt();

        if (!skills.isEmpty()) {
            prompt +=
                    """

                    <available_skills>
                      <instructions>You have access to the following skills. Use the `load_skills` tool to read their full instructions when needed.</instructions>
                    %s
                    </available_skills>
                    """
                            .formatted(buildSkillsList());
        }

        return ChatCompletionSystemMessageParam.builder().content(prompt).build();
    }

    /**
     * Builds the XML list of available skills.
     *
     * @return XML formatted list of skills
     */
    private String buildSkillsList() {
        StringBuilder skillsList = new StringBuilder();
        for (Skill skill : skills) {
            skillsList.append(
                    """
                      <skill>
                        <name>%s</name>
                        <description>%s</description>
                      </skill>
                    """
                            .formatted(escapeXml(skill.name()), escapeXml(skill.description())));
        }
        return skillsList.toString();
    }

    /**
     * Escapes special XML characters to prevent XML injection.
     *
     * @param text the text to escape
     * @return the escaped text with XML special characters replaced
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
