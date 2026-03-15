/**
 * Memory storage and retrieval interfaces for conversation persistence.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides abstractions for storing and retrieving conversation history,
 * enabling agents to maintain context across multiple interactions. The memory system
 * supports session-based isolation, allowing concurrent agents to maintain separate
 * conversation histories.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.memory.Memory} - Core interface for
 *       conversation storage and retrieval with session-based isolation</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.memory.InMemoryStore} - Default
 *       in-memory implementation suitable for single-instance deployments</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create memory store
 * Memory memory = new InMemoryStore();
 *
 * // Store messages in a session
 * String sessionId = "user-session-123";
 * memory.addMessage(sessionId, userMessage);
 * memory.addMessage(sessionId, assistantMessage);
 *
 * // Retrieve conversation history
 * List<ChatCompletionMessageParam> history = memory.getMessages(sessionId);
 *
 * // Use in agent for context-aware conversations
 * AgentLoop loop = new AgentLoop(llmClient, toolRouter, memory, skills, config, hooks, skillCache, lockManager);
 * AgentResponse response = loop.chat(sessionId, "What did we discuss earlier?");
 * }</pre>
 *
 * <h2>Memory Storage Strategies</h2>
 * <p>
 * <b>In-Memory (Default):</b> Fast, suitable for single-instance deployments and testing.
 * Data is lost on restart.
 * </p>
 * <p>
 * <b>Persistent Storage:</b> For production deployments, implement {@link Memory}
 * with a backing store like:
 * </p>
 * <ul>
 *   <li>Redis - Fast, distributed caching</li>
 *   <li>Database - PostgreSQL, MongoDB with proper indexing</li>
 *   <li>File-based - JSON, Parquet for archiving</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link Memory} implementations must be thread-safe for concurrent access across
 * multiple sessions. The default {@link InMemoryStore} is thread-safe and uses
 * concurrent data structures.
 * </p>
 *
 * <h2>Extension Points</h2>
 * <p>
 * To implement persistent memory storage:
 * </p>
 * <pre>{@code
 * public class RedisMemoryStore implements Memory {
 *     private final RedisClient redis;
 *
 *     public RedisMemoryStore(RedisClient redis) {
 *         this.redis = redis;
 *     }
 *
 *     @Override
 *     public void addMessage(String sessionId, ChatCompletionMessageParam message) {
 *         String key = "session:" + sessionId + ":messages";
 *         redis.rpush(key, serialize(message));
 *         redis.expire(key, Duration.ofDays(7)); // Expire after 7 days
 *     }
 *
 *     @Override
 *     public List<ChatCompletionMessageParam> getMessages(String sessionId) {
 *         String key = "session:" + sessionId + ":messages";
 *         return redis.lrange(key, 0, -1).stream()
 *             .map(this::deserialize)
 *             .collect(Collectors.toList());
 *     }
 * }
 * }</pre>
 *
 * @see com.github.ndanhkhoi.zeroagent.agent.AgentLoop
 */
package com.github.ndanhkhoi.zeroagent.memory;
