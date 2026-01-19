package com.kuroyale.controller;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.User;
import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.NetworkService;
import com.kuroyale.service.NetworkService.ConnectionState;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.config.NetworkConfig;
import com.kuroyale.util.ui.SceneLoader;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import java.util.Optional;

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

    // Join Setup
    @FXML
    private VBox joinSetupPane;
    @FXML
    private TextField hostIpField;
    @FXML
    private TextField joinPortField;

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

        // Connection ready callback - shows room code when ready
        networkService.setOnConnectionReady((success, roomCode) -> Platform.runLater(() -> {
            if (!waitingPane.isVisible())
                return;

            if (success && roomCode != null) {
                // Success! Show the room code
                waitingLabel.setText("✓ Ready! Share this code with your friend:");
                connectionInfoLabel.setText(roomCode);
                connectionInfoLabel.setStyle(
                        "-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #00ff00; -fx-font-family: monospace;");
                copyToClipboard(roomCode);
            } else {
                waitingLabel.setText("Connection failed. Please try again.");
                connectionInfoLabel.setText("");
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
        startMatchButton.setDisable(!canStart);
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
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return;
        }

        if (networkService.startHosting(port, playerName)) {
            showPane(waitingPane);
            waitingLabel.setText("Creating public game server...");
            connectionInfoLabel.setText("Please wait...");
            connectionInfoLabel.setStyle("-fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleConnect() {
        SoundEffectUtil.playButtonClick();

        String roomCode = hostIpField.getText().trim().toUpperCase();
        if (roomCode.isEmpty()) {
            showError("Please enter the room code");
            return;
        }

        if (roomCode.length() != 6) {
            showError("Room code should be 6 characters");
            return;
        }

        showPane(waitingPane);
        waitingLabel.setText("Joining room...");
        connectionInfoLabel.setText("Room: " + roomCode);
        connectionInfoLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        networkService.connectToHost(roomCode, 0, playerName);
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

        // You are always player1 (column 1)
        updateReadyIndicator(player1ReadyIndicator, player1ReadyLabel, isReady);

        readyButton.setText(isReady ? "NOT READY" : "READY");

        // Send to opponent
        networkService.sendReadyStatus(isReady);

        checkStartConditions();
    }

    @FXML
    private void handleStartMatch() {
        SoundEffectUtil.playButtonClick();

        if (!networkService.isHost())
            return;
        if (!isReady || !opponentReady)
            return;

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
        try {
            sceneLoader.load(root, "/fxml/network-battle.fxml", "KU Royale - Network Battle", controller -> {
                if (controller instanceof NetworkBattleController nbc) {
                    nbc.setNetworkService(networkService);
                }
            });
        } catch (IOException e) {
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
        com.kuroyale.util.ui.ThemedAlertManager.show("Network Error", message);
    }
}
