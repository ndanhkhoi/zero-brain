/**
 * Agent lifecycle management, configuration, and orchestration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the core agent orchestration logic for Zero Brain. It manages the reasoning loop,
 * coordinates between the LLM, tools, and skills, and handles the complete lifecycle of agent interactions
 * from initialization to response generation.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.agent.AgentLoop} - Main agent execution loop that orchestrates
 *       the reasoning process, tool calls, and skill invocation</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.agent.AgentConfig} - Immutable configuration record defining
 *       LLM parameters, iteration limits, and system prompts</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.agent.AgentHooks} - Callback mechanism for monitoring agent
 *       execution events (thinking, tool calls, token streaming)</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.agent.AgentResponse} - Response wrapper containing final
 *       answer, tool execution results, and metadata</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an LLM client
 * LlmClient llmClient = new OpenAiChatClient(apiKey);
 *
 * // Create tools for the agent
 * List<Tool> tools = List.of(
 *     new CurrentTimeTool(),
 *     new JavaScriptTool()
 * );
 * ToolRouter toolRouter = new ToolRouter(tools);
 *
 * // Configure the agent
 * AgentConfig config = new AgentConfig(
 *     "gpt-4o-mini",           // model
 *     0.7,                     // temperature
 *     10,                      // max iterations
 *     "You are a helpful assistant that can use tools and write code."
 * );
 *
 * // Create and run the agent
 * AgentLoop loop = new AgentLoop(llmClient, toolRouter, config);
 * AgentResponse response = loop.run("What time is it? Write a Python function to calculate fibonacci numbers.");
 *
 * System.out.println(response.answer());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link com.github.ndanhkhoi.zeroagent.agent.AgentLoop} is thread-safe when used with a
 * {@link com.github.ndanhkhoi.zeroagent.concurrent.SessionLockManager}. Multiple threads can safely
 * execute agents for different sessions concurrently, while messages for the same session are
 * processed sequentially.
 * </p>
 *
 * <h2>Extension Points</h2>
 * <ul>
 *   <li>Implement custom tools via {@link com.github.ndanhkhoi.zeroagent.tool.Tool} interface</li>
 *   <li>Add lifecycle hooks via {@link com.github.ndanhkhoi.zeroagent.agent.AgentHooks}</li>
 *   <li>Customize LLM behavior via {@link com.github.ndanhkhoi.zeroagent.llm.LlmClient} implementations</li>
 * </ul>
 *
 * @see com.github.ndanhkhoi.zeroagent.tool
 * @see com.github.ndanhkhoi.zeroagent.skill
 * @see com.github.ndanhkhoi.zeroagent.llm
 */
package com.github.ndanhkhoi.zeroagent.agent;
