package com.github.ndanhkhoi.zeroagent.agent;

/**
 * Configuration record defining default LLM parameters and limits for an agent session.
 * <p>
 * This immutable record encapsulates all configuration parameters needed to configure
 * an {@link AgentLoop} instance. It defines the LLM model to use, sampling parameters,
 * iteration limits, and the system prompt that controls the agent's persona and behavior.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AgentConfig config = new AgentConfig(
 *     "gpt-4o-mini",                    // model
 *     0.7,                              // temperature (creativity)
 *     10,                               // max iterations (prevents infinite loops)
 *     "You are a helpful coding assistant that can use tools and write code."
 * );
 * }</pre>
 *
 * @param model the ID of the primary LLM model (e.g., "gpt-4o-mini", "gpt-4o")
 * @param temperature the sampling temperature (0.0 = deterministic, 1.0 = creative)
 * @param maxIterations the maximum number of tool-call iterations before halting
 * @param systemPrompt the overarching role prompt controlling the agent's persona
 * @see AgentLoop
 */
public record AgentConfig(String model, Double temperature, int maxIterations, String systemPrompt) {}
