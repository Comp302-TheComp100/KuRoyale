package com.kuroyale.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimatedSprite for rendering animated GIF troops using frame-by-frame
 * animation.
 * GRASP Principles:
 * - Information Expert: Knows how to extract and display GIF frames
 * - Creator: Created by BattleArenaView when rendering troops
 * - Low Coupling: Only depends on JavaFX and ImageIO
 */
public class AnimatedSprite extends ImageView {

    private List<Image> frames;
    private static final java.util.Map<String, List<Image>> frameCache = new java.util.HashMap<>();

    private int currentFrame = 0;
    private Timeline timeline;
    private double speedMultiplier = 1.0;

    /**
     * Creates an animated sprite from a GIF path.
     * 
     * @param gifPath Path to the GIF resource
     * @param size    Size in pixels to fit the sprite
     */
    public AnimatedSprite(String gifPath, double size) {
        this(gifPath, size, 1.0);
    }

    /**
     * Creates an animated sprite from a GIF path with speed control.
     * 
     * @param gifPath         Path to the GIF resource
     * @param size            Size in pixels to fit the sprite
     * @param speedMultiplier Animation speed multiplier (1.0 = normal 10fps)
     */
    public AnimatedSprite(String gifPath, double size, double speedMultiplier) {
        super();
        this.speedMultiplier = speedMultiplier;

        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        setSmooth(true);

        // Check cache first
        if (frameCache.containsKey(gifPath)) {
            this.frames = frameCache.get(gifPath);
            initAnimation();
        } else {
            // Async load
            java.util.concurrent.CompletableFuture.supplyAsync(() -> loadFrames(gifPath))
                    .thenAccept(loadedFrames -> {
                        javafx.application.Platform.runLater(() -> {
                            if (loadedFrames != null && !loadedFrames.isEmpty()) {
                                frameCache.put(gifPath, loadedFrames);
                                // Only update if we haven't been stopped or changed in the meantime
                                // (For simplicity, we just update. A more robust solution might check a
                                // 'currentPath' tag)
                                this.frames = loadedFrames;
                                initAnimation();
                            }
                        });
                    });
        }
    }

    private void initAnimation() {
        if (frames != null && !frames.isEmpty()) {
            // Set initial frame
            setImage(frames.get(0));

            // Start animation if multiple frames
            if (frames.size() > 1) {
                startAnimation();
            }
        }
    }

    /**
     * Updates the animation to a new GIF and speed without recreating the object.
     * 
     * @param gifPath         Path to the new GIF resource
     * @param speedMultiplier New animation speed multiplier
     */
    public void updateAnimation(String gifPath, double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;

        // Check cache first
        if (frameCache.containsKey(gifPath)) {
            List<Image> cachedFrames = frameCache.get(gifPath);
            stop();
            this.frames = cachedFrames;
            this.currentFrame = 0;
            initAnimation();
        } else {
            // Async load
            java.util.concurrent.CompletableFuture.supplyAsync(() -> loadFrames(gifPath))
                    .thenAccept(loadedFrames -> {
                        javafx.application.Platform.runLater(() -> {
                            if (loadedFrames != null && !loadedFrames.isEmpty()) {
                                frameCache.put(gifPath, loadedFrames);
                                stop();
                                this.frames = loadedFrames;
                                this.currentFrame = 0;
                                initAnimation();
                            }
                        });
                    });
        }
    }

    private List<Image> loadFrames(String gifPath) {
        List<Image> loadedFrames = new ArrayList<>();
        try {
            InputStream inputStream = getClass().getResourceAsStream(gifPath);
            if (inputStream == null) {
                throw new RuntimeException("GIF not found: " + gifPath);
            }

            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
            ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
            reader.setInput(imageInputStream);

            int i = 0;
            while (true) {
                try {
                    BufferedImage bufferedImage = reader.read(i++);
                    Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    loadedFrames.add(fxImage);
                } catch (IndexOutOfBoundsException e) {
                    break; // No more frames
                }
            }

            reader.dispose();
            imageInputStream.close();
            inputStream.close();

        } catch (Exception e) {
            System.err.println("Failed to load animated sprite: " + gifPath);
            e.printStackTrace();
            return null;
        }
        return loadedFrames;
    }

    /**
     * Starts the frame-by-frame animation.
     */
    private void startAnimation() {
        // Cycle through frames based on speed multiplier
        // Base duration is 100ms (10 FPS)
        double duration = 100.0 / Math.max(0.1, speedMultiplier);
        timeline = new Timeline(
                new KeyFrame(Duration.millis(duration), e -> nextFrame()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Advances to the next frame.
     */
    private void nextFrame() {
        currentFrame = (currentFrame + 1) % frames.size();
        setImage(frames.get(currentFrame));
    }

    /**
     * Stops the animation.
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    /**
     * Helper to construct GIF path based on troop properties.
     * Naming pattern: {CardName}_{state}_{side}_{W}-{H}.gif
     * 
     * @param cardName     Name of the card (e.g., "Giant")
     * @param state        Animation state: "walk" or "fight"
     * @param isPlayerSide true for player, false for opponent
     * @param isRage       true if in rage mode
     * @return Path to GIF resource
     */
    public static String buildGifPath(String cardName, String state, boolean isPlayerSide, boolean isRage) {
        String side = isPlayerSide ? "player" : "opponent";
        String rageStr = isRage ? "-rage" : "";

        // Map card name to GIF filename format
        String fileName = cardName.replace(" ", "");

        // Pattern: {CardName}_{state}{-rage}_{side}_{W}-{H}.gif
        // For simplicity, we'll use a lookup for dimensions
        String dimensions = getAnimationDimensions(fileName);

        return String.format("/gifs/%s_%s%s_%s_%s.gif",
                fileName, state, rageStr, side, dimensions);
    }

    /**
     * Returns the dimension string for a given card name.
     * This maps card names to their GIF dimensions.
     */
    private static String getAnimationDimensions(String cardName) {
        // Based on the GIF files provided
        switch (cardName) {
            case "Giant":
                return "109-109";
            case "Archer":
                return "62-62"; // walk dimension; fight is different but we'll use walk as default
            case "Barbarian":
                return "115-90";
            case "Valkyrie":
                return "46-52";
            case "Wizard":
                return "43-53";
            case "MiniPekka":
                return "62-62";
            case "BabyDragon":
                return "88-80";
            default:
                return "62-62"; // Default fallback
        }
    }

    /**
     * Checks if the given card name has a supported animation.
     */
    public static boolean isAnimated(String cardName) {
        switch (cardName) {
            case "Giant":
            case "Archer":
            case "Barbarian":
            case "Valkyrie":
            case "Wizard":
            case "MiniPekka":
            case "BabyDragon":
                return true;
            default:
                return false;
        }
    }
}
