package com.github.ndanhkhoi.zeroagent.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ndanhkhoi.zeroagent.skill.Skill;
import com.github.ndanhkhoi.zeroagent.tool.Tool;
import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A meta-tool injected automatically by the framework when folder-based Skills are registered.
 * <p>
 * Allows the LLM to lazily request and retrieve the full multi-step instruction block
 * of a specific Skill only when the LLM deems it contextually necessary,
 * saving vast amounts of prompt tokens.
 *
 * <p>Skill instructions are returned in the tool result, which becomes part of the
 * conversation history. The LLM can reference these instructions in subsequent turns
 * without needing to reload the skill. This provides natural "caching" through the
 * conversation context itself.
 */
public class LoadSkillsTool implements Tool {
    private final ObjectMapper mapper;
    private final Map<String, Skill> skillsMap;

    /**
     * Creates a LoadSkillsTool.
     *
     * @param skills the list of all available skills
     */
    public LoadSkillsTool(List<Skill> skills) {
        this.mapper = new ObjectMapper();
        this.skillsMap = Map.copyOf(skills.stream().collect(Collectors.toMap(Skill::name, s -> s)));
    }

    @Override
    public String name() {
        return "load_skills";
    }

    @Override
    public String description() {
        return "Loads the full instructions and context for one or multiple skills. Call this tool when you need to execute skills but don't know the full instructions.";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        ObjectNode skillNames = mapper.createObjectNode();
        skillNames.put("type", "array");
        ObjectNode items = mapper.createObjectNode();
        items.put("type", "string");
        skillNames.set("items", items);
        skillNames.put(
                "description",
                "The exact names of the skills to load. You can provide multiple skill names to load them all at once.");
        properties.set("skill_names", skillNames);

        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("skill_names"));
        return schema;
    }

    /**
     * Executes the skill loading operation.
     *
     * <p>Parses the requested skill names and returns their full instructions.
     * The instructions are returned in the tool result, which becomes part of
     * the conversation history. The LLM can reference these instructions in
     * subsequent turns without needing to reload the skill.
     *
     * @param argumentsJson JSON containing "skill_names" array with skill names to load
     * @return the combined instructions of all requested skills, or error message
     */
    @Override
    public ToolResult execute(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            if (!args.has("skill_names") || !args.get("skill_names").isArray()) {
                return ToolResult.error("Missing or invalid required parameter: skill_names (must be an array)");
            }

            StringBuilder sb = new StringBuilder();
            for (JsonNode node : args.get("skill_names")) {
                String skillName = node.asText();
                sb.append("\n\n--- Skill: ").append(skillName).append(" ---\n\n");

                Skill skill = skillsMap.get(skillName);
                if (skill == null) {
                    sb.append("Error: Skill not found.");
                } else {
                    sb.append(skill.instructions());
                }
            }

            return ToolResult.success(sb.toString().trim());
        } catch (Exception e) {
            return ToolResult.error("Failed to parse arguments: " + e.getMessage());
        }
    }
}
