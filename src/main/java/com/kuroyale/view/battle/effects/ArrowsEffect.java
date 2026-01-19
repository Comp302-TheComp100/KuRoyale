package com.kuroyale.view.battle.effects;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Handles Arrows spell visual effects.
 */
public class ArrowsEffect {

    public static void play(Pane unitLayer, boolean isPlayerSource, double cx, double cy, double rPixels,
            double duration, java.util.List<ActiveSpellVisual> activeVisuals) {
        int arrowCount = 8;
        double spreadRadius = rPixels * 0.8;

        // Area indicator
        Circle areaIndicator = new Circle(cx, cy, rPixels);
        areaIndicator.setFill(Color.rgb(255, 100, 0, 0.15));
        areaIndicator.setStroke(Color.rgb(255, 150, 0, 0.5));
        areaIndicator.setStrokeWidth(2.0);
        areaIndicator.setMouseTransparent(true);
        unitLayer.getChildren().add(areaIndicator);
        activeVisuals.add(new ActiveSpellVisual(areaIndicator, duration));

        double startY = isPlayerSource ? (cy + 200) : (cy - 200);

        for (int i = 0; i < arrowCount; i++) {
            double offsetX = (Math.random() - 0.5) * spreadRadius * 2;
            double offsetY = (Math.random() - 0.5) * spreadRadius * 2;
            double targetX = cx + offsetX;
            double targetY = cy + offsetY;
            double startX = targetX + (Math.random() - 0.5) * 80;

            Rectangle arrowVisual = new Rectangle(25, 3);
            arrowVisual.setArcWidth(3);
            arrowVisual.setArcHeight(3);
            arrowVisual.setFill(Color.rgb(139, 90, 43));

            javafx.scene.effect.DropShadow trailGlow = new javafx.scene.effect.DropShadow();
            trailGlow.setColor(Color.ORANGE);
            trailGlow.setRadius(8);
            trailGlow.setSpread(0.3);
            arrowVisual.setEffect(trailGlow);
            arrowVisual.setMouseTransparent(true);
            arrowVisual.setVisible(false);
            unitLayer.getChildren().add(arrowVisual);

            javafx.scene.shape.QuadCurve arrowPath = new javafx.scene.shape.QuadCurve();
            arrowPath.setStartX(startX);
            arrowPath.setStartY(startY);
            arrowPath.setEndX(targetX);
            arrowPath.setEndY(targetY);

            double midX = (startX + targetX) / 2.0;
            double midY = (startY + targetY) / 2.0;
            double arcAmount = isPlayerSource ? 60.0 : -60.0;
            arrowPath.setControlX(midX);
            arrowPath.setControlY(midY + arcAmount);
            arrowPath.setVisible(false);
            unitLayer.getChildren().add(arrowPath);

            javafx.animation.PathTransition pathTransition = new javafx.animation.PathTransition();
            double delayMs = (i * 40) + Math.random() * 60;
            pathTransition.setDuration(javafx.util.Duration.millis(300 + Math.random() * 100));
            pathTransition.setPath(arrowPath);
            pathTransition.setNode(arrowVisual);
            pathTransition
                    .setOrientation(javafx.animation.PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
            pathTransition.setCycleCount(1);

            final Rectangle fArrow = arrowVisual;
            final javafx.scene.shape.QuadCurve fPath = arrowPath;
            final double fTargetX = targetX, fTargetY = targetY;

            pathTransition.setOnFinished(e -> {
                unitLayer.getChildren().remove(fArrow);
                unitLayer.getChildren().remove(fPath);
                createImpactEffect(unitLayer, fTargetX, fTargetY);
            });

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(delayMs));
            delay.setOnFinished(e -> {
                fArrow.setVisible(true);
                pathTransition.play();
            });
            delay.play();
        }
    }

    private static void createImpactEffect(Pane unitLayer, double x, double y) {
        Circle impact = new Circle(x, y, 0);
        impact.setFill(Color.rgb(255, 200, 0, 0.8));
        impact.setStroke(Color.WHITE);
        impact.setStrokeWidth(2.0);
        impact.setMouseTransparent(true);

        unitLayer.getChildren().add(impact);

        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300),
                impact);
        scale.setFromX(0.1);
        scale.setFromY(0.1);
        scale.setToX(1.5);
        scale.setToY(1.5);

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                impact);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        javafx.animation.ParallelTransition impactAnim = new javafx.animation.ParallelTransition(scale, fade);
        impactAnim.setOnFinished(e -> unitLayer.getChildren().remove(impact));
        impactAnim.play();
    }
}
