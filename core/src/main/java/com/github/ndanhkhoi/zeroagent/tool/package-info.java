/**
 * Tool routing and execution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the tool abstraction layer that enables agents to extend their capabilities
 * by calling external functions. Tools are automatically converted to OpenAI function schemas and
 * dispatched when the LLM requests a tool invocation.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.Tool} - Core interface for defining tools that
 *       agents can call. Implement this interface to add custom capabilities.</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.ToolRouter} - Internal registry and dispatcher
 *       that converts tools to OpenAI function schemas and routes LLM function calls to Java implementations</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.ToolResult} - Wrapper for tool execution results,
 *   supporting both success and error cases</li>
 * </ul>
 *
 * <h2>Creating Custom Tools</h2>
 * <pre>{@code
 * public class WeatherTool implements Tool {
 *     @Override
 *     public String name() {
 *         return "get_weather";
 *     }
 *
 *     @Override
 *     public String description() {
 *         return "Get the current weather for a location";
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
 *             .set("required", new ObjectMapper().createArrayNode().add("location")));
 *     }
 *
 *     @Override
 *     public ToolResult execute(String argumentsJson) {
 *         // Parse arguments and call weather API
 *         // Return ToolResult.success(...) or ToolResult.error(...)
 *     }
 * }
 * }</pre>
 *
 * <h2>Built-in Tools</h2>
 * <p>
 * Zero Brain includes several built-in tools in the
 * {@link com.github.ndanhkhoi.zeroagent.tool.builtin} package:
 * </p>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.builtin.CurrentTimeTool} - Get current date/time</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.builtin.JavaScriptTool} - Execute JavaScript code</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.builtin.LoadSkillsTool} - Load skills from YAML files</li>
 * </ul>
 *
 * <h2>Tool Execution Flow</h2>
 * <ol>
 *   <li>Agent registers tools with {@link com.github.ndanhkhoi.zeroagent.tool.ToolRouter}</li>
 *   <li>ToolRouter converts tools to OpenAI function schemas</li>
 *   <li>LLM requests function call with tool name and arguments</li>
 *   <li>ToolRouter dispatches call to appropriate {@link com.github.ndanhkhoi.zeroagent.tool.Tool} implementation</li>
 *   <li>Tool executes and returns {@link com.github.ndanhkhoi.zeroagent.tool.ToolResult}</li>
 *   <li>Result is fed back to LLM for response generation</li>
 * </ol>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Tools should handle errors gracefully and return {@link com.github.ndanhkhoi.zeroagent.tool.ToolResult#error(String)}
 * with a descriptive error message. The agent will pass these error messages back to the LLM,
 * allowing it to retry or adjust its approach.
 * </p>
 *
 * @see com.github.ndanhkhoi.zeroagent.agent
 * @see com.github.ndanhkhoi.zeroagent.tool.builtin
 */
package com.github.ndanhkhoi.zeroagent.tool;
