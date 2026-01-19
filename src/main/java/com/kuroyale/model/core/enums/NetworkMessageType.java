package com.kuroyale.model.core.enums;

/**
 * Defines the types of messages that can be sent over the network.
 * Part of the network protocol for multiplayer synchronization.
 * 
 * Message Format: MESSAGE_TYPE|player_id|data|timestamp
 * Example: CARD_PLACED|1|Knight|5.2,3.8|00:45
 */
public enum NetworkMessageType {
    // Connection & Lobby Messages
    CONNECT("Player connection request"),
    CONNECT_ACK("Connection acknowledged by host"),
    DISCONNECT("Player disconnecting gracefully"),
    HEARTBEAT("Keep-alive ping"),

    // Lobby Messages
    PLAYER_INFO("Player name and deck info"),
    READY_STATUS("Player ready/not ready status"),
    LOBBY_UPDATE("Lobby state update"),
    MATCH_START("Match is starting"),

    // Game State Synchronization (Host-Authoritative Model)
    ARENA_LAYOUT("Arena layout synchronization from host"),
    GAME_STATE_SYNC("Full authoritative game state from host - includes all entities, scores, timers"),
    PLAYER_INPUT("Player input intent: seq:playerId:type:cardName:x:y:clientTime"),
    CARD_PLACED("Card placement on arena"),
    CLIENT_INPUT("Client input: card placement request sent to host for validation"),
    REQUEST_SNAPSHOT("Client requests full state snapshot for resync"),
    UNIT_MOVE("Unit movement update"),
    UNIT_ATTACK("Unit attacking"),
    TOWER_DAMAGED("Tower took damage"),
    TOWER_DESTROYED("Tower was destroyed"),
    ELIXIR_UPDATE("Elixir level changed"),
    TIMER_SYNC("Game timer synchronization"),
    TROOP_SYNC("Troop positions and health sync from host"),
    SCORE_SYNC("Score synchronization from host"),
    TOWER_SYNC("Tower health synchronization from host"),
    GAME_OVER("Game over with winner info from host"),
    FULL_STATE_SYNC("Complete entity state sync - troops, buildings, projectiles from host"),
    AREA_EFFECT("Area effect visual sync - explosions, splash damage effects"),
    SPELL_CAST("Spell cast visual sync - spell animations like arrows, zap"),

    // Match End Messages
    VICTORY("Player won the match"),
    DEFEAT("Player lost the match"),
    DRAW("Match ended in a draw"),

    // Error Messages
    ERROR("Error occurred"),
    RECONNECT("Attempting to reconnect"),
    OPPONENT_DISCONNECTED("Opponent has disconnected");

    private final String description;

    NetworkMessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
