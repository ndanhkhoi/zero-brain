package com.github.ndanhkhoi.zeroagent;

import com.github.ndanhkhoi.zeroagent.agent.AgentConfig;
import com.github.ndanhkhoi.zeroagent.agent.AgentHooks;
import com.github.ndanhkhoi.zeroagent.agent.AgentLoop;
import com.github.ndanhkhoi.zeroagent.agent.AgentResponse;
import com.github.ndanhkhoi.zeroagent.agent.DefaultSystemPrompt;
import com.github.ndanhkhoi.zeroagent.agent.ImageInput;
import com.github.ndanhkhoi.zeroagent.concurrent.PerSessionLockManager;
import com.github.ndanhkhoi.zeroagent.concurrent.SessionLockManager;
import com.github.ndanhkhoi.zeroagent.llm.LlmClient;
import com.github.ndanhkhoi.zeroagent.llm.OpenAiChatClient;
import com.github.ndanhkhoi.zeroagent.memory.InMemoryStore;
import com.github.ndanhkhoi.zeroagent.memory.Memory;
import com.github.ndanhkhoi.zeroagent.skill.Skill;
import com.github.ndanhkhoi.zeroagent.skill.SkillLoader;
import com.github.ndanhkhoi.zeroagent.tool.Tool;
import com.github.ndanhkhoi.zeroagent.tool.ToolResult;
import com.github.ndanhkhoi.zeroagent.tool.ToolRouter;
import com.github.ndanhkhoi.zeroagent.tool.builtin.CurrentTimeTool;
import com.github.ndanhkhoi.zeroagent.tool.builtin.JavaScriptTool;
import com.github.ndanhkhoi.zeroagent.tool.builtin.LoadSkillsTool;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Entry point and builder facade for the ZeroAgent framework.
 * <p>
 * This class provides a fluent builder API for configuring and executing AI agents with
 * tools, skills, memory, and custom LLM providers. It simplifies agent setup while
 * maintaining flexibility for advanced use cases.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Basic usage with defaults
 * ZeroAgent agent = ZeroAgent.builder()
 *     .apiKey(System.getenv("OPENAI_API_KEY"))
 *     .build();
 *
 * AgentResponse response = agent
 *     .message("What time is it?")
 *     .send();
 *
 * System.out.println(response.answer());
 * }</pre>
 *
 * <h2>Advanced Configuration</h2>
 * <pre>{@code
 * // Custom tools and skills
 * SkillLoader skillLoader = new SkillLoader();
 * List<Skill> skills = skillLoader.loadSkills("skills/").values().stream().toList();
 *
 * ZeroAgent agent = ZeroAgent.builder()
 *     .apiKey(System.getenv("OPENAI_API_KEY"))
 *     .model("gpt-4o-mini")
 *     .temperature(0.7)
 *     .maxIterations(10)
 *     .systemPrompt("You are a helpful coding assistant")
 *     .tools(new CurrentTimeTool(), new JavaScriptTool())
 *     .skills(skillLoader, skills)
 *     .memory(new InMemoryStore())
 *     .sessionLockManager(new PerSessionLockManager())
 *     .onToken(token -> System.out.print(token))  // Stream tokens
 *     .build();
 *
 * // Chat with images
 * AgentResponse response = agent
 *     .message("What's in this image?")
 *     .image(imageStream, "image/jpeg")
 *     .sessionId("user-123")
 *     .send();
 * }</pre>
 *
 * <h2>Default Components</h2>
 * When not explicitly configured, the builder uses sensible defaults:
 * <ul>
 *   <li>LLM Client: {@link OpenAiChatClient} with provided API key</li>
 *   <li>Model: gpt-4o-mini</li>
 *   <li>Tools: {@link CurrentTimeTool}, {@link JavaScriptTool}, {@link LoadSkillsTool}</li>
 *   <li>Memory: {@link InMemoryStore}</li>
 *   <li>Lock Manager: {@link PerSessionLockManager}</li>
 *   <li>System Prompt: {@link DefaultSystemPrompt}</li>
 * </ul>
 *
 * @see AgentLoop
 * @see AgentConfig
 * @see AgentHooks
 */
public class ZeroAgent {

    private LlmClient llmClient;
    private final ToolRouter toolRouter;
    private final Memory memory;
    private final List<Skill> skills;
    private final AgentConfig config;
    private final AgentHooks hooks;
    private final SessionLockManager lockManager;

    private ZeroAgent(
            LlmClient llmClient,
            ToolRouter toolRouter,
            Memory memory,
            List<Skill> skills,
            AgentConfig config,
            AgentHooks hooks,
            SessionLockManager lockManager) {
        this.llmClient = llmClient;
        this.toolRouter = toolRouter;
        this.memory = memory;
        this.skills = skills;
        this.config = config;
        this.hooks = hooks;
        this.lockManager = lockManager;
    }

    /**
     * Starts building a fluent chat request.
     *
     * <p>This method returns a {@link ChatRequest} builder that allows chaining
     * additional configuration like session ID, images, before executing the request.
     *
     * @param text the message prompt to send to the agent
     * @return a ChatRequest builder for further configuration
     */
    public ChatRequest message(String text) {
        return new ChatRequest(this, text);
    }

    /**
     * Executes the agent loop with the given user message, optional images, and session id.
     */
    private AgentResponse chat(String sessionId, String userMessage, List<ImageInput> images) {
        AgentLoop loop = new AgentLoop(llmClient, toolRouter, memory, skills, config, hooks, lockManager);
        return loop.chat(sessionId, userMessage, images);
    }

    /**
     * Allows switching the LLM provider at runtime.
     * @param llmClient the new client to use for subsequent chat calls
     */
    public void setLlmClient(LlmClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        this.llmClient = llmClient;
    }

    /**
     * Fluent builder for constructing and executing chat requests.
     * <p>
     * This class provides a chainable API for configuring message parameters
     * such as session ID and image attachments before sending the request to the agent.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * AgentResponse response = agent.message("What's in this image?")
     *     .sessionId("user-123")
     *     .image(imageStream, "image/jpeg")
     *     .send();
     * }</pre>
     */
    public static class ChatRequest {
        private final ZeroAgent agent;
        private final String text;
        private String sessionId = UUID.randomUUID().toString();
        private final List<ImageInput> images = new ArrayList<>();

        /**
         * Creates a new ChatRequest builder.
         *
         * @param agent the ZeroAgent agent that will execute this request
         * @param text the user message text
         */
        private ChatRequest(ZeroAgent agent, String text) {
            this.agent = agent;
            this.text = text;
        }

        /**
         * Sets the session ID for this request.
         * <p>
         * If not set, a random UUID will be generated. Sessions with the same ID
         * share conversation history in memory.
         *
         * @param sessionId the unique session identifier
         * @return this builder for method chaining
         */
        public ChatRequest sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Attaches an image to the message.
         * <p>
         * Multiple images can be attached by calling this method multiple times.
         * Images are processed using vision-capable models.
         *
         * @param stream the input stream containing the image data
         * @param mimeType the MIME type of the image (e.g., "image/jpeg", "image/png")
         * @return this builder for method chaining
         */
        public ChatRequest image(InputStream stream, String mimeType) {
            this.images.add(new ImageInput(stream, mimeType));
            return this;
        }

        /**
         * Executes the chat request and returns the agent's response.
         * <p>
         * This method triggers the agent loop, which may involve multiple LLM calls
         * and tool executions before returning the final response.
         *
         * @return the agent's response containing the answer and metadata
         */
        public AgentResponse send() {
            return agent.chat(sessionId, text, images.isEmpty() ? null : images);
        }
    }

    /**
     * Creates a new builder for configuring a ZeroAgent agent.
     *
     * <p>The builder provides a fluent API for setting all agent configuration
     * options before constructing the immutable agent instance.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for configuring and creating ZeroAgent agent instances.
     * <p>
     * This builder allows customization of all aspects of the agent including
     * LLM provider, model parameters, tools, skills, memory, and event hooks.
     * Sensible defaults are provided for all optional settings.
     * <p>
     * <strong>Example:</strong>
     * <pre>{@code
     * ZeroAgent agent = ZeroAgent.builder()
     *     .apiKey(System.getenv("OPENAI_API_KEY"))
     *     .model("gpt-4o-mini")
     *     .temperature(0.7)
     *     .tools(new CustomTool())
     *     .onToken(token -> System.out.print(token))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        /**
         * Creates a new Builder with default configuration.
         * <p>
         * All configuration values are initialized to defaults:
         * <ul>
         *   <li>maxIterations: 10</li>
         *   <li>systemPrompt: {@link DefaultSystemPrompt#PROMPT}</li>
         *   <li>tools: empty list (built-in tools added during build)</li>
         *   <li>skills: empty list</li>
         *   <li>hooks: empty {@link AgentHooks}</li>
         * </ul>
         */
        public Builder() {}

        private String apiKey;
        private String baseUrl;
        private String model;
        private Double temperature;
        private int maxIterations = 10;
        private String systemPrompt = DefaultSystemPrompt.PROMPT;

        private LlmClient customLlmClient;
        private Memory memory;
        private SessionLockManager lockManager;
        private final List<Tool> tools = new ArrayList<>();
        private final List<Skill> skills = new ArrayList<>();
        private final AgentHooks hooks = new AgentHooks();
        private LoadSkillsTool loadSkillsTool;

        /**
         * Sets the API key for the default OpenAI client.
         * <p>
         * Required unless {@link #llmClient(LlmClient)} is used to provide
         * a custom LLM client implementation.
         *
         * @param apiKey the OpenAI API key
         * @return this builder for method chaining
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets a custom base URL for the OpenAI API.
         * <p>
         * This allows routing requests to OpenAI-compatible endpoints like
         * Groq, Together, or self-hosted models.
         *
         * @param baseUrl the custom base URL (e.g., "https://api.groq.com/openai/v1")
         * @return this builder for method chaining
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the model identifier for LLM requests.
         * <p>
         * If not set, defaults to "gpt-4o-mini".
         *
         * @param model the model name (e.g., "gpt-4o", "gpt-4o-mini", "gpt-4-turbo")
         * @return this builder for method chaining
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the temperature for LLM sampling.
         * <p>
         * Higher values (e.g., 0.8) make output more random and creative,
         * while lower values (e.g., 0.2) make it more focused and deterministic.
         *
         * @param temperature the temperature value between 0.0 and 2.0
         * @return this builder for method chaining
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * Sets the maximum number of agent iterations.
         * <p>
         * Each iteration consists of an LLM call and any tool executions it triggers.
         * If not set, defaults to 10.
         *
         * @param maxIterations the maximum number of iterations before stopping
         * @return this builder for method chaining
         */
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        /**
         * Sets the system prompt for the agent.
         * <p>
         * If not set, uses the default system prompt from {@link DefaultSystemPrompt}.
         *
         * @param systemPrompt the system prompt text
         * @return this builder for method chaining
         */
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets a custom LLM client implementation.
         * <p>
         * When set, the default OpenAI client is not used. This allows integration
         * with custom LLM providers or testing with mock clients.
         *
         * @param client the custom LLM client implementation
         * @return this builder for method chaining
         */
        public Builder llmClient(LlmClient client) {
            this.customLlmClient = client;
            return this;
        }

        /**
         * Sets the memory implementation for conversation history.
         * <p>
         * If not set, defaults to {@link InMemoryStore} with a limit of 100 messages per session.
         *
         * @param memory the memory implementation
         * @return this builder for method chaining
         */
        public Builder memory(Memory memory) {
            this.memory = memory;
            return this;
        }

        /**
         * Sets the session lock manager for concurrent request handling.
         * <p>
         * If not set, defaults to {@link PerSessionLockManager}.
         *
         * @param lockManager the session lock manager implementation
         * @return this builder for method chaining
         */
        public Builder sessionLockManager(SessionLockManager lockManager) {
            this.lockManager = lockManager;
            return this;
        }

        /**
         * Adds custom tools to the agent's tool registry.
         * <p>
         * Built-in tools ({@link CurrentTimeTool}, {@link JavaScriptTool}) are
         * always included automatically.
         *
         * @param toolsToAdd the tools to add (null values are ignored)
         * @return this builder for method chaining
         */
        public Builder tools(Tool... toolsToAdd) {
            for (Tool t : toolsToAdd) {
                if (t != null) {
                    this.tools.add(t);
                }
            }
            return this;
        }

        /**
         * Adds skills from a skill loader to the agent.
         * <p>
         * Skills are multi-step instruction blocks that can be lazily loaded
         * by the LLM via the {@link LoadSkillsTool} to save tokens.
         *
         * @param loader the skill loader containing skills to register
         * @return this builder for method chaining
         */
        public Builder skills(SkillLoader loader) {
            if (loader != null && !loader.getSkills().isEmpty()) {
                this.skills.addAll(loader.getSkills());
                this.loadSkillsTool = new LoadSkillsTool(this.skills);
            }
            return this;
        }

        /**
         * Sets a callback invoked when the agent starts thinking (before LLM call).
         *
         * @param onThinking the callback to run when agent starts thinking
         * @return this builder for method chaining
         */
        public Builder onThinking(Runnable onThinking) {
            this.hooks.setOnThinking(onThinking);
            return this;
        }

        /**
         * Sets a callback invoked when the agent calls a tool.
         * <p>
         * The callback receives the tool name and its arguments as JSON.
         *
         * @param onToolCall the callback consuming tool name and arguments
         * @return this builder for method chaining
         */
        public Builder onToolCall(BiConsumer<String, String> onToolCall) {
            this.hooks.setOnToolCall(onToolCall);
            return this;
        }

        /**
         * Sets a callback invoked when a tool execution completes.
         * <p>
         * The callback receives the tool name and its execution result.
         *
         * @param onToolResult the callback consuming tool name and result
         * @return this builder for method chaining
         */
        public Builder onToolResult(BiConsumer<String, ToolResult> onToolResult) {
            this.hooks.setOnToolResult(onToolResult);
            return this;
        }

        /**
         * Sets a callback invoked for each token in the LLM's streaming response.
         * <p>
         * This enables real-time display of the agent's output as it's generated.
         *
         * @param onToken the callback consuming each token string
         * @return this builder for method chaining
         */
        public Builder onToken(Consumer<String> onToken) {
            this.hooks.setOnToken(onToken);
            return this;
        }

        /**
         * Sets a callback invoked when the agent completes successfully.
         *
         * @param onComplete the callback to run on completion
         * @return this builder for method chaining
         */
        public Builder onComplete(Runnable onComplete) {
            this.hooks.setOnComplete(onComplete);
            return this;
        }

        /**
         * Sets a callback invoked when an error occurs during agent execution.
         *
         * @param onError the callback consuming the error
         * @return this builder for method chaining
         */
        public Builder onError(Consumer<Throwable> onError) {
            this.hooks.setOnError(onError);
            return this;
        }

        /**
         * Builds and returns the configured ZeroAgent agent instance.
         * <p>
         * This method validates the configuration, applies defaults for unset values,
         * and constructs the immutable agent instance ready for use.
         *
         * @return the configured ZeroAgent agent
         * @throws IllegalArgumentException if API key is not set and no custom LLM client is provided
         */
        public ZeroAgent build() {
            LlmClient client = this.customLlmClient;
            if (client == null) {
                client = OpenAiChatClient.builder()
                        .apiKey(this.apiKey)
                        .baseUrl(this.baseUrl)
                        .build();
            }

            Memory mem = this.memory;
            if (mem == null) {
                mem = new InMemoryStore();
            }

            List<Tool> allTools = new ArrayList<>(this.tools);

            // Auto-register built-in tools
            allTools.add(new CurrentTimeTool());
            allTools.add(new JavaScriptTool());

            // Add LoadSkillsTool if skills were registered
            if (this.loadSkillsTool != null) {
                allTools.add(this.loadSkillsTool);
            }

            ToolRouter router = ToolRouter.create(allTools);
            AgentConfig config = new AgentConfig(this.model, this.temperature, this.maxIterations, this.systemPrompt);

            // Create session lock manager for sequential processing per session
            SessionLockManager lockMgr = this.lockManager != null ? this.lockManager : new PerSessionLockManager();

            return new ZeroAgent(client, router, mem, new ArrayList<>(this.skills), config, this.hooks, lockMgr);
        }
    }
}
