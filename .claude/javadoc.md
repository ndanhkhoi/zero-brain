# Javadoc Guidelines

This document defines the standards and conventions for writing Javadoc documentation in the ZeroAgent framework.

## Overview

ZeroAgent maintains comprehensive Javadoc documentation for both framework users (API documentation) and contributors (internal implementation details). All public APIs must be documented with Javadoc.

## Documentation Standards

### Class-Level Documentation

Every public class, interface, record, and enum must have class-level Javadoc that includes:

- **One-line summary** - Brief description of what the class does
- **Detailed description** - 2-3 sentences explaining purpose and use cases
- **Usage example** - Code snippet showing typical usage (for complex classes)
- **Thread safety** - Document thread-safety guarantees

**Example:**
```java
/**
 * The core orchestrator that manages the multi-turn conversational agent loop.
 * <p>
 * AgentLoop coordinates the complete reasoning process, including streaming responses
 * from the LLM, managing conversation context and memory, executing tool calls, and
 * dispatching lifecycle events to registered hooks.
 * <p>
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AgentLoop loop = new AgentLoop(llmClient, toolRouter, memory, skills, config, hooks, skillCache, lockManager);
 * AgentResponse response = loop.chat("session-123", "What time is it?");
 * }</pre>
 * <p>
 * <h2>Thread Safety</h2>
 * This class is thread-safe when used with a SessionLockManager.
 *
 */
public class AgentLoop {
    // ...
}
```

### Method-Level Documentation

Every public method must have Javadoc that includes:

- **One-line summary** - What the method does
- **Detailed description** - Additional context if needed (preconditions, postconditions)
- **@param tags** - ALL parameters (no exceptions)
- **@return tag** - ALL non-void methods
- **@throws tag** - ALL checked exceptions and documented unchecked exceptions

**Example:**
```java
/**
 * Executes the conversational loop with a text-only prompt.
 * <p>
 * This method acquires a lock for the session, adds the user message to memory,
 * and begins the iterative reasoning process.
 *
 * @param sessionId a unique identifier mapping the conversation to memory state
 * @param userMessageText the text prompt from the user
 * @return the final accumulated response after the LLM completes its thought process
 * @throws RuntimeException if max iterations is reached or an error occurs during streaming
 */
public AgentResponse chat(String sessionId, String userMessageText) {
    // ...
}
```

### Field Documentation

Public fields should have Javadoc explaining their purpose:

```java
/**
 * Maps session IDs to the set of skill names that have been loaded in that session.
 * Uses ConcurrentHashMap for thread-safety without explicit synchronization.
 */
private final ConcurrentHashMap<String, Set<String>> cache;
```

### Package Documentation

Every package must have a `package-info.java` file with:

- Package purpose and overview
- Key classes with links
- Usage example
- Related packages

**Example:**
```java
/**
 * Agent lifecycle management, configuration, and orchestration.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link AgentLoop} - Main agent execution loop</li>
 *   <li>{@link AgentConfig} - Agent configuration</li>
 * </ul>
 *
 */
package com.github.ndanhkhoi.zeroagent.agent;
```

## Required Tags

### @param
**Required for:** ALL method parameters

```java
* @param sessionId the unique conversation identifier
```

### @return
**Required for:** ALL non-void methods

```java
* @return the chronological list of messages for this session
```

### @throws
**Required for:** ALL checked exceptions and documented unchecked exceptions

```java
* @throws RuntimeException if max iterations is reached
```

### @see
**Usage:** Link to related classes or methods

```java
* @see AgentLoop
* @see Tool
```

## Code Examples

### When to Include Examples

Include usage examples for:
- Complex APIs with multiple steps
- Classes with non-obvious usage patterns
- Framework entry points (e.g., ZeroAgent builder)
- Template methods or patterns

### Example Format

Use `<pre>{@code ... }</pre>` for code examples:

```java
* <h2>Usage Example</h2>
* <pre>{@code
* Memory memory = new InMemoryStore();
* memory.addMessage("session-1", userMessage);
* List<ChatCompletionMessageParam> history = memory.getMessages("session-1");
* }</pre>
```

## Thread Safety Documentation

Document thread safety for classes that:
- Are designed for concurrent use
- Maintain shared state
- Use locks or synchronization

**Levels of thread safety:**
- **Thread-safe** - Can be safely used by multiple threads
- **Conditionally thread-safe** - Thread-safe with external synchronization
- **Not thread-safe** - Single-threaded use only

**Example:**
```java
* <h2>Thread Safety</h2>
* This class is thread-safe when used with a SessionLockManager. Multiple threads
* can safely execute agents for different sessions concurrently.
```

## HTML in Javadoc

Use semantic HTML for structure:
- `<p>` - Paragraphs
- `<ul>`, `<li>` - Lists
- `<h2>`, `<h3>` - Section headers (start at h2)
- `<pre>{@code ... }</pre>` - Code blocks

**Important:**
- Escape HTML entities: `&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`
- Use `{@code ...}` inline instead of `<code>`
- Use `{@link ClassName}` for links instead of `<a href>`

## Package-Level Documentation

Create `package-info.java` for each package:

```java
/**
 * Brief description of the package purpose.
 *
 * <h2>Overview</h2>
 * Detailed description of what this package provides.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link ClassName} - Description</li>
 * </ul>
 *
 */
package com.github.ndanhkhoi.zeroagent.package;
```

## Pre-Commit Checklist

Before committing code:

1. **Add Javadoc** to all new public classes and methods
2. **Update existing Javadoc** if behavior changes
3. **Check for warnings:** Run `./gradlew javadoc` and fix warnings
4. **Verify examples:** Test code examples in Javadoc comments
5. **Check links:** Ensure `{@link}` references resolve correctly

## Common Mistakes to Avoid

1. **Missing @param or @return tags** - All parameters and return values must be documented
2. **Vague descriptions** - Be specific about what methods do and what they return
3. **Broken @link references** - Verify all `{@link}` targets exist
4. **HTML entities not escaped** - Use `&amp;` not `&`
5. **Empty Javadoc** - Avoid `/** */` - either document or don't use Javadoc

## Build Integration

The project automatically validates Javadoc quality:

```bash
./gradlew javadoc      # Generate Javadoc (fails on errors)
./gradlew build        # Full build (includes Javadoc check)
```

Javadoc validation is integrated into CI. Pull requests with Javadoc errors will fail.

## Resources

- [Oracle Javadoc Guidelines](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- [Effective Java: Documentation](https://www.oracle.com/java/technologies/javase/codeconventions-documentation.html)
- [ZeroAgent Overview](../../src/main/javadoc/overview.html)
