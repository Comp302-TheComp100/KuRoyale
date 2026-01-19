package com.kuroyale.view.battle.effects;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Handles Zap spell visual effects.
 */
public class ZapEffect {

    public static void play(Pane unitLayer, double cx, double cy, double rPixels, double duration,
            java.util.List<ActiveSpellVisual> activeVisuals) {
        double radiusX = rPixels;
        double radiusY = rPixels * 0.9;

        javafx.scene.shape.Ellipse areaEllipse = new javafx.scene.shape.Ellipse(cx, cy, radiusX, radiusY);
        areaEllipse.setFill(Color.rgb(255, 255, 0, 0.3));
        areaEllipse.setStroke(Color.WHITE);
        areaEllipse.setStrokeWidth(3.0);

        javafx.scene.effect.DropShadow zapGlow = new javafx.scene.effect.DropShadow();
        zapGlow.setColor(Color.WHITE);
        zapGlow.setRadius(25);
        zapGlow.setSpread(0.6);
        areaEllipse.setEffect(zapGlow);
        areaEllipse.setMouseTransparent(true);

        unitLayer.getChildren().add(areaEllipse);
        activeVisuals.add(new ActiveSpellVisual(areaEllipse, duration));

        // Lightning GIF
        javafx.scene.image.Image gifImage = new javafx.scene.image.Image(
                ZapEffect.class.getResourceAsStream("/gifs/zap.gif"));
        javafx.scene.image.ImageView gifView = new javafx.scene.image.ImageView(gifImage);

        double spellWidth = rPixels * 2.0;
        double visualOffset = 100.0;
        double lightningHeight = cy + visualOffset;

        gifView.setFitWidth(spellWidth);
        gifView.setFitHeight(lightningHeight);
        gifView.setPreserveRatio(false);
        gifView.setLayoutX(cx - (spellWidth / 2.0));
        gifView.setLayoutY(cy - lightningHeight + (radiusY / 2));

        gifView.setMouseTransparent(true);
        unitLayer.getChildren().add(gifView);
        activeVisuals.add(new ActiveSpellVisual(gifView, duration));
    }
}
