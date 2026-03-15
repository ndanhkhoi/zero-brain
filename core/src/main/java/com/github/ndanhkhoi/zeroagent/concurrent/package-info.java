/**
 * Session-based locking and concurrency control for agent execution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides mechanisms for ensuring sequential message processing within
 * each session while allowing different sessions to execute concurrently. This is
 * critical for maintaining conversation integrity when multiple requests arrive
 * simultaneously for the same session.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.concurrent.SessionLockManager} - Interface
 *       defining session-based locking operations</li>
 *   <li>{@link com.github.ndanhkhoi.zeroagent.concurrent.PerSessionLockManager} - Default
 *       implementation using per-session {@link java.util.concurrent.locks.ReentrantLock}
 *       instances for optimal concurrency</li>
 * </ul>
 *
 * <h2>Problem Statement</h2>
 * <p>
 * Without session-based locking, concurrent requests for the same session could:
 * </p>
 * <ul>
 *   <li>Interleave messages, breaking conversation flow</li>
 *   <li>Corrupt memory stores with race conditions</li>
 *   <li>Generate conflicting tool calls</li>
 *   <li>Produce incoherent responses</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create lock manager
 * SessionLockManager lockManager = new PerSessionLockManager();
 *
 * // Agent loop automatically handles locking
 * AgentLoop loop = new AgentLoop(
 *     llmClient,
 *     toolRouter,
 *     memory,
 *     skills,
 *     config,
 *     hooks,
 *     skillCache,
 *     lockManager  // Ensures sequential processing per session
 * );
 *
 * // Concurrent requests for same session are processed sequentially
 * CompletableFuture.supplyAsync(() -> loop.chat("session-1", "Hello"));
 * CompletableFuture.supplyAsync(() -> loop.chat("session-1", "Wait, first question"));
 * // Second request waits for first to complete
 *
 * // Different sessions process in parallel
 * CompletableFuture.supplyAsync(() -> loop.chat("session-2", "Hi"));
 * CompletableFuture.supplyAsync(() -> loop.chat("session-3", "Hello"));
 * // These execute concurrently with session-1 and each other
 * }</pre>
 *
 * <h2>Implementation Details</h2>
 * <p>
 * {@link PerSessionLockManager} uses:
 * </p>
 * <ul>
 *   <li>{@link java.util.concurrent.ConcurrentHashMap} for thread-safe lock storage</li>
 *   <li>Per-session {@link java.util.concurrent.locks.ReentrantLock} instances</li>
 *   <li>Atomic lock creation via {@link java.util.concurrent.ConcurrentHashMap#computeIfAbsent}</li>
 *   <li>Ownership verification via {@link java.util.concurrent.locks.ReentrantLock#isHeldByCurrentThread()}</li>
 * </ul>
 *
 * <h2>Memory Considerations</h2>
 * <p>
 * Locks are created on-demand and never removed. For applications with many unique
 * sessions, this may lead to unbounded memory growth. Future enhancements should
 * implement lock cleanup for sessions inactive for a configurable period.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All implementations of {@link SessionLockManager} must be thread-safe. The default
 * {@link PerSessionLockManager} uses concurrent data structures and proper synchronization
 * to ensure thread safety.
 * </p>
 *
 * <h2>Extension Points</h2>
 * <p>
 * For distributed deployments, implement {@link SessionLockManager} with:
 * </p>
 * <ul>
 *   <li>Redis distributed locks (Redlock algorithm)</li>
 *   <li>Database row-level locking</li>
 *   <li>ZooKeeper/Etcd coordination</li>
 * </ul>
 *
 * @see com.github.ndanhkhoi.zeroagent.agent.AgentLoop
 * @see java.util.concurrent.locks.ReentrantLock
 */
package com.github.ndanhkhoi.zeroagent.concurrent;
