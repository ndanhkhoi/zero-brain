# 3. Advanced Usage

ZeroAgent allows complete customization over memory structure, multi-modal Vision integrations, and Runtime Model APIs.

---

## 🧠 Custom Memory Engines

ZeroAgent tracks conversations per `sessionId`. By default, it uses `InMemoryStore` which trims messages once the context history reaches a 100-message boundary.

If you are deploying ZeroAgent in a distributed environment (Spring Boot, K8s, NodeJS lambda wrappers, etc.), your sessions need persistent storage like PostgreSQL, MongoDB, or Redis.

You can wire in your own persistence by implementing the `io.zeroagent.memory.Memory` interface:

```java
import com.github.ndanhkhoi.zeroagent.memory.Memory;
import com.openai.models.chat.completions.ChatCompletionMessageParam;

public class RedisMemoryStore implements Memory {
    @Override
    public void addMessage(String sessionId, ChatCompletionMessageParam msg) {
        // convert msg JSON to Redis HSET or append to LIST
    }

    @Override
    public List<ChatCompletionMessageParam> getMessages(String sessionId) {
        // read Redis LIST, parse into ChatCompletionMessageParam format
        return Collections.emptyList();
    }

    @Override
    public void clear(String sessionId) {
        // delete from Redis
    }
}
```

Then attach it during construction:
```java
ZeroAgent agent = ZeroAgent.builder().memory(new RedisMemoryStore()).build();
```

---

## 🌎 Runtime Provider Switching

Are you testing Anthropic models, Cohere, or routing prompts locally via `vLLM`/`Ollama`?

ZeroAgent runs pure `OpenAiChatClient` internally relying on `openai-java:4.26.0`, which dynamically forwards paths. Because `LlmClient` is an arbitrary interface, you can substitute the URL endpoint or the provider ENTIRELY at Runtime.

```java
// Default is OpenAI
ZeroAgent agent = ZeroAgent.builder()
    .model("gpt-4o")
    .apiKey("sk-proj-...")
    .build();

// Change to groq on-the-fly inside the application flow
LlmClient groqClient = OpenAiChatClient.builder()
    .baseUrl("https://api.groq.com/openai/v1") 
    .apiKey("gsk-...") 
    .build();

agent.setLlmClient(groqClient);
```

---

## 🖼️ Vision protocol

If you are passing PDFs, Screenshots, or Diagrams to the AI, use the fluent request builder to pass native InputStreams which get automatically formatted into Multi-Part Base64 payloads expected by OpenAI Chat Completions.

```java
import com.github.ndanhkhoi.zeroagent.agent.AgentResponse;

// Load raw byte streams
try(InputStream chartStream = Files.newInputStream(Path.of("chart.png"))) {

    // Append to chat using fluent builder
    AgentResponse response = agent.message("Look at this chart, summarize the outliers.")
        .sessionId("dashboard-session-2")
        .image(chartStream, "image/png")
        .send();
}
```

> [!WARNING]
> Keep images optimized before streaming! High-resolution diagrams can consume thousands of tokens instantly.
