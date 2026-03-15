package com.github.ndanhkhoi.zeroagent.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe implementation of {@link SessionLockManager} using per-session {@link ReentrantLock} instances.
 * <p>
 * This implementation maintains a map of session identifiers to locks, creating new locks
 * on-demand when first accessed. Each session has its own independent lock, allowing
 * different sessions to process messages in parallel while ensuring sequential processing
 * within each session.
 * <p>
 * <strong>Thread Safety:</strong>
 * <ul>
 *   <li>Uses {@link ConcurrentHashMap} for thread-safe access to the locks map</li>
 *   <li>{@link ReentrantLock#lock()} provides exclusive access per session</li>
 *   <li>Uses {@link ConcurrentHashMap#computeIfAbsent(Object, java.util.function.Function)}
 *       for atomic lock creation - no race condition when multiple threads create the same lock</li>
 *   <li>{@link ReentrantLock#isHeldByCurrentThread()} ensures locks are only released by the owning thread</li>
 * </ul>
 * <p>
 * <strong>Memory Management:</strong>
 * Locks are created on-demand and never removed from the map. In applications with
 * many unique sessions, this may lead to unbounded memory growth over time. Future
 * enhancements should implement lock cleanup for sessions inactive for a configurable period.
 * <p>
 * <strong>Example:</strong>
 * <pre>{@code
 * SessionLockManager lockManager = new PerSessionLockManager();
 *
 * // Thread 1
 * lockManager.lock("user-123");
 * try {
 *     // Process message - Thread 2 will block if it tries to lock "user-123"
 * } finally {
 *     lockManager.unlock("user-123");
 * }
 *
 * // Thread 2 (different session - no blocking)
 * lockManager.lock("user-456");  // Acquires immediately even if Thread 1 is running
 * }</pre>
 *
 * @see SessionLockManager
 * @see ReentrantLock
 */
public final class PerSessionLockManager implements SessionLockManager {

    /**
     * Creates a new PerSessionLockManager.
     */
    public PerSessionLockManager() {}

    /**
     * Map of session identifiers to their corresponding locks.
     * Uses ConcurrentHashMap for thread-safe access without explicit synchronization.
     * Locks are created on-demand using computeIfAbsent() for atomic creation.
     */
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Acquires exclusive lock for the given session.
     * <p>
     * If the lock for this session does not exist, it will be created atomically.
     * The calling thread will block until the lock is available.
     * <p>
     * Note: This method uses {@link ReentrantLock#lock()} which is not interruptible.
     * For interruptible locking, use {@link ReentrantLock#lockInterruptibly()} instead.
     *
     * @param sessionId The unique session identifier to lock. Must not be null.
     * @throws NullPointerException if sessionId is null
     */
    @Override
    public void lock(String sessionId) {
        ReentrantLock lock = locks.computeIfAbsent(sessionId, k -> new ReentrantLock());
        lock.lock();
    }

    /**
     * Releases the lock for the given session.
     * <p>
     * This method is safe to call even if:
     * <ul>
     *   <li>The session has no lock in the map (no-op)</li>
     *   <li>The lock exists but is not currently held (no-op)</li>
     *   <li>The lock is held by a different thread (no-op, not released)</li>
     * </ul>
     * <p>
     * The {@link ReentrantLock#isHeldByCurrentThread()} check ensures that locks
     * are only released by the thread that actually holds them, preventing accidental
     * unlock attempts from other threads.
     *
     * @param sessionId The unique session identifier to unlock. Must not be null.
     * @throws NullPointerException if sessionId is null
     */
    @Override
    public void unlock(String sessionId) {
        ReentrantLock lock = locks.get(sessionId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
