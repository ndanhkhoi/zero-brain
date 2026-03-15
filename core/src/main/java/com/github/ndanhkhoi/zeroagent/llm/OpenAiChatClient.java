package com.github.ndanhkhoi.zeroagent.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.stream.Stream;

/**
 * Concrete implementation of {@link LlmClient} built explicitly around the official
 * OpenAI Java SDK.
 *
 * <p>This client strictly streams responses. It also supports configuration of custom
 * base URLs to enable routing requests to OpenAI-compatible endpoints like Groq or Together.
 */
public class OpenAiChatClient implements LlmClient {
    private final OpenAIClient client;

    /**
     * Creates a new OpenAiChatClient with the specified OpenAI client.
     *
     * @param client the configured OpenAI client instance
     */
    private OpenAiChatClient(OpenAIClient client) {
        this.client = client;
    }

    @Override
    public Stream<ChatCompletionChunk> chatStream(ChatCompletionCreateParams params) {
        StreamResponse<ChatCompletionChunk> response =
                client.chat().completions().createStreaming(params);
        return response.stream().onClose(response::close);
    }

    /**
     * Creates a new builder for configuring an OpenAiChatClient.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring and creating OpenAiChatClient instances.
     * <p>
     * This builder allows customization of the OpenAI API key and base URL.
     */
    public static class Builder {
        /**
         * Creates a new Builder with empty configuration.
         */
        public Builder() {}

        private String apiKey;
        private String baseUrl;

        /**
         * Sets the OpenAI API key.
         * <p>
         * This is a required parameter for authentication.
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
         * This allows routing requests to OpenAI-compatible endpoints.
         *
         * @param baseUrl the custom base URL (e.g., "<a href="https://api.groq.com/openai/v1">...</a>")
         * @return this builder for method chaining
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Builds and returns the configured OpenAiChatClient instance.
         *
         * @return the configured OpenAiChatClient
         * @throws IllegalArgumentException if API key is not provided or is empty
         */
        public OpenAiChatClient build() {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API Key must be provided.");
            }

            OpenAIOkHttpClient.Builder clientBuilder =
                    OpenAIOkHttpClient.builder().apiKey(apiKey);

            if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                clientBuilder.baseUrl(baseUrl);
            }

            return new OpenAiChatClient(clientBuilder.build());
        }
    }
}
