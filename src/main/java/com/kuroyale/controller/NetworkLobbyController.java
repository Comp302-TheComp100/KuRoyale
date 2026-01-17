package com.kuroyale.controller;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.User;
import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.NetworkService;
import com.kuroyale.service.NetworkService.ConnectionState;
import com.kuroyale.util.NetworkConfig;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the Network Lobby screen.
 * Manages host/join setup and pre-match lobby.
 * 
 * GRASP Patterns:
 * - Controller: Handles UI events and delegates to services
 * - Low Coupling: Uses NetworkService for all network operations
 */
public class NetworkLobbyController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private Circle statusIndicator;
    @FXML
    private Label statusLabel;

    // Mode Selection
    @FXML
    private VBox modeSelectionPane;
    @FXML
    private Button hostButton;
    @FXML
    private Button joinButton;

    // Host Setup
    @FXML
    private VBox hostSetupPane;
    @FXML
    private TextField portField;
    @FXML
    private Label hostIpLabel;
    @FXML
    private Label publicIpLabel;
    @FXML
    private Label portForwardingLabel;

    // Join Setup
    @FXML
    private VBox joinSetupPane;
    @FXML
    private TextField hostIpField;
    @FXML
    private TextField joinPortField;
    
    // Join Relay
    @FXML
    private VBox joinRelayPane;
    @FXML
    private TextField roomCodeField;

    // Waiting
    @FXML
    private VBox waitingPane;
    @FXML
    private Label waitingLabel;
    @FXML
    private Label connectionInfoLabel;

    // Lobby
    @FXML
    private VBox lobbyPane;
    @FXML
    private Label player1NameLabel;
    @FXML
    private Label player2NameLabel;
    @FXML
    private ListView<String> player1DeckList;
    @FXML
    private ListView<String> player2DeckList;
    @FXML
    private Circle player1ReadyIndicator;
    @FXML
    private Circle player2ReadyIndicator;
    @FXML
    private Label player1ReadyLabel;
    @FXML
    private Label player2ReadyLabel;
    @FXML
    private Button readyButton;
    @FXML
    private Button startMatchButton;
    @FXML
    private Label connectionQualityLabel;

    @FXML
    private Button backButton;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final NetworkConfig config = NetworkConfig.getInstance();
    private NetworkService networkService;

    private boolean isReady = false;
    private boolean opponentReady = false;
    private String playerName;
    private List<String> playerDeck;

    @FXML
    private void initialize() {
        // Get current user info through AuthenticationService
        AuthenticationService authService = ServiceFactory.getInstance().getAuthenticationService();
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            playerName = currentUser.getUsername();
            playerDeck = currentUser.getDeck();
        } else {
            playerName = "Player";
            playerDeck = Arrays.asList("Knight", "Archers", "Giant", "Fireball", "Arrows", "Bomber", "Minions",
                    "Musketeer");
        }

        // Initialize network service
        networkService = new NetworkService();
        setupNetworkCallbacks();

        // Setup UI
        portField.setText(String.valueOf(config.getDefaultPort()));
        joinPortField.setText(String.valueOf(config.getDefaultPort()));

        updateConnectionStatus(ConnectionState.DISCONNECTED);
    }

    private void setupNetworkCallbacks() {
        networkService.setOnStateChanged(state -> Platform.runLater(() -> {
            updateConnectionStatus(state);

            if (state == ConnectionState.CONNECTED) {
                showLobby();
            } else if (state == ConnectionState.DISCONNECTED) {
                showModeSelection();
            }
        }));

        networkService.setOnMessageReceived(message -> Platform.runLater(() -> handleNetworkMessage(message)));

        networkService.setOnError(error -> Platform.runLater(() -> showError(error)));

        // Connection ready callback - shows IP:port when hosting is ready
        networkService.setOnConnectionReady((success, connectionInfo) -> Platform.runLater(() -> {
            if (success && connectionInfo != null) {
                // Make sure we're showing the waiting pane
                if (!waitingPane.isVisible()) {
                    showPane(waitingPane);
                }
                // Success! Show the IP:port for clients to connect
                waitingLabel.setText("✓ Server started! Share this with your friend:");
                connectionInfoLabel.setText(connectionInfo);
                connectionInfoLabel.setStyle(
                        "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #00ff00; -fx-font-family: monospace;");
                copyToClipboard(connectionInfo);
                
                // Also show public IP if available
                networkService.fetchPublicIPAsync(publicIP -> Platform.runLater(() -> {
                    if (publicIP != null && publicIpLabel != null) {
                        String port = connectionInfo.contains(":") ? connectionInfo.split(":")[1] : "8080";
                        publicIpLabel.setText("For internet: " + publicIP + ":" + port);
                        publicIpLabel.setVisible(true);
                    }
                }));
            } else {
                // Server failed - go back to mode selection
                showModeSelection();
            }
        }));

    }

    /**
     * Copies text to clipboard.
     */
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        System.out.println("[NetworkLobby] Copied to clipboard: " + text);
    }

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PLAYER_INFO:
                handlePlayerInfo(message);
                break;

            case READY_STATUS:
                handleReadyStatus(message);
                break;

            case MATCH_START:
                startMatch();
                break;

            case OPPONENT_DISCONNECTED:
                showError("Opponent disconnected!");
                showModeSelection();
                break;

            case CONNECT:
            case CONNECT_ACK:
                // Connection established, update opponent name
                if (networkService.getOpponentName() != null) {
                    updateOpponentInfo(networkService.getOpponentName(), null);
                }
                break;

            default:
                break;
        }
    }

    private void handlePlayerInfo(NetworkMessage message) {
        String[] info = message.parsePlayerInfo();
        if (info != null) {
            String name = info[0];
            String deckStr = info.length > 1 ? info[1] : "";
            List<String> deck = deckStr.isEmpty() ? null : Arrays.asList(deckStr.split(","));
            updateOpponentInfo(name, deck);
        }
    }

    private void handleReadyStatus(NetworkMessage message) {
        boolean ready = Boolean.parseBoolean(message.getData());
        opponentReady = ready;
        System.out.println("[NetworkLobby] Received READY_STATUS: opponentReady=" + ready);

        // Opponent is always in column 2 (player2)
        updateReadyIndicator(player2ReadyIndicator, player2ReadyLabel, ready);

        checkStartConditions();
    }

    private void updateReadyIndicator(Circle indicator, Label label, boolean ready) {
        if (ready) {
            indicator.setFill(Color.LIME);
            label.setText("Ready!");
        } else {
            indicator.setFill(Color.RED);
            label.setText("Not Ready");
        }
    }

    private void checkStartConditions() {
        // Only host can start, and both must be ready
        boolean canStart = networkService.isHost() && isReady && opponentReady;
        System.out.println("[NetworkLobby] checkStartConditions: isHost=" + networkService.isHost() 
            + ", isReady=" + isReady + ", opponentReady=" + opponentReady + ", canStart=" + canStart);
        startMatchButton.setDisable(!canStart);
        System.out.println("[NetworkLobby] Start button disabled=" + startMatchButton.isDisable() + ", visible=" + startMatchButton.isVisible());
    }

    private void updateOpponentInfo(String name, List<String> deck) {
        // Opponent info always goes to player2 column (deck is hidden)
        player2NameLabel.setText(name != null ? name : "Opponent");
        // Deck is intentionally not shown - it's a secret!
    }

    // ==================== UI Actions ====================

    @FXML
    private void handleHostGame() {
        SoundEffectUtil.playButtonClick();
        showPane(hostSetupPane);

        // Show local IP immediately
        hostIpLabel.setText("Local IP (same network): " + networkService.getLocalIPAddress());

        // Fetch public IP asynchronously
        if (publicIpLabel != null) {
            publicIpLabel.setText("Public IP (internet): Fetching...");
            networkService.fetchPublicIPAsync(publicIP -> Platform.runLater(() -> {
                if (publicIP != null) {
                    publicIpLabel.setText("Public IP (internet): " + publicIP);
                } else {
                    publicIpLabel.setText("Public IP: Unable to detect (no internet?)");
                }
            }));
        }
    }

    @FXML
    private void handleJoinGame() {
        SoundEffectUtil.playButtonClick();
        showPane(joinSetupPane);
    }

    @FXML
    private void handleStartHosting() {
        SoundEffectUtil.playButtonClick();

        int port;
        try {
            String portText = portField.getText().trim();
            port = portText.isEmpty() ? config.getDefaultPort() : Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return;
        }

        if (networkService.startHosting(port, playerName)) {
            showPane(waitingPane);
            waitingLabel.setText("Waiting for opponent to connect...");
            connectionInfoLabel.setText("Starting server on port " + port + "...");
            connectionInfoLabel.setStyle("-fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleConnect() {
        SoundEffectUtil.playButtonClick();

        // Get host IP address
        String hostIP = hostIpField.getText().trim();
        if (hostIP.isEmpty()) {
            showError("Please enter the host's IP address");
            return;
        }

        // Get port number
        int port;
        try {
            String portText = joinPortField != null ? joinPortField.getText().trim() : "";
            port = portText.isEmpty() ? config.getDefaultPort() : Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return;
        }

        showPane(waitingPane);
        waitingLabel.setText("Connecting to host...");
        connectionInfoLabel.setText(hostIP + ":" + port);
        connectionInfoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        networkService.connectToHost(hostIP, port, playerName);
    }
    
    // ==================== Relay Mode Handlers (for Internet play) ====================
    
    @FXML
    private void handleHostRelay() {
        SoundEffectUtil.playButtonClick();
        
        showPane(waitingPane);
        waitingLabel.setText("Creating room...");
        connectionInfoLabel.setText("Connecting to relay server...");
        connectionInfoLabel.setStyle("-fx-font-size: 14px;");
        
        networkService.createRelayRoom(playerName);
    }
    
    @FXML
    private void handleJoinRelay() {
        SoundEffectUtil.playButtonClick();
        showPane(joinRelayPane);
    }
    
    @FXML
    private void handleConnectRelay() {
        SoundEffectUtil.playButtonClick();
        
        String roomCode = roomCodeField.getText().trim().toUpperCase();
        if (roomCode.isEmpty()) {
            showError("Please enter a room code");
            return;
        }
        
        showPane(waitingPane);
        waitingLabel.setText("Joining room...");
        connectionInfoLabel.setText(roomCode);
        connectionInfoLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: monospace;");
        
        networkService.joinRelayRoom(roomCode, playerName);
    }

    @FXML
    private void handleCancelSetup() {
        SoundEffectUtil.playButtonClick();
        showModeSelection();
    }

    @FXML
    private void handleCancelWaiting() {
        SoundEffectUtil.playButtonClick();
        networkService.disconnect();
        showModeSelection();
    }

    @FXML
    private void handleToggleReady() {
        SoundEffectUtil.playButtonClick();
        isReady = !isReady;
        System.out.println("[NetworkLobby] Toggle ready: isReady=" + isReady + ", sending to opponent...");

        // You are always player1 (column 1)
        updateReadyIndicator(player1ReadyIndicator, player1ReadyLabel, isReady);

        readyButton.setText(isReady ? "NOT READY" : "READY");

        // Send to opponent
        networkService.sendReadyStatus(isReady);
        System.out.println("[NetworkLobby] Sent ready status: " + isReady);

        checkStartConditions();
    }

    @FXML
    private void handleStartMatch() {
        System.out.println("[NetworkLobby] handleStartMatch() called!");
        System.out.println("[NetworkLobby] isHost=" + networkService.isHost() + ", isReady=" + isReady + ", opponentReady=" + opponentReady);
        SoundEffectUtil.playButtonClick();

        if (!networkService.isHost()) {
            System.out.println("[NetworkLobby] Not host, returning");
            return;
        }
        if (!isReady || !opponentReady) {
            System.out.println("[NetworkLobby] Not both ready, returning");
            return;
        }

        // Load and send the host's arena layout to the client
        // This ensures both players see the same arena design
        ArenaService arenaService = ServiceFactory.getInstance().getArenaService();
        User currentUser = ServiceFactory.getInstance().getAuthenticationService().getCurrentUser();
        if (currentUser != null) {
            arenaService.setCurrentUser(currentUser);
        }
        ArenaLayout hostLayout = arenaService.loadArenaLayout();
        networkService.sendArenaLayout(hostLayout);
        System.out.println("[NetworkLobby] Sent host arena layout to client: " + hostLayout.getName());

        // Send match start to opponent
        networkService.sendMatchStart();

        // Start match locally
        startMatch();
    }

    private void startMatch() {
        System.out.println("[NetworkLobby] Starting match - loading network-battle.fxml...");
        try {
            sceneLoader.load(root, "/fxml/network-battle.fxml", "KU Royale - Network Battle", controller -> {
                System.out.println("[NetworkLobby] Controller loaded: " + controller.getClass().getName());
                if (controller instanceof NetworkBattleController nbc) {
                    System.out.println("[NetworkLobby] Setting NetworkService on controller...");
                    nbc.setNetworkService(networkService);
                } else {
                    System.err.println("[NetworkLobby] ERROR: Controller is not NetworkBattleController!");
                }
            });
            System.out.println("[NetworkLobby] Scene loaded successfully!");
        } catch (Exception e) {
            System.err.println("[NetworkLobby] ERROR loading battle: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to load battle: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();

        // Disconnect in background to avoid UI freeze
        new Thread(() -> {
            networkService.disconnect();
        }).start();

        // Navigate immediately - don't wait for disconnect to complete
        try {
            sceneLoader.load(root, "/fxml/battle-mode-selection.fxml", "KU Royale - Select Battle Mode", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== UI Helpers ====================

    private void showModeSelection() {
        showPane(modeSelectionPane);
        isReady = false;
        opponentReady = false;
    }

    private void showLobby() {
        showPane(lobbyPane);

        // Column 1 is always YOU (your deck visible)
        // Column 2 is always OPPONENT (deck hidden)
        player1NameLabel.setText(playerName);
        player1DeckList.setItems(FXCollections.observableArrayList(playerDeck));

        String opponentName = networkService.getOpponentName();
        player2NameLabel.setText(opponentName != null ? opponentName : "Waiting...");

        // Send player info (without deck - keep it secret!)
        networkService.send(NetworkMessage.playerInfo(networkService.getPlayerId(), playerName, ""));

        // Reset ready state
        isReady = false;
        opponentReady = false;
        readyButton.setText("READY");
        updateReadyIndicator(player1ReadyIndicator, player1ReadyLabel, false);
        updateReadyIndicator(player2ReadyIndicator, player2ReadyLabel, false);

        // Only host can start match
        startMatchButton.setVisible(networkService.isHost());
        startMatchButton.setManaged(networkService.isHost());
    }

    private void showPane(VBox pane) {
        modeSelectionPane.setVisible(false);
        modeSelectionPane.setManaged(false);
        hostSetupPane.setVisible(false);
        hostSetupPane.setManaged(false);
        joinSetupPane.setVisible(false);
        joinSetupPane.setManaged(false);
        if (joinRelayPane != null) {
            joinRelayPane.setVisible(false);
            joinRelayPane.setManaged(false);
        }
        waitingPane.setVisible(false);
        waitingPane.setManaged(false);
        lobbyPane.setVisible(false);
        lobbyPane.setManaged(false);

        pane.setVisible(true);
        pane.setManaged(true);
    }

    private void updateConnectionStatus(ConnectionState state) {
        switch (state) {
            case DISCONNECTED:
                statusIndicator.setFill(Color.RED);
                statusLabel.setText("Disconnected");
                break;
            case CONNECTING:
                statusIndicator.setFill(Color.YELLOW);
                statusLabel.setText("Connecting...");
                break;
            case CONNECTED:
                statusIndicator.setFill(Color.LIME);
                statusLabel.setText("Connected");
                break;
            case RECONNECTING:
                statusIndicator.setFill(Color.ORANGE);
                statusLabel.setText("Reconnecting...");
                break;
        }
    }

    private void showError(String message) {
        com.kuroyale.view.ThemedAlertController.show("Network Error", message);
    }
}
