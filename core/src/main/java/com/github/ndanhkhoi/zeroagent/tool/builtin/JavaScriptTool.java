package com.github.ndanhkhoi.zeroagent.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ndanhkhoi.zeroagent.tool.Tool;
import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * A highly capable built-in sandbox tool powered by the Mozilla Rhino engine.
 * <p>
 * Permits the LLM to write and evaluate ES6-compatible JavaScript snippets dynamically
 * to solve complex mathematical tracking, data mutations, or logical operations
 * that would fail simple text Generation.
 *
 * <p>Secure by default: Completely isolates the JS context using a {@code ClassShutter}
 * that blocks all attempts to access raw Java classes or host internal objects.
 */
public class JavaScriptTool implements Tool {
    /**
     * Creates a new JavaScriptTool instance.
     */
    public JavaScriptTool() {
        this.mapper = new ObjectMapper();
    }

    private final ObjectMapper mapper;

    @Override
    public String name() {
        return "evaluate_javascript";
    }

    @Override
    public String description() {
        return "Evaluates JavaScript code and returns the result. Use this tool for complex mathematical calculations or executing custom script logic. Return the final result of your computation as the last evaluated expression. DO NOT use BigInt syntax (e.g., 10n) or very modern ES2020+ features, as the engine (Rhino) only supports up to ES6. Use standard numbers or a library from CDN if large integers are required.";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        ObjectNode code = mapper.createObjectNode();
        code.put("type", "string");
        code.put(
                "description",
                "The JavaScript code to evaluate. Must be ES5/ES6 compatible. DO NOT use BigInt (e.g. 10n)!");
        properties.set("code", code);

        ObjectNode libs = mapper.createObjectNode();
        libs.put("type", "array");
        libs.put(
                "description",
                "Optional list of CDN URLs of JavaScript libraries to load before evaluating the code (e.g. lodash, mathjs).");
        ObjectNode items = mapper.createObjectNode();
        items.put("type", "string");
        libs.set("items", items);
        properties.set("libs", libs);

        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("code"));
        return schema;
    }

    @Override
    public ToolResult execute(String argumentsJson) {
        Context cx = Context.enter();
        try {
            cx.setClassShutter(new ClassShutter() {
                @Override
                public boolean visibleToScripts(String fullClassName) {
                    return false; // Deny access to all Java classes
                }
            });
            Scriptable scope = cx.initStandardObjects();

            JsonNode args = mapper.readTree(argumentsJson);
            if (!args.has("code")) {
                return ToolResult.error("Missing required parameter: code");
            }

            if (args.has("libs") && args.get("libs").isArray()) {
                HttpClient client = HttpClient.newHttpClient();
                for (JsonNode libUrl : args.get("libs")) {
                    String url = libUrl.asText();
                    HttpRequest request =
                            HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        cx.evaluateString(scope, response.body(), url, 1, null);
                    } else {
                        return ToolResult.error(
                                "Failed to load library from " + url + ": HTTP " + response.statusCode());
                    }
                }
            }

            String code = args.get("code").asText();
            Object result = cx.evaluateString(scope, code, "script", 1, null);
            return ToolResult.success(Context.toString(result));
        } catch (Exception e) {
            return ToolResult.error("JavaScript evaluation failed: " + e.getMessage());
        } finally {
            Context.exit();
        }
    }
}
