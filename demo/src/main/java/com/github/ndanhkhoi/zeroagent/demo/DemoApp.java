package com.github.ndanhkhoi.zeroagent.demo;

import com.github.ndanhkhoi.zeroagent.ZeroAgent;
import com.github.ndanhkhoi.zeroagent.agent.AgentResponse;
import com.github.ndanhkhoi.zeroagent.skill.SkillLoader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interactive demo application for ZeroAgent framework.
 * <p>
 * This application demonstrates the core features of ZeroAgent including:
 * </p>
 * <ul>
 *   <li>Interactive chat sessions with an AI agent</li>
 *   <li>Custom base URL configuration for different OpenAI-compatible APIs</li>
 *   <li>Optional skill loading from resources directory</li>
 *   <li>Event hooks for monitoring agent behavior</li>
 * </ul>
 * <p>
 * Usage: Run the application and provide credentials when prompted:
 * </p>
 * <pre>
 *   Enter: &lt;API_KEY&gt; &lt;BASE_URL&gt; &lt;MODEL_NAME&gt; (separated by space)
 *   Example: sk-1234 <a href="https://api.openai.com/v1">...</a> gpt-4o
 * </pre>
 */
public class DemoApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoApp.class);

    // Prevent instantiation
    private DemoApp() {}

    /**
     * Entry point for the demo application.
     * <p>
     * Initializes an interactive chat session with a ZeroAgent agent.
     * Reads configuration from stdin and runs a message loop until the user types "exit".
     * </p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {

            LOGGER.info("--- ZeroAgent Setup ---");
            LOGGER.info("Enter: <API_KEY> <BASE_URL> <MODEL_NAME> (separated by space)");
            LOGGER.info("Example: sk-1234 https://api.openai.com/v1 gpt-4o");
            System.out.print("> ");

            String inputLine = scanner.nextLine().trim();
            String[] parts = inputLine.split("\\s+");

            if (parts.length < 3) {
                LOGGER.error("Error: All 3 parameters (API_KEY, BASE_URL, MODEL_NAME) are required.");
                return;
            }

            String apiKey = parts[0];
            String baseUrl = parts[1];
            String modelName = parts[2];

            LOGGER.info("\nInitializing ZeroAgent Agent...");

            SkillLoader skillLoader = null;
            try {
                URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("my-skills");
                if (resourceUrl != null) {
                    skillLoader = SkillLoader.fromDirectory(Paths.get(resourceUrl.toURI()));
                } else {
                    LOGGER.warn("Warning: Could not find my-skills directory in resources.");
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("Warning: Could not load skills from resources - {}", e.getMessage());
            }

            ZeroAgent.Builder builder = ZeroAgent.builder()
                    .apiKey(apiKey)
                    .model(modelName)
                    .skills(skillLoader)
                    .onThinking(() -> {
                        LOGGER.info("[Agent is thinking...]");
                    })
                    .onToolCall((name, arguments) -> {
                        LOGGER.info("[Calling tool: {} with args: {}]", name, arguments);
                    })
                    .onToolResult((name, result) -> {
                        LOGGER.info("[Tool {} returned {}]", name, result.success() ? "SUCCESS" : "ERROR");
                    })
                    .onToken(LOGGER::info)
                    .onComplete(() -> LOGGER.info("[Agent execution complete]"))
                    .onError(error -> LOGGER.error("Agent execution encountered an error: {}", error.getMessage()));

            if (!"default".equalsIgnoreCase(baseUrl) && !"https://api.openai.com/v1".equalsIgnoreCase(baseUrl)) {
                builder.baseUrl(baseUrl);
            }

            ZeroAgent agent = builder.build();
            String sessionId = "demo-session-interactive";

            LOGGER.info("=== Chat Session Started (Type 'exit' to quit) ===");

            while (true) {
                System.out.print("\nYou: ");
                String message = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(message)) {
                    LOGGER.info("Exiting chat session.");
                    break;
                }

                if (message.isEmpty()) {
                    continue;
                }

                AgentResponse response =
                        agent.message(message).sessionId(sessionId).send();
                LOGGER.info("--- Response Info ---");
                LOGGER.info("Iterations: {}", response.iterations());
                LOGGER.info("Tool Calls: {}", response.toolCallsExecuted());
            }
        } catch (Exception e) {
            LOGGER.error("Error initializing demo app: {}", e.getMessage(), e);
        }
    }
}
