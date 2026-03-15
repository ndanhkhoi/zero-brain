/**
 * Skill loading and YAML parsing.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides dynamic prompt template management through "skills" - reusable prompt
 * components defined in YAML files. Skills can be loaded at runtime and provided to the LLM
 * through tool calls, enhancing capabilities without code changes.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.skill.Skill} - Immutable record representing a
 *       loaded skill with metadata, prompt template, and configuration</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.skill.SkillLoader} - Loads skills from YAML files
 *       and parses frontmatter metadata</li>
 * </ul>
 *
 * <h2>Skill File Format</h2>
 * <p>
 * Skills are defined as Markdown files with YAML frontmatter:
 * </p>
 * <pre>{@code
 * ---
 * name: "code-reviewer"
 * description: "Reviews code for best practices and potential issues"
 * version: "1.0.0"
 * tags: ["code", "review", "analysis"]
 * ---
 *
 * You are a code reviewer. Analyze the provided code for:
 * - Code style and formatting
 * - Potential bugs or issues
 * - Performance concerns
 * - Security vulnerabilities
 * - Best practice violations
 *
 * Provide constructive feedback and specific suggestions for improvement.
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Load skills from directory
 * SkillLoader skillLoader = SkillLoader.fromDirectory(Path.of("skills/"));
 *
 * // Register with ZeroBrain
 * ZeroBrain agent = ZeroBrain.builder()
 *     .apiKey(apiKey)
 *     .skills(skillLoader)
 *     .build();
 *
 * // LLM can now load skills via the load_skills tool
 * agent.message("Load the code-reviewer skill").send();
 * }</pre>
 *
 * <h2>Built-in Skills</h2>
 * <p>
 * Zero Brain includes a meta-tool {@link com.github.ndanhkhoi.zeroagent.tool.builtin.LoadSkillsTool}
 * that allows agents to dynamically load skills during execution. This enables agents to discover
 * and use new capabilities without restarting. Skill instructions are returned in tool results
 * and become part of the conversation history, naturally providing context for subsequent turns.
 * </p>
 *
 * <h2>Extension Points</h2>
 * <ul>
 *   <li>Create custom skill files in YAML format with Markdown content</li>
 *   <li>Extend {@link com.github.ndanhkhoi.zeroagent.skill.SkillLoader} for custom file locations</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link com.github.ndanhkhoi.zeroagent.skill.SkillLoader} and {@link com.github.ndanhkhoi.zeroagent.skill.Skill}
 * are thread-safe and can be shared across concurrent agent executions.
 * </p>
 *
 * @see com.github.ndanhkhoi.zeroagent.agent
 * @see com.github.ndanhkhoi.zeroagent.tool
 */
package com.github.ndanhkhoi.zeroagent.skill;
