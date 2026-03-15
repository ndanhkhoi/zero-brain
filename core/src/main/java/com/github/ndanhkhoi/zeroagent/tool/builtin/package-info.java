/**
 * Built-in tool implementations for common agent capabilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides ready-to-use tool implementations that extend agent capabilities
 * without requiring custom development. These tools cover common use cases like time
 * retrieval, code execution, and dynamic skill loading.
 * </p>
 *
 * <h2>Available Tools</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.builtin.CurrentTimeTool} - Get current
 *       date and time with timezone support</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.builtin.JavaScriptTool} - Execute
 *       JavaScript code in a sandboxed Rhino environment</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.tool.builtin.LoadSkillsTool} - Dynamically
 *       load skills from YAML files during agent execution</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create built-in tools
 * List<Tool> tools = List.of(
 *     new CurrentTimeTool(),
 *     new JavaScriptTool(),
 *     new LoadSkillsTool(skills)
 * );
 *
 * // Register with tool router
 * ToolRouter toolRouter = new ToolRouter(tools);
 *
 * // Use in agent
 * AgentLoop loop = new AgentLoop(llmClient, toolRouter, memory, config, hooks, lockManager);
 *
 * // Agent can now use these tools
 * loop.chat("session-1", "What time is it in Tokyo?");  // Uses CurrentTimeTool
 * loop.chat("session-1", "Calculate fibonacci(10) in JavaScript");  // Uses JavaScriptTool
 * loop.chat("session-1", "Load the code-reviewer skill");  // Uses LoadSkillsTool
 * }</pre>
 *
 * <h2>CurrentTimeTool</h2>
 * <p>
 * Provides current date and time information with timezone support. Useful for:
 * </p>
 * <ul>
 *   <li>Timestamping conversations</li>
 *   <li>Scheduling and time-based reasoning</li>
 *   <li>Timezone conversions</li>
 * </ul>
 *
 * <h2>JavaScriptTool</h2>
 * <p>
 * Executes JavaScript code in a sandboxed Rhino environment. Supports:
 * </p>
 * <ul>
 *   <li>Mathematical calculations</li>
 *   <li>Data transformations</li>
 *   <li>String manipulations</li>
 *   <li>JSON processing</li>
 * </ul>
 * <p>
 * <b>Security:</b> Runs in Rhino Sandbox mode with restricted access to Java APIs.
 * </p>
 *
 * <h2>LoadSkillsTool</h2>
 * <p>
 * Meta-tool that enables agents to dynamically discover and load skills at runtime.
 * This allows agents to adapt their capabilities based on task requirements without
 * restarting. Skill instructions are returned in the tool result and become part of
 * the conversation history, naturally providing context for subsequent turns.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All built-in tools are thread-safe and can be used concurrently across multiple
 * sessions. Tools maintain no internal state.
 * </p>
 *
 * <h2>Creating Custom Tools</h2>
 * <p>
 * To create your own tools, implement the {@link com.github.ndanhkhoi.zeroagent.tool.Tool}
 * interface:
 * </p>
 * <pre>{@code
 * public class WeatherTool implements Tool {
 *     private final WeatherApiClient api;
 *
 *     public WeatherTool(String apiKey) {
 *         this.api = new WeatherApiClient(apiKey);
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "get_weather";
 *     }
 *
 *     @Override
 *     public String description() {
 *         return "Get current weather for a location";
 *     }
 *
 *     @Override
 *     public JsonNode parametersSchema() {
 *         // Define JSON schema for parameters
 *     }
 *
 *     @Override
 *     public ToolResult execute(String argumentsJson) {
 *         // Execute tool logic
 *     }
 * }
 * }</pre>
 *
 * @see com.github.ndanhkhoi.zeroagent.tool.Tool
 * @see com.github.ndanhkhoi.zeroagent.tool.ToolRouter
 */
package com.github.ndanhkhoi.zeroagent.tool.builtin;
