# Code Style Guidelines

## Formatter

- **Palantir Java Format** enforced via Spotless plugin (120 char limit, 4-space indentation)
- **Always run `./gradlew spotlessApply` before committing**
- CI rejects PRs failing `./gradlew spotlessCheck`

## Imports

- Package: `com.github.ndanhkhoi.zeroagent.<module>`
- Always use imports instead of fully qualified packages and classes except in unavoidable (force majeure) circumstances.
- Group logically: java.*, javax.*, third-party, project packages

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `AgentLoop`, `ToolRouter` |
| Methods | camelCase | `chatStream`, `parametersSchema` |
| Constants | UPPER_SNAKE_CASE | `PROMPT` |
| Records | PascalCase | `ToolResult`, `AgentConfig` |
| Builders | Nested static `Builder` | `ZeroAgent.builder()...build()` |

## Types

- Use **Java 17+ records** for simple data models
- Use **var** when type is obvious from right-hand side
- Use **text blocks** (`"""..."""`) for multi-line strings, LLM descriptions, system prompts
