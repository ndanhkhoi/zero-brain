# Contributing to ZeroAgent 🧠

Thank you for your interest in contributing to ZeroAgent! As a framework designed to build autonomous agents, we have specific coding and architectural guidelines that ensure the library remains maintainable, predictable, and LLM-friendly.

---

## 🏗 Formatter & Code Style

We enforce **Palantir Java Format** (120 character limit, 4-space indentation) for consistent readability.

1. **Before Committing**: Run `./gradlew spotlessApply` locally to automatically format your code.
2. **CI Gates**: Our GitHub Actions pipeline will run `./gradlew spotlessCheck`. **Pull Requests that fail this formatting check will be rejected.**
3. **Imports**: Always use imports instead of fully qualified packages and classes except in unavoidable circumstances.

---

## 🤖 Agentic Design Patterns

ZeroAgent tools are consumed directly by the LLM via Function Calling. Write code with the AI as your consumer.

### 1. Stateless Tools
Tools (`io.zeroagent.tool.Tool`) must be strictly **stateless**. Do not store contextual conversation variables as class fields. Memory is maintained externally by the `Memory` layer. A Tool should only receive input arguments, perform the required work, and return the result.

### 2. Verbose Tool Schemas
While normal Java code favors brevity, your Tool's `description()` and `parametersSchema()` exist to teach the LLM how to use it.
- Describe edge cases explicitly.
- Use full, descriptive words for parameters.
- Provide instructions on what the LLM should do if the tool returns a certain error payload.

### 3. Fail Gracefully
Avoid throwing raw Java Exceptions from a `Tool`. Instead, catch exceptions and return `ToolResult.error("Reason...")`. This allows the autonomous Agent loop to see the error string, comprehend the failure, and attempt to self-correct in its next iteration.

---

## ☕ Modern Java 17+ Usage

ZeroAgent requires Java 17+. We expect contributors to leverage modern features:

- **Records**: Use `record` instead of traditional classes with getters/setters when defining simple data models (e.g., `ToolResult`).
- **Local Variable Inference**: Use `var` for local variables when the right-hand side makes the type obvious, reducing visual clutter.
- **Text Blocks**: Always use multi-line strings (`"""..."""`) for LLM descriptions, system prompts, or complex JSON/documentation blocks.

---

## 🛠 Project Architecture Constraints

- **Fluent API**: Ensure new configuration objects provide a builder with chained, fluent setter methods. The configuration of an Agent should read eloquently.
- **Zero Third-Party Clutter**: The `core/` module is strictly kept lean. Do not add heavy dependencies (like Guava, Apache Commons, etc.) unless absolutely mandatory and discussed in an Issue first.

---

## 📊 Code Quality Requirements

We maintain lightweight code quality standards to keep the framework maintainable while enabling fast iteration during early development.

### Quality Gates

Our CI pipeline enforces the following standards:

1. **Spotless** - Code formatting (Palantir Java Format, 120 char limit)
2. **Javadoc** - API documentation validation

### Pre-Commit Checklist

Before committing or creating a PR, ensure:

1. ✅ Run `./gradlew spotlessApply` to format code
2. ✅ Run `./gradlew javadoc` to verify documentation builds without errors
3. ✅ Run `./gradlew build` to ensure everything compiles
4. ✅ Verify new public APIs have Javadoc comments
5. ✅ No secrets or sensitive data in commits

### Running Quality Checks Locally

```bash
# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Generate and validate Javadoc
./gradlew javadoc

# Build project
./gradlew build
```

### Testing Approach

During early development, we prioritize manual testing via the Demo App over automated unit tests:

1. **Run Demo App**: Test new features interactively through `demo/src/main/java/com/github/ndanhkhoi/zeroagent/demo/DemoApp.java`
2. **Verify Behavior**: Ensure agents, tools, and skills work as expected in real scenarios
3. **Expand Demo**: Add test cases to the demo app to cover edge cases and integration scenarios

This approach provides practical integration testing that reflects actual usage patterns while maintaining development speed.

---

Thank you for building zero-agent with us!
