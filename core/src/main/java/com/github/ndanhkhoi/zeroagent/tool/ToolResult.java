package com.github.ndanhkhoi.zeroagent.tool;

/**
 * Represents the result of a tool execution, supporting both success and error cases.
 * <p>
 * This record wraps the output of tool execution and indicates whether the execution
 * was successful. Errors are passed back to the LLM so it can retry or adjust its approach.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public ToolResult execute(String argumentsJson) {
 *     try {
 *         JsonNode args = new ObjectMapper().readTree(argumentsJson);
 *         String location = args.get("location").asText();
 *
 *         // Execute tool logic
 *         String weather = weatherApi.getWeather(location);
 *         return ToolResult.success(weather);
 *     } catch (Exception e) {
 *         // Return error so LLM can retry
 *         return ToolResult.error("Failed to get weather: " + e.getMessage());
 *     }
 * }
 * }</pre>
 *
 * @param output the text output of the tool, or error details if unsuccessful
 * @param success true if the tool execution was successful, false otherwise
 */
public record ToolResult(String output, boolean success) {

    /**
     * Creates a successful tool result with the specified output.
     *
     * @param output the successful output of the tool execution
     * @return a ToolResult indicating success
     */
    public static ToolResult success(String output) {
        return new ToolResult(output, true);
    }

    /**
     * Creates an error tool result with the specified error message.
     *
     * @param error the error message describing what went wrong
     * @return a ToolResult indicating failure
     */
    public static ToolResult error(String error) {
        return new ToolResult(error, false);
    }
}
