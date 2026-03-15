# Architecture Patterns

## Fluent Builder API

All configuration objects must provide a Builder with chained fluent setter methods:

```java
ZeroAgent agent = ZeroAgent.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .model("gpt-4o-mini")
    .systemPrompt("You are a helpful assistant.")
    .onToken(System.out::print)
    .build();
```

## Tool Interface

```java
public interface Tool {
    String name();                    // Unique snake_case identifier
    String description();             // LLM-readable description
    JsonNode parametersSchema();      // JSON Schema for parameters
    ToolResult execute(String argumentsJson);
}
```

### Tool Design Principles

1. **Stateless**: Tools must NOT store conversation state as class fields
2. **Verbose Schemas**: `description()` and `parametersSchema()` teach the LLM how to use the tool
3. **Graceful Failure**: Return `ToolResult.error("reason")` instead of throwing exceptions

```java
@Override
public ToolResult execute(String argumentsJson) {
    try {
        // Tool logic here
        return ToolResult.success(result);
    } catch (Exception e) {
        return ToolResult.error("Operation failed: " + e.getMessage());
    }
}
```

## Memory Interface

```java
public interface Memory {
    void addMessage(String sessionId, ChatCompletionMessageParam message);
    List<ChatCompletionMessageParam> getMessages(String sessionId);
    void clear(String sessionId);
}
```

## Error Handling

- **Never throw raw exceptions from Tools** - return `ToolResult.error()` instead
- This allows the agent loop to see errors and self-correct
- Use `IllegalArgumentException` for builder validation (e.g., null checks)
- Log errors via SLF4J (api only - no implementation in core)

## Dependencies

- Core module is strictly lean - avoid adding heavy dependencies
- Main: `openai-java`, `slf4j-api`, `rhino` (JavaScript sandbox)
- Test: `junit-jupiter`, `logback-classic` (runtime)
- Discuss new dependencies in an Issue before adding

## Project Structure

```
zero-agent/
├── core/src/main/java/com/github/ndanhkhoi/zeroagent/
│   ├── agent/       # Agent loop, config, response
│   ├── llm/         # LLM client abstraction
│   ├── memory/      # Conversation storage
│   ├── skill/       # Skill loading (agentskills.io)
│   └── tool/        # Tool interface & built-ins
├── demo/            # Example application
└── docs/            # Documentation
```

## Quality Enforcement

Quality gates are enforced at multiple levels to maintain code standards:

### Build-Time Checks

- **Spotless** - Enforces consistent code formatting
- **Checkstyle** - Validates style and complexity limits
- **PMD** - Detects code smells and duplicate code
- **SpotBugs** - Identifies bug patterns and security issues
- **JaCoCo** - Enforces test coverage thresholds

### Quality Thresholds

- **Coverage**: 80% line, 70% branch minimum
- **Complexity**: Cyclomatic complexity ≤ 15
- **Class Size**: ≤ 30 methods per class
- **Method Length**: ≤ 100 lines per method
- **All violations block the build**

### Development Workflow

1. Write code following architectural patterns
2. Run `./gradlew spotlessApply` to format
3. Run `./gradlew qualityCheck` to verify
4. Fix any violations before pushing
5. CI validates quality gate on all PRs

See [Code Quality Guide](code-quality.md) for detailed information.
```
