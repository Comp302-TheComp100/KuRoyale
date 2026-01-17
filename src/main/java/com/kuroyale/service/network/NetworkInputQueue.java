package com.kuroyale.service.network;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NETWORK INPUT QUEUE
 * 
 * Non-blocking, thread-safe input queue for player actions.
 * 
 * Key Design:
 * 1. Uses lock-free concurrent queues for each player
 * 2. All operations are O(1) and never block
 * 3. The authoritative loop polls these queues without waiting
 * 4. Network threads can push inputs at any time
 * 
 * If no input arrives from a player, that player simply takes no action
 * that tick - the game continues regardless.
 */
public class NetworkInputQueue {
    
    // Per-player input queues (lock-free, thread-safe)
    private final ConcurrentLinkedQueue<PlayerInput> player1Queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PlayerInput> player2Queue = new ConcurrentLinkedQueue<>();
    
    // Input sequence tracking (for ordering and dedup)
    private final AtomicLong player1LastSeq = new AtomicLong(-1);
    private final AtomicLong player2LastSeq = new AtomicLong(-1);
    
    // Statistics
    private final AtomicLong player1InputCount = new AtomicLong(0);
    private final AtomicLong player2InputCount = new AtomicLong(0);
    private final AtomicLong droppedInputCount = new AtomicLong(0);
    
    /**
     * Queues an input from Player 1.
     * Thread-safe, non-blocking, O(1).
     */
    public void queuePlayer1Input(PlayerInput input) {
        if (input == null) return;
        
        // Check sequence to avoid duplicates or out-of-order
        long seq = input.getSequence();
        long lastSeq = player1LastSeq.get();
        
        if (seq <= lastSeq) {
            // Duplicate or out-of-order - drop it
            droppedInputCount.incrementAndGet();
            return;
        }
        
        // Update last seen sequence
        player1LastSeq.set(seq);
        
        // Add to queue
        player1Queue.offer(input);
        player1InputCount.incrementAndGet();
    }
    
    /**
     * Queues an input from Player 2.
     * Thread-safe, non-blocking, O(1).
     */
    public void queuePlayer2Input(PlayerInput input) {
        if (input == null) return;
        
        // Check sequence to avoid duplicates or out-of-order
        long seq = input.getSequence();
        long lastSeq = player2LastSeq.get();
        
        if (seq <= lastSeq) {
            // Duplicate or out-of-order - drop it
            droppedInputCount.incrementAndGet();
            return;
        }
        
        // Update last seen sequence
        player2LastSeq.set(seq);
        
        // Add to queue
        player2Queue.offer(input);
        player2InputCount.incrementAndGet();
    }
    
    /**
     * Polls an input from Player 1's queue.
     * Returns null if queue is empty.
     * Non-blocking, O(1).
     */
    public PlayerInput pollPlayer1Input() {
        return player1Queue.poll();
    }
    
    /**
     * Polls an input from Player 2's queue.
     * Returns null if queue is empty.
     * Non-blocking, O(1).
     */
    public PlayerInput pollPlayer2Input() {
        return player2Queue.poll();
    }
    
    /**
     * Checks if Player 1 has pending inputs.
     */
    public boolean hasPlayer1Input() {
        return !player1Queue.isEmpty();
    }
    
    /**
     * Checks if Player 2 has pending inputs.
     */
    public boolean hasPlayer2Input() {
        return !player2Queue.isEmpty();
    }
    
    /**
     * Gets the number of pending inputs for Player 1.
     */
    public int getPlayer1QueueSize() {
        return player1Queue.size();
    }
    
    /**
     * Gets the number of pending inputs for Player 2.
     */
    public int getPlayer2QueueSize() {
        return player2Queue.size();
    }
    
    /**
     * Clears all pending inputs.
     */
    public void clear() {
        player1Queue.clear();
        player2Queue.clear();
    }
    
    // ==================== Statistics ====================
    
    public long getPlayer1InputCount() {
        return player1InputCount.get();
    }
    
    public long getPlayer2InputCount() {
        return player2InputCount.get();
    }
    
    public long getDroppedInputCount() {
        return droppedInputCount.get();
    }
    
    public long getPlayer1LastSequence() {
        return player1LastSeq.get();
    }
    
    public long getPlayer2LastSequence() {
        return player2LastSeq.get();
    }
}
