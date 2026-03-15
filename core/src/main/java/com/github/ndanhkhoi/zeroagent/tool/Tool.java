package com.github.ndanhkhoi.zeroagent.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface representing a tool that the AI agent can use to extend its capabilities.
 *
 * <p>Tools are functions that agents can call to perform actions or retrieve information.
 * Each tool is automatically converted to an OpenAI function schema and made available
 * to the LLM during conversation. When the LLM requests a tool call, the {@link ToolRouter}
 * dispatches the request to the appropriate {@link Tool} implementation.
 *
 * <h2>Implementing a Tool</h2>
 * Tools must define:
 * <ul>
 *   <li>A unique name (snake_case recommended)</li>
 *   <li>A description of what the tool does (helps LLM decide when to use it)</li>
 *   <li>A JSON Schema defining expected parameters</li>
 *   <li>Execution logic that returns a {@link ToolResult}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class WeatherTool implements Tool {
 *     private final WeatherApiClient apiClient;
 *
 *     public WeatherTool(String apiKey) {
 *         this.apiClient = new WeatherApiClient(apiKey);
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "get_weather";
 *     }
 *
 *     @Override
 *     public String description() {
 *         return "Get the current weather for a specific location";
 *     }
 *
 *     @Override
 *     public JsonNode parametersSchema() {
 *         return new ObjectMapper().createObjectNode()
 *             .put("type", "object")
 *             .set("properties", new ObjectMapper().createObjectNode()
 *                 .set("location", new ObjectMapper().createObjectNode()
 *                     .put("type", "string")
 *                     .put("description", "City name, e.g. San Francisco, CA"))
 *                 .set("unit", new ObjectMapper().createObjectNode()
 *                     .put("type", "string")
 *                     .put("enum", new String[]{"celsius", "fahrenheit"})
 *                     .put("description", "Temperature unit")))
 *             .set("required", new ObjectMapper().createArrayNode().add("location"));
 *     }
 *
 *     @Override
 *     public ToolResult execute(String argumentsJson) {
 *         try {
 *             JsonNode args = new ObjectMapper().readTree(argumentsJson);
 *             String location = args.get("location").asText();
 *             String unit = args.has("unit") ? args.get("unit").asText() : "celsius";
 *
 *             WeatherData weather = apiClient.getWeather(location, unit);
 *             return ToolResult.success(weather.toString());
 *         } catch (Exception e) {
 *             return ToolResult.error("Failed to get weather: " + e.getMessage());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * Tools should handle errors gracefully and return {@link ToolResult#error(String)} with
 * descriptive error messages. The agent will pass these errors back to the LLM, allowing
 * it to retry or adjust its approach.
 *
 * <h2>Thread Safety</h2>
 * Tool implementations must be thread-safe, as the same tool instance may be called
 * concurrently from different sessions. Use synchronization or immutable state where needed.
 *
 * @see ToolRouter
 * @see ToolResult
 */
public interface Tool {
    /**
     * Returns the unique name of this tool.
     *
     * <p>The name must be unique across all tools registered with the {@link ToolRouter}.
     * Use snake_case format (e.g., "get_weather", "calculate_fibonacci").
     *
     * @return the unique name of this tool
     */
    String name();

    /**
     * Returns a description of what this tool does.
     *
     * <p>The description is used by the LLM to decide when to call this tool. Be specific
     * about what the tool does, when to use it, and any important constraints or requirements.
     *
     * @return a description of what this tool does
     */
    String description();

    /**
     * Returns the JSON Schema defining the expected parameters for this tool.
     *
     * <p>The schema must be a valid JSON Schema draft-2020-12 or earlier. It should define:
     * <ul>
     *   <li>The type (usually "object")</li>
     *   <li>Properties with their types and descriptions</li>
     *   <li>Required parameters</li>
     *   <li>Constraints (enum, minimum/maximum, pattern)</li>
     * </ul>
     *
     * @return a {@link JsonNode} containing the JSON Schema for tool parameters
     * @see ToolRouter
     */
    JsonNode parametersSchema();

    /**
     * Executes this tool with the provided arguments.
     *
     * <p>This method is called when the LLM requests a tool call. The arguments are provided
     * as a JSON string matching the schema defined in {@link #parametersSchema()}.
     *
     * @param argumentsJson the JSON string containing the arguments passed from the LLM
     * @return the result of the tool execution
     */
    ToolResult execute(String argumentsJson);
}
