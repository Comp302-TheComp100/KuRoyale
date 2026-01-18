package com.kuroyale.view.battle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PngSequenceSprite extends ImageView {

    private static final Map<String, List<Image>> frameCache = new java.util.concurrent.ConcurrentHashMap<>();

    private volatile List<Image> frames = Collections.emptyList();
    private int currentFrame = 0;
    private Timeline timeline;

    private boolean loop = true;
    private double loopDurationSeconds = 0.8;
    private Runnable onFinished;

    private String fallbackImagePath;

    public PngSequenceSprite(String baseFolderPath, double fitWidth, double fitHeight) {
        super();
        setFitWidth(fitWidth);
        setFitHeight(fitHeight);
        setPreserveRatio(true);
        setSmooth(true);

        loadAndPlay(baseFolderPath);
    }

    public PngSequenceSprite(String baseFolderPath, double fitWidth, double fitHeight, String fallbackImagePath) {
        this(baseFolderPath, fitWidth, fitHeight);
        this.fallbackImagePath = fallbackImagePath;
        applyFallbackIfNeeded();
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        restartTimelineIfReady();
    }

    public void setLoopDurationSeconds(double loopDurationSeconds) {
        this.loopDurationSeconds = Math.max(0.01, loopDurationSeconds);
        restartTimelineIfReady();
    }

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public boolean hasFramesLoaded() {
        return frames != null && !frames.isEmpty();
    }

    public void updateSequence(String baseFolderPath) {
        stop();
        currentFrame = 0;
        loadAndPlay(baseFolderPath);
    }

    public void setFallbackImagePath(String fallbackImagePath) {
        this.fallbackImagePath = fallbackImagePath;
        applyFallbackIfNeeded();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void loadAndPlay(String baseFolderPath) {
        if (baseFolderPath == null || baseFolderPath.isBlank()) {
            applyFallbackIfNeeded();
            return;
        }

        if (frameCache.containsKey(baseFolderPath)) {
            frames = frameCache.get(baseFolderPath);
            initAndStart();
            applyFallbackIfNeeded();
            return;
        }

        CompletableFuture
                .supplyAsync(() -> loadFrames(baseFolderPath))
                .thenAccept(loaded -> javafx.application.Platform.runLater(() -> {
                    if (loaded == null || loaded.isEmpty()) {
                        applyFallbackIfNeeded();
                        return;
                    }
                    frameCache.put(baseFolderPath, loaded);
                    frames = loaded;
                    initAndStart();
                }));
    }

    private void initAndStart() {
        if (frames == null || frames.isEmpty()) {
            applyFallbackIfNeeded();
            return;
        }

        currentFrame = 0;
        setImage(frames.get(0));

        if (frames.size() > 1) {
            startTimeline();
        }
    }

    private void restartTimelineIfReady() {
        if (frames == null || frames.size() <= 1) {
            return;
        }

        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        startTimeline();
    }

    private void startTimeline() {
        if (frames == null || frames.size() <= 1) {
            return;
        }

        int frameCount = frames.size();
        double frameDurationMillis = (loopDurationSeconds * 1000.0) / Math.max(1, frameCount);
        frameDurationMillis = clamp(frameDurationMillis, 25.0, 250.0);

        timeline = new Timeline(new KeyFrame(Duration.millis(frameDurationMillis), e -> nextFrame()));
        timeline.setCycleCount(loop ? Timeline.INDEFINITE : Math.max(1, frameCount - 1));
        if (!loop) {
            timeline.setOnFinished(e -> {
                if (onFinished != null) {
                    onFinished.run();
                }
            });
        }
        timeline.play();
    }

    private void nextFrame() {
        if (frames == null || frames.isEmpty()) {
            return;
        }

        if (loop) {
            currentFrame++;
            if (currentFrame >= frames.size()) {
                currentFrame = 0;
            }
            setImage(frames.get(currentFrame));
            return;
        }

        if (currentFrame < frames.size() - 1) {
            currentFrame++;
            setImage(frames.get(currentFrame));
        }
    }

    private List<Image> loadFrames(String baseFolderPath) {
        List<Image> loaded = new ArrayList<>();

        List<String> patterns = List.of(
                "%d.png",
                "%02d.png",
                "%03d.png",
                "frame_%d.png",
                "frame_%02d.png",
                "frame_%03d.png",
                "frame%d.png",
                "frame%02d.png",
                "frame%03d.png");

        for (String pattern : patterns) {
            loaded.clear();
            for (int i = 0; i < 200; i++) {
                String file = String.format(Locale.ENGLISH, pattern, i);
                String full = baseFolderPath.endsWith("/") ? (baseFolderPath + file) : (baseFolderPath + "/" + file);

                try (InputStream is = getClass().getResourceAsStream(full)) {
                    if (is == null) {
                        break;
                    }
                    loaded.add(new Image(is));
                } catch (Exception e) {
                    break;
                }
            }

            if (!loaded.isEmpty()) {
                return new ArrayList<>(loaded);
            }
        }

        return Collections.emptyList();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static String toCardKey(String cardName) {
        if (cardName == null) {
            return "";
        }
        return cardName.toLowerCase(Locale.ENGLISH).replace(" ", "_").replace(".", "");
    }

    private void applyFallbackIfNeeded() {
        if (hasFramesLoaded()) {
            return;
        }
        if (getImage() != null) {
            return;
        }
        if (fallbackImagePath == null || fallbackImagePath.isBlank()) {
            return;
        }
        try (InputStream is = getClass().getResourceAsStream(fallbackImagePath)) {
            if (is != null) {
                setImage(new Image(is));
            }
        } catch (Exception ignored) {
        }
    }
}
