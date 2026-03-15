# 2. Tools and Skills

ZeroAgent handles functional actions via **Tools** (code-driven execution) and **Skills** (prompt-driven workflows).

---

## 🛠️ Custom Tools

You can register custom functionality by implementing the `io.zeroagent.tool.Tool` interface. A tool requires:
1. `name()`: Identifier for the LLM.
2. `description()`: Tells the LLM *when* and *why* to use it.
3. `parametersSchema()`: A JSON Schema object declaring the inputs.
4. `execute(String jsonArgs)`: The runtime method executed by the Agent.

```java
import com.github.ndanhkhoi.zeroagent.tool.Tool;
import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WeatherTool implements Tool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "get_weather"; }

    @Override
    public String description() { return "Gets the current weather for a city."; }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        // ... define 'city' property here
        return schema;
    }

    @Override
    public ToolResult execute(String args) {
        return ToolResult.success("The weather in the requested city is 24°C.");
    }
}
```

> [!TIP]
> Use `.tools(new WeatherTool())` on the Builder to register it!

---

## 🔒 The JavaScript Execution Sandbox

ZeroAgent includes a built-in `JavaScriptTool` that enables the agent to dynamically script algorithms (e.g., calculating advanced math, parsing complex logical gates) on the fly using Mozilla Rhino.

**Security**: 
To prevent the LLM from injecting malicious JavaScript that acts on the host OS (like reading files via `java.io.File` or shutting down the server via `System.exit()`), the tool is wrapped in a strict `ClassShutter`. 

This guarantees the script runs purely for computation and cannot access *any* underlying Java classes.

---

## 🧩 Agent Skills (`agentskills.io`)

If you want to provide complex workflows or contextual knowledge without hardcoding strings in Java, ZeroAgent fully supports the folder-based **Agent Skills Specification**.

A Skill is a directory containing a `SKILL.md` file wrapped in YAML frontmatter.

### Directory Structure
```
skills/
└── code-review-skill/
    └── SKILL.md
```

### SKILL.md Example
```markdown
---
name: code_reviewer
description: Evaluates logic and formatting for Java source code
---
# Instructions
You are an expert Java reviewer. Always enforce DRY principles.
When reviewing PRs, verify that logging is properly utilized.
```

### Loading Skills dynamically
```java
ZeroAgent agent = ZeroAgent.builder()
    .skills(SkillLoader.fromDirectory(Path.of("./skills")))
    .build();
```
ZeroAgent injects the skill names into the System prompt and provides the LLM an internal "Read Skill" tool context, keeping token limits low until the LLM actively requests the details of a specific workflow.
