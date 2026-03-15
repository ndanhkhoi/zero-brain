package com.github.ndanhkhoi.zeroagent.concurrent;

/**
 * Manages exclusive locks per session to ensure sequential processing.
 * <p>
 * This interface provides a mechanism to acquire and release locks for specific
 * session identifiers, ensuring that messages for the same session are processed
 * sequentially while allowing different sessions to process in parallel.
 * <p>
 * Implementations must be thread-safe and handle concurrent lock requests correctly.
 * The lock operation should block the calling thread until the lock is available.
 * <p>
 * Example usage:
 * <pre>{@code
 * SessionLockManager lockManager = new PerSessionLockManager();
 * lockManager.lock("session-123");
 * try {
 *     // Process message for session-123
 * } finally {
 *     lockManager.unlock("session-123");
 * }
 * }</pre>
 *
 * @see PerSessionLockManager
 */
public interface SessionLockManager {
    /**
     * Acquires exclusive lock for the given session.
     * <p>
     * If the lock is currently held by another thread, this method will block
     * the calling thread until the lock becomes available.
     * <p>
     * This operation is reentrant - if the same thread already holds the lock
     * for this session, calling this method again will succeed immediately
     * (depending on implementation).
     *
     * @param sessionId The unique session identifier to lock. Must not be null.
     */
    void lock(String sessionId);

    /**
     * Releases the lock for the given session.
     * <p>
     * If the current thread does not hold the lock for this session, this method
     * should do nothing (safe no-op) rather than throwing an exception.
     * <p>
     * Implementations should verify that the lock is held by the current thread
     * before releasing to prevent accidental unlocking by other threads.
     *
     * @param sessionId The unique session identifier to unlock. Must not be null.
     */
    void unlock(String sessionId);
}
