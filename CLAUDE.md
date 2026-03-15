# ZeroAgent

A modular Java 17+ framework for building agentic workflows using OpenAI Chat Completions API.

## Commands

```bash
./gradlew build                      # Build project
./gradlew spotlessApply              # Format code (run before committing)
./gradlew spotlessCheck              # Check formatting (CI)
./gradlew javadoc                    # Generate Javadoc (validates documentation)
```

## Guidelines

- [Code Style](.claude/code-style.md) - Formatting, naming, imports
- [Architecture](.claude/architecture.md) - Patterns, interfaces, dependencies
- [Javadoc](.claude/javadoc.md) - Documentation standards and conventions

## Pre-Commit

1. `./gradlew spotlessApply`
2. `./gradlew javadoc`  # Verify Javadoc builds without errors
3. `./gradlew build`
4. Verify new public APIs have Javadoc
5. No secrets in commits
