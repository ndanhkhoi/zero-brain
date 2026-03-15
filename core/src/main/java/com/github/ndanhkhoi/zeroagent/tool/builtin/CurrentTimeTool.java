package com.github.ndanhkhoi.zeroagent.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ndanhkhoi.zeroagent.tool.Tool;
import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A built-in Zero Brain tool that allows the LLM to query the host machine's current system date and time.
 * Required for temporal awareness since LLMs have a fixed training cutoff.
 */
public class CurrentTimeTool implements Tool {
    /**
     * Creates a new CurrentTimeTool instance.
     */
    public CurrentTimeTool() {
        this.mapper = new ObjectMapper();
    }

    private final ObjectMapper mapper;

    @Override
    public String name() {
        return "get_current_time";
    }

    @Override
    public String description() {
        return "Returns the current system date and time. Optionally accepts a timezone parameter (e.g., 'America/New_York', 'Asia/Tokyo'). Defaults to system timezone if not provided.";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        ObjectNode timezone = mapper.createObjectNode();
        timezone.put("type", "string");
        timezone.put(
                "description",
                "Optional IANA timezone identifier (e.g., 'America/New_York', 'Asia/Tokyo', 'UTC'). If not provided, uses system default timezone.");
        properties.set("timezone", timezone);

        schema.set("properties", properties);
        return schema;
    }

    @Override
    public ToolResult execute(String argumentsJson) {
        try {
            ZoneId zoneId = ZoneId.systemDefault();

            if (argumentsJson != null && !argumentsJson.trim().isEmpty()) {
                JsonNode args = mapper.readTree(argumentsJson);
                if (args.has("timezone") && !args.get("timezone").isNull()) {
                    String timezoneStr = args.get("timezone").asText();
                    zoneId = ZoneId.of(timezoneStr);
                }
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss z");
            String currentTime = ZonedDateTime.now(zoneId).format(formatter);
            return ToolResult.success(currentTime);
        } catch (Exception e) {
            return ToolResult.error("Failed to get current time: " + e.getMessage());
        }
    }
}
