# 1. Core Concepts

ZeroAgent is built around a few central concepts that make it both simple to use and powerful to extend.

## 🔄 The Agent Loop (`AgentLoop`)

The core engine of ZeroAgent is an iterative conversation loop. When you call `agent.chat()`, the loop performs the following sequence:

1. **Context Assembly**: Gathers the System Prompt, registered Skills (as instructions), and previous Conversation History from `Memory`.
2. **LLM Request**: Streams these messages to the `LlmClient` to get the assistant's response.
3. **Execution**:
   - If the LLM generates normal text, it streams directly to your console (via `onToken`).
   - If the LLM decides to call a Tool (e.g., "get the weather"), the loop intercepts the JSON arguments, executes the corresponding `Tool`, appends the result to `Memory`, and restarts from Step 1.
4. **Completion**: The loop exits when the LLM finishes responding without invoking any more tools, or when the `maxIterations` limit is reached.

---

## 🔌 Reusable Providers (`LlmClient`)

ZeroAgent is designed to communicate via the generic `LlmClient` interface. Currently, it ships with `OpenAiChatClient`, which wraps the official OpenAI Java SDK (`v4.26`).

Because `LlmClient` expects standard `ChatCompletionChunk` objects, you can:
- **Change Models**: Use `gpt-4o`, `gpt-4o-mini`, or any supported OpenAI models.
- **Provider Switching**: Use any LLM backend that provides an OpenAI-compatible API endpoint (like **Groq**, **Together AI**, or a local **vLLM** cluster).

```java
// Switching LLM provider at runtime
ZeroAgent agent = ZeroAgent.builder().build();

agent.setLlmClient(OpenAiChatClient.builder()
    .baseUrl("https://api.groq.com/openai/v1") // Use Groq!
    .apiKey(System.getenv("GROQ_API_KEY"))
    .build());
```

---

## 🪝 Event Hooks (`AgentHooks`)

To maintain transparency during the otherwise black-box LLM streaming process, `ZeroAgent.Builder` accepts several lifecycle hooks:

- `.onThinking(Runnable)`: Triggered right before launching a network request to the LLM.
- `.onToken(Consumer<String>)`: Yields each chunk of streamed text directly as it arrives. Great for printing real-time console UX.
- `.onToolCall(BiConsumer<String, String>)`: Fires when the LLM requests to run a tool. Provides the tool's `name` and JSON `arguments`.
- `.onToolResult(BiConsumer<String, ToolResult>)`: Fires exactly after the tool finishes running, allowing you to intercept success or error states.
- `.onComplete(Runnable)`: Triggered when the `chat()` iteration loop concludes successfully.
- `.onError(Consumer<Throwable>)`: Catch-all for API exceptions, iteration limits, or network timeouts.

These hooks allow you to wire ZeroAgent deeply into UIs like Telegram bots, WebSocket streams, or terminal apps.
