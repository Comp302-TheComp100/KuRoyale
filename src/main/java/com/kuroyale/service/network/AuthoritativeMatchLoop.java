package com.kuroyale.service.network;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.logic.GameState;
import com.kuroyale.model.logic.ElixirManager;
import com.kuroyale.model.dto.NetworkGameStateSnapshot;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * AUTHORITATIVE MATCH LOOP
 * 
 * This is the single source of truth for the match. It runs independently of network
 * conditions and NEVER blocks waiting for client input or acknowledgment.
 * 
 * Key Design Principles:
 * 1. The loop runs at a fixed tick rate (e.g., 20 ticks/second = 50ms per tick)
 * 2. Network I/O is completely async - the loop never waits for packets
 * 3. Missing inputs simply mean "no action this tick" - game continues
 * 4. State snapshots are broadcast periodically for client reconciliation
 * 5. High ping clients catch up via snapshots, they don't stall the match
 * 
 * Architecture:
 * - ScheduledExecutorService drives the tick loop (non-blocking)
 * - InputQueue provides player inputs (non-blocking poll)
 * - StateReplicator broadcasts state (async, fire-and-forget)
 */
public class AuthoritativeMatchLoop {
    
    // Tick rate configuration
    public static final int TICKS_PER_SECOND = 20;
    public static final long TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND; // 50ms
    public static final double TICK_DELTA_TIME = 1.0 / TICKS_PER_SECOND; // 0.05s
    
    // Snapshot broadcast rate (every N ticks)
    private static final int SNAPSHOT_BROADCAST_INTERVAL = 2; // Every 2 ticks = 10 snapshots/sec
    
    // Core components
    private final GameState gameState;
    private final NetworkInputQueue inputQueue;
    private final ScheduledExecutorService tickExecutor;
    
    // State tracking
    private final AtomicLong currentTick = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    
    // Callbacks
    private Consumer<NetworkGameStateSnapshot> onStateSnapshot;
    private Consumer<MatchEvent> onMatchEvent;
    private Runnable onMatchEnd;
    
    // Instrumentation
    private long lastTickTimeMs = 0;
    private long tickProcessingTimeNs = 0;
    private int ticksSinceLastSnapshot = 0;
    
    // Card catalog for spawning
    private java.util.function.Function<String, Card> cardCatalog;
    
    public AuthoritativeMatchLoop(GameState gameState) {
        this.gameState = gameState;
        this.inputQueue = new NetworkInputQueue();
        this.tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MatchLoop-Authoritative");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Starts the authoritative match loop.
     * The loop will run at a fixed tick rate, completely independent of network conditions.
     */
    public void start() {
        if (running.getAndSet(true)) {
            return; // Already running
        }
        
        System.out.println("[MatchLoop] Starting authoritative loop at " + TICKS_PER_SECOND + " ticks/sec");
        lastTickTimeMs = System.currentTimeMillis();
        
        // Schedule the tick loop at fixed rate
        // This NEVER blocks - it runs every TICK_INTERVAL_MS regardless of network state
        tickExecutor.scheduleAtFixedRate(
            this::executeTick,
            0,
            TICK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stops the match loop.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return; // Already stopped
        }
        
        System.out.println("[MatchLoop] Stopping authoritative loop at tick " + currentTick.get());
        tickExecutor.shutdown();
        try {
            tickExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            tickExecutor.shutdownNow();
        }
    }
    
    /**
     * Pauses the match (e.g., for disconnect handling).
     * The loop continues running but doesn't advance game state.
     */
    public void pause() {
        paused.set(true);
        System.out.println("[MatchLoop] Paused at tick " + currentTick.get());
    }
    
    /**
     * Resumes a paused match.
     */
    public void resume() {
        paused.set(false);
        System.out.println("[MatchLoop] Resumed at tick " + currentTick.get());
    }
    
    /**
     * Executes a single tick of the authoritative simulation.
     * This method is called at a fixed rate and NEVER blocks.
     */
    private void executeTick() {
        if (!running.get()) return;
        
        long tickStart = System.nanoTime();
        long tick = currentTick.incrementAndGet();
        
        try {
            // 1. Process pending inputs (NON-BLOCKING)
            processInputs();
            
            // 2. Advance simulation (if not paused)
            if (!paused.get()) {
                advanceSimulation();
            }
            
            // 3. Check for game end
            if (gameState.isGameOver()) {
                handleGameEnd();
            }
            
            // 4. Broadcast state snapshot (periodic)
            ticksSinceLastSnapshot++;
            if (ticksSinceLastSnapshot >= SNAPSHOT_BROADCAST_INTERVAL) {
                broadcastStateSnapshot();
                ticksSinceLastSnapshot = 0;
            }
            
        } catch (Exception e) {
            System.err.println("[MatchLoop] Error in tick " + tick + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Instrumentation
        tickProcessingTimeNs = System.nanoTime() - tickStart;
        lastTickTimeMs = System.currentTimeMillis();
        
        // Log performance warning if tick took too long
        if (tickProcessingTimeNs > TICK_INTERVAL_MS * 1_000_000 * 0.8) {
            System.out.println("[MatchLoop] WARNING: Tick " + tick + " took " + 
                (tickProcessingTimeNs / 1_000_000) + "ms (budget: " + TICK_INTERVAL_MS + "ms)");
        }
    }
    
    /**
     * Processes all pending player inputs.
     * This is NON-BLOCKING - if no inputs are available, it returns immediately.
     */
    private void processInputs() {
        // Process Player 1 (Host) inputs
        PlayerInput input;
        while ((input = inputQueue.pollPlayer1Input()) != null) {
            applyInput(input, true);
        }
        
        // Process Player 2 (Client) inputs
        while ((input = inputQueue.pollPlayer2Input()) != null) {
            applyInput(input, false);
        }
    }
    
    /**
     * Applies a validated player input to the game state.
     */
    private void applyInput(PlayerInput input, boolean isPlayer1) {
        if (input.getType() == PlayerInput.InputType.CARD_DEPLOY) {
            Card card = cardCatalog != null ? cardCatalog.apply(input.getCardName()) : null;
            if (card == null) {
                System.out.println("[MatchLoop] Unknown card: " + input.getCardName());
                return;
            }
            
            // Validate elixir cost
            ElixirManager elixir = isPlayer1 ? gameState.getPlayerElixir() : gameState.getBotElixir();
            if (elixir.getCurrentElixir() < card.getCost()) {
                System.out.println("[MatchLoop] Insufficient elixir for " + input.getCardName());
                return;
            }
            
            // Apply the card placement
            // Note: For player 2, coordinates are in their local space and need to be mirrored
            int x = input.getX();
            int y = input.getY();
            
            if (!isPlayer1) {
                // Mirror Y coordinate for player 2 (their bottom is our top)
                y = (Arena.HEIGHT - 1) - y;
            }
            
            gameState.placeCard(isPlayer1, card, x, y);
            elixir.spend(card.getCost());
            
            System.out.println("[MatchLoop] Applied " + (isPlayer1 ? "P1" : "P2") + 
                " card: " + input.getCardName() + " at (" + x + ", " + y + ")");
        }
    }
    
    /**
     * Advances the game simulation by one tick.
     * This includes elixir generation, unit movement, combat, etc.
     */
    private void advanceSimulation() {
        gameState.update(TICK_DELTA_TIME);
    }
    
    /**
     * Broadcasts a state snapshot to all clients.
     * This is ASYNC and fire-and-forget - it never blocks the loop.
     */
    private void broadcastStateSnapshot() {
        if (onStateSnapshot == null) return;
        
        NetworkGameStateSnapshot snapshot = new NetworkGameStateSnapshot(gameState, currentTick.get());
        
        // Fire-and-forget broadcast
        CompletableFuture.runAsync(() -> {
            try {
                onStateSnapshot.accept(snapshot);
            } catch (Exception e) {
                System.err.println("[MatchLoop] Error broadcasting snapshot: " + e.getMessage());
            }
        });
    }
    
    /**
     * Handles game end.
     */
    private void handleGameEnd() {
        System.out.println("[MatchLoop] Game ended at tick " + currentTick.get());
        running.set(false);
        
        // Broadcast final state
        broadcastStateSnapshot();
        
        if (onMatchEnd != null) {
            onMatchEnd.run();
        }
    }
    
    // ==================== Input Queue Access ====================
    
    /**
     * Queues an input from Player 1 (Host).
     * This is thread-safe and non-blocking.
     */
    public void queuePlayer1Input(PlayerInput input) {
        inputQueue.queuePlayer1Input(input);
    }
    
    /**
     * Queues an input from Player 2 (Client).
     * This is thread-safe and non-blocking.
     */
    public void queuePlayer2Input(PlayerInput input) {
        inputQueue.queuePlayer2Input(input);
    }
    
    // ==================== Configuration ====================
    
    public void setCardCatalog(java.util.function.Function<String, Card> catalog) {
        this.cardCatalog = catalog;
    }
    
    public void setOnStateSnapshot(Consumer<NetworkGameStateSnapshot> callback) {
        this.onStateSnapshot = callback;
    }
    
    public void setOnMatchEvent(Consumer<MatchEvent> callback) {
        this.onMatchEvent = callback;
    }
    
    public void setOnMatchEnd(Runnable callback) {
        this.onMatchEnd = callback;
    }
    
    // ==================== Instrumentation ====================
    
    public long getCurrentTick() {
        return currentTick.get();
    }
    
    public long getLastTickTimeMs() {
        return lastTickTimeMs;
    }
    
    public long getTickProcessingTimeNs() {
        return tickProcessingTimeNs;
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public boolean isPaused() {
        return paused.get();
    }
    
    public NetworkInputQueue getInputQueue() {
        return inputQueue;
    }
    
    // ==================== Match Event ====================
    
    public static class MatchEvent {
        public enum Type {
            CARD_DEPLOYED,
            TOWER_DAMAGED,
            TOWER_DESTROYED,
            GAME_OVER
        }
        
        private final Type type;
        private final String data;
        private final long tick;
        
        public MatchEvent(Type type, String data, long tick) {
            this.type = type;
            this.data = data;
            this.tick = tick;
        }
        
        public Type getType() { return type; }
        public String getData() { return data; }
        public long getTick() { return tick; }
    }
}
