package com.kuroyale.model.dto;

import com.kuroyale.model.enums.NetworkMessageType;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NetworkMessage - Network Protocol Implementation.
 * Tests message creation, serialization, and parsing.
 */
class NetworkMessageTest {

    /**
     * Test Case 1: Create message and convert to protocol string
     * Verifies that messages are correctly formatted as protocol strings.
     */
    @Test
    @DisplayName("NetworkMessage should convert to correct protocol string format")
    void testToProtocolString() {
        // Arrange
        NetworkMessage message = new NetworkMessage(
            NetworkMessageType.CARD_PLACED,
            1,
            "Knight|5.2,3.8"
        );
        
        // Act
        String protocolString = message.toProtocolString();
        
        // Assert
        assertTrue(protocolString.startsWith("CARD_PLACED|1|Knight|5.2,3.8|"));
        assertEquals(NetworkMessageType.CARD_PLACED, message.getType());
        assertEquals(1, message.getPlayerId());
        assertEquals("Knight|5.2,3.8", message.getData());
        assertNotNull(message.getTimestamp());
    }

    /**
     * Test Case 2: Parse protocol string back to NetworkMessage
     * Verifies that protocol strings are correctly parsed into messages.
     */
    @Test
    @DisplayName("fromProtocolString should parse valid protocol strings")
    void testFromProtocolString() {
        // Arrange
        String protocolString = "TOWER_DAMAGED|2|CrownLeft|450|01:23";
        
        // Act
        NetworkMessage message = NetworkMessage.fromProtocolString(protocolString);
        
        // Assert
        assertNotNull(message);
        assertEquals(NetworkMessageType.TOWER_DAMAGED, message.getType());
        assertEquals(2, message.getPlayerId());
        assertEquals("CrownLeft|450", message.getData());
    }

    /**
     * Test Case 3: Factory method for card placement
     * Verifies the cardPlaced factory method creates correct messages.
     */
    @Test
    @DisplayName("cardPlaced factory should create correct message")
    void testCardPlacedFactory() {
        // Act
        NetworkMessage message = NetworkMessage.cardPlaced(1, "Giant", 8.5, 15.0);
        
        // Assert
        assertEquals(NetworkMessageType.CARD_PLACED, message.getType());
        assertEquals(1, message.getPlayerId());
        assertTrue(message.getData().contains("Giant"));
        assertTrue(message.getData().contains("8.5"));
        assertTrue(message.getData().contains("15.0"));
    }

    /**
     * Test Case 4: Factory method for tower damage
     * Verifies the towerDamaged factory method creates correct messages.
     */
    @Test
    @DisplayName("towerDamaged factory should create correct message")
    void testTowerDamagedFactory() {
        // Act
        NetworkMessage message = NetworkMessage.towerDamaged(2, "PrincessLeft", 250);
        
        // Assert
        assertEquals(NetworkMessageType.TOWER_DAMAGED, message.getType());
        assertEquals(2, message.getPlayerId());
        assertTrue(message.getData().contains("PrincessLeft"));
        assertTrue(message.getData().contains("250"));
    }

    /**
     * Test Case 5: Parse invalid protocol string returns null
     * Verifies that malformed protocol strings are handled gracefully.
     */
    @Test
    @DisplayName("fromProtocolString should return null for invalid strings")
    void testFromProtocolStringInvalid() {
        // Test various invalid inputs
        assertNull(NetworkMessage.fromProtocolString(null));
        assertNull(NetworkMessage.fromProtocolString(""));
        assertNull(NetworkMessage.fromProtocolString("INVALID"));
        assertNull(NetworkMessage.fromProtocolString("NOT_A_TYPE|1|data|00:00"));
    }

    /**
     * Test Case 6: Parse card placement data
     * Verifies that card placement data is correctly parsed from message.
     */
    @Test
    @DisplayName("parseCardPlacement should extract card name and coordinates")
    void testParseCardPlacement() {
        // Arrange
        NetworkMessage message = NetworkMessage.cardPlaced(1, "Archers", 3.0, 12.5);
        
        // Act
        String[] parsed = message.parseCardPlacement();
        
        // Assert
        assertNotNull(parsed);
        assertEquals(3, parsed.length);
        assertEquals("Archers", parsed[0]);
        assertEquals("3.0", parsed[1]);
        assertEquals("12.5", parsed[2]);
    }

    /**
     * Test Case 7: Player info message creation and parsing
     */
    @Test
    @DisplayName("playerInfo factory and parsing should work correctly")
    void testPlayerInfoMessage() {
        // Arrange
        String playerName = "TestPlayer";
        String deckCards = "Knight,Archers,Giant,Fireball";
        
        // Act
        NetworkMessage message = NetworkMessage.playerInfo(1, playerName, deckCards);
        String[] parsed = message.parsePlayerInfo();
        
        // Assert
        assertEquals(NetworkMessageType.PLAYER_INFO, message.getType());
        assertNotNull(parsed);
        assertEquals(playerName, parsed[0]);
        assertEquals(deckCards, parsed[1]);
    }

    /**
     * Test Case 8: Ready status message
     */
    @Test
    @DisplayName("readyStatus factory should create correct message")
    void testReadyStatusMessage() {
        // Act
        NetworkMessage readyMsg = NetworkMessage.readyStatus(1, true);
        NetworkMessage notReadyMsg = NetworkMessage.readyStatus(2, false);
        
        // Assert
        assertEquals(NetworkMessageType.READY_STATUS, readyMsg.getType());
        assertEquals("true", readyMsg.getData());
        assertEquals("false", notReadyMsg.getData());
    }

    /**
     * Test Case 9: Connection messages
     */
    @Test
    @DisplayName("Connection messages should be created correctly")
    void testConnectionMessages() {
        // Test connect
        NetworkMessage connect = NetworkMessage.connect("Player2");
        assertEquals(NetworkMessageType.CONNECT, connect.getType());
        assertEquals(2, connect.getPlayerId());
        assertEquals("Player2", connect.getData());
        
        // Test connect ack
        NetworkMessage ack = NetworkMessage.connectAck("HostPlayer");
        assertEquals(NetworkMessageType.CONNECT_ACK, ack.getType());
        assertEquals(1, ack.getPlayerId());
        
        // Test disconnect
        NetworkMessage disconnect = NetworkMessage.disconnect(1, "User quit");
        assertEquals(NetworkMessageType.DISCONNECT, disconnect.getType());
        assertEquals("User quit", disconnect.getData());
    }

    /**
     * Test Case 10: Game result messages
     */
    @Test
    @DisplayName("Victory and defeat messages should be created correctly")
    void testGameResultMessages() {
        // Test victory
        NetworkMessage victory = NetworkMessage.victory(1);
        assertEquals(NetworkMessageType.VICTORY, victory.getType());
        assertEquals(1, victory.getPlayerId());
        
        // Test defeat
        NetworkMessage defeat = NetworkMessage.defeat(2);
        assertEquals(NetworkMessageType.DEFEAT, defeat.getType());
        assertEquals(2, defeat.getPlayerId());
    }
}

