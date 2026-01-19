package com.kuroyale.view.battle.renderers;

import com.kuroyale.util.GameColors;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class HealthBarRenderer {

    // Prevent instantiation
    private HealthBarRenderer() {
    }

    public static class HealthBarNodes {
        public final StackPane root;
        public final Rectangle background;
        public final Rectangle foreground;
        public final Text text;

        public HealthBarNodes(StackPane root, Rectangle background, Rectangle foreground, Text text) {
            this.root = root;
            this.background = background;
            this.foreground = foreground;
            this.text = text;
        }
    }

    /**
     * Creates a simple health bar (bg + fg) wrapped in a StackPane.
     * Used for Troops and small entities.
     */
    public static HealthBarNodes createSimpleHealthBar(double width, double height) {
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(GameColors.HEALTH_BAR_BG);
        bg.setStroke(GameColors.HEALTH_BAR_STROKE);
        bg.setStrokeWidth(0.5);

        Rectangle fg = new Rectangle(width, height); // Init full width
        fg.setFill(GameColors.PLAYER_TEAM); // Default color

        StackPane container = new StackPane(bg, fg);
        container.setAlignment(Pos.CENTER_LEFT);

        return new HealthBarNodes(container, bg, fg, null);
    }

    /**
     * Creates a detailed health bar with text value.
     * Used for Towers and Buildings.
     */
    public static HealthBarNodes createDetailedHealthBar(double width, double height, double currentHealth) {
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(GameColors.HEALTH_BAR_BG);
        bg.setStroke(GameColors.HEALTH_BAR_STROKE);
        bg.setStrokeWidth(0.5);

        Rectangle fg = new Rectangle(width, height);
        fg.setFill(GameColors.PLAYER_TEAM);

        Text text = new Text(String.format("%.0f", currentHealth));
        text.setFont(Font.font("Arial Black", FontWeight.BOLD, 10));
        text.setFill(GameColors.TEXT_FILL);
        text.setStroke(GameColors.TEXT_STROKE);
        text.setStrokeWidth(0.5);

        StackPane container = new StackPane(bg, fg, text);
        StackPane.setAlignment(fg, Pos.CENTER_LEFT);
        StackPane.setAlignment(text, Pos.CENTER);

        return new HealthBarNodes(container, bg, fg, text);
    }

    public static void updateHealthBar(HealthBarNodes nodes, double currentHealth, double maxHealth,
            boolean isPlayerSide) {
        if (nodes == null)
            return;

        double pct = maxHealth > 0 ? Math.max(0, currentHealth) / maxHealth : 0.0;
        double width = nodes.background.getWidth();

        nodes.foreground.setWidth(width * pct);
        nodes.foreground.setFill(isPlayerSide ? GameColors.PLAYER_TEAM : GameColors.ENEMY_TEAM);

        if (nodes.text != null) {
            nodes.text.setText(String.format("%.0f", currentHealth));
        }
    }
}
