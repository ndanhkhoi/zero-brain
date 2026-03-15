package com.github.ndanhkhoi.zeroagent.agent;

/**
 * Represents the final result returned after an agent loop finishes execution.
 * <p>
 * This record encapsulates the complete response from an agent execution, including
 * the accumulated text content, statistics about tool usage, and iteration count.
 * It provides insights into the agent's reasoning process and resource utilization.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AgentResponse response = agent.chat("session-1", "Calculate fibonacci(10)");
 *
 * System.out.println("Answer: " + response.content());
 * System.out.println("Tools used: " + response.toolCallsExecuted());
 * System.out.println("Iterations: " + response.iterations());
 * }</pre>
 *
 * @param content the fully accumulated textual response from the LLM across all iterations
 * @param toolCallsExecuted the total number of tool invocations performed during the loop
 * @param iterations the total number of complete request-response round-trips to the LLM
 * @see AgentLoop
 */
public record AgentResponse(String content, int toolCallsExecuted, int iterations) {}
