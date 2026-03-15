package com.github.ndanhkhoi.zeroagent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal registry and dispatcher for agent tools.
 *
 * <p>ToolRouter is responsible for:
 * <ul>
 *   <li>Maintaining a registry of available tools</li>
 *   <li>Converting tools to OpenAI function schemas for LLM consumption</li>
 *   <li>Dispatching LLM tool call requests to the appropriate Java implementations</li>
 *   <li>Providing access to converted tool schemas for API requests</li>
 * </ul>
 *
 * <p>This class bridges the gap between Java tool implementations and the OpenAI function calling
 * protocol. Each tool is converted to a function definition schema that the LLM can understand,
 * and incoming tool call requests are routed to the corresponding {@link Tool} implementations.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create tools
 * List<Tool> tools = List.of(
 *     new CurrentTimeTool(),
 *     new JavaScriptTool(),
 *     new WeatherTool()
 * );
 *
 * // Register tools with router
 * ToolRouter router = new ToolRouter(tools);
 *
 * // Get converted schemas for LLM
 * List<ChatCompletionTool> sdkTools = router.getSdkTools();
 *
 * // Dispatch tool call from LLM
 * ToolResult result = router.dispatch(
 *     "get_weather",
 *     '{"location": "San Francisco, CA", "unit": "celsius"}'
 * );
 *
 * if (result.success()) {
 *     System.out.println("Tool result: " + result.output());
 * } else {
 *     System.err.println("Tool error: " + result.output());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe. Tools can be dispatched concurrently from multiple sessions
 * without synchronization.
 *
 * @see Tool
 * @see ToolResult
 */
public class ToolRouter {
    private final Map<String, Tool> toolMap = new HashMap<>();
    private final List<ChatCompletionTool> sdkTools = new ArrayList<>();

    /**
     * Creates a ToolRouter with the specified tools.
     * <p>
     * All tools are immediately converted to OpenAI's expected format
     * during construction for efficient reuse.
     *
     * @param tools the list of tools to register
     * @return a new ToolRouter instance
     * @throws IllegalArgumentException if any tool's schema cannot be parsed
     */
    public static ToolRouter create(List<Tool> tools) {
        // Parse all tools first - this is the only place that can throw exceptions
        List<ParsedTool> parsedTools = new ArrayList<>();
        for (Tool tool : tools) {
            try {
                Object parameters = new ObjectMapper().treeToValue(tool.parametersSchema(), Object.class);

                FunctionDefinition functionDefinition = FunctionDefinition.builder()
                        .name(tool.name())
                        .description(tool.description())
                        .parameters(JsonValue.from(parameters))
                        .build();

                ChatCompletionFunctionTool functionTool = ChatCompletionFunctionTool.builder()
                        .function(functionDefinition)
                        .build();

                parsedTools.add(new ParsedTool(tool, ChatCompletionTool.ofFunction(functionTool)));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to parse schema for tool: " + tool.name(), e);
            }
        }
        return new ToolRouter(parsedTools);
    }

    /**
     * Private constructor. Use {@link #create(List)} factory method instead.
     * This constructor only stores pre-validated data and cannot throw exceptions.
     *
     * @param parsedTools the list of pre-parsed tools (validated and converted)
     */
    private ToolRouter(List<ParsedTool> parsedTools) {
        for (ParsedTool parsed : parsedTools) {
            toolMap.put(parsed.tool.name(), parsed.tool);
            sdkTools.add(parsed.sdkTool);
        }
    }

    /**
     * Internal holder for parsed tool data.
     * Used to pass pre-validated tools from factory method to constructor.
     */
    private record ParsedTool(Tool tool, ChatCompletionTool sdkTool) {}

    /**
     * Returns the list of tools in OpenAI's native SDK schema format.
     *
     * <p>The tools are converted once during construction and cached for efficiency.
     *
     * @return an unmodifiable list of tools converted to OpenAI's expected native SDK schema format
     */
    public List<ChatCompletionTool> getSdkTools() {
        return Collections.unmodifiableList(sdkTools);
    }

    /**
     * Routes an LLM function call request to the corresponding mapped Java tool.
     *
     * @param name The unique name of the invoked function.
     * @param argumentsJson The raw JSON parameter string generated by the LLM.
     * @return The execution payload wrapped in a ToolResult.
     */
    public ToolResult dispatch(String name, String argumentsJson) {
        Tool tool = toolMap.get(name);
        if (tool == null) {
            return ToolResult.error("Unknown tool: " + name);
        }
        return tool.execute(argumentsJson);
    }
}
