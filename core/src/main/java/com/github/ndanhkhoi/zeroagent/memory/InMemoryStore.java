package com.github.ndanhkhoi.zeroagent.memory;

import com.openai.models.chat.completions.ChatCompletionMessageParam;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A default thread-safe implementation of {@link Memory} using a ConcurrentHashMap backing.
 *
 * <p>This store retains all messages per session.
 */
public class InMemoryStore implements Memory {
    private final ConcurrentHashMap<String, Deque<ChatCompletionMessageParam>> store = new ConcurrentHashMap<>();

    /**
     * Creates a new InMemoryStore.
     */
    public InMemoryStore() {}

    @Override
    public void addMessage(String sessionId, ChatCompletionMessageParam message) {
        store.compute(sessionId, (k, v) -> {
            Deque<ChatCompletionMessageParam> deque = (v != null) ? v : new ConcurrentLinkedDeque<>();
            deque.addLast(message);
            return deque;
        });
    }

    @Override
    public List<ChatCompletionMessageParam> getMessages(String sessionId) {
        Deque<ChatCompletionMessageParam> deque = store.get(sessionId);
        if (deque == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(deque);
    }

    @Override
    public void clear(String sessionId) {
        store.remove(sessionId);
    }
}
