package com.kuroyale.util;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

/**
 * Utility class for applying consistent JavaFX styling across the application
 * Replaces external CSS with programmatic styling
 */
public class StyleHelper {

    // PseudoClass definitions for state management
    public static final PseudoClass HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("hover");
    public static final PseudoClass FILLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("filled");
    public static final PseudoClass HIGHLIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlight");

    // Color constants
    public static final String COLOR_PRIMARY = "#f97316";
    public static final String COLOR_PRIMARY_HOVER = "#fb923c";
    public static final String COLOR_PRIMARY_PRESSED = "#ea580c";
    public static final String COLOR_SECONDARY = "#64748b";
    public static final String COLOR_SECONDARY_HOVER = "#475569";
    public static final String COLOR_BROWN_LIGHT = "#A0826D";
    public static final String COLOR_BROWN_LIGHTER = "#B89880";
    public static final String COLOR_BROWN_DARK = "#654321";
    public static final String COLOR_YELLOW = "#fbbf24";
    public static final String COLOR_YELLOW_DARK = "#f59e0b";
    public static final String COLOR_BLUE = "#3b82f6";
    public static final String COLOR_BLUE_DARK = "#2563eb";
    public static final String COLOR_GREEN = "#10b981";
    public static final String COLOR_GREEN_DARK = "#059669";
    public static final String COLOR_RED = "#ef4444";
    public static final String COLOR_RED_DARK = "#dc2626";
    public static final String COLOR_PURPLE = "#8b5cf6";
    public static final String COLOR_WHITE = "white";
    public static final String COLOR_DARK = "#1e293b";
    public static final String COLOR_GRAY = "#cbd5e1";
    public static final String COLOR_GRAY_DARK = "#475569";
    public static final String COLOR_GRAY_MEDIUM = "#94a3b8";

    // Font family (loaded programmatically)
    public static final String FONT_FAMILY = "Clash";

    /**
     * Apply main menu background style
     */
    public static void applyMainMenuBackground(Node node) {
        node.setStyle(
                "-fx-background-image: url('/images/main_menu_background.png');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center;");
    }

    /**
     * Apply deck builder background style
     */
    public static void applyDeckBuilderBackground(Node node) {
        node.setStyle(
                "-fx-background-image: url('/images/main_menu_background.png');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center;" +
                        "-fx-background-repeat: no-repeat;");
    }

    /**
     * Apply title label style
     */
    public static void applyTitleStyle(Label label) {
        label.setStyle(
                "-fx-font-size: 60px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                        "-fx-text-fill: white;");

        // Drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        dropShadow.setRadius(10);
        dropShadow.setOffsetY(3);
        label.setEffect(dropShadow);
    }

    /**
     * Apply battle deck title style
     */
    public static void applyBattleDeckTitleStyle(Label label) {
        label.setStyle(
                "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                        "-fx-text-fill: white;");

        // Drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        dropShadow.setRadius(5);
        dropShadow.setOffsetY(2);
        label.setEffect(dropShadow);
    }

    /**
     * Apply menu button style with hover effects
     */
    public static void applyMenuButtonStyle(Button button) {
        // Base style
        String baseStyle = "-fx-background-color: linear-gradient(to bottom, " + COLOR_PRIMARY + " 0%, "
                + COLOR_PRIMARY_PRESSED + " 100%);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-border-color: #92400e;" +
                "-fx-border-width: 3;" +
                "-fx-cursor: hand;";

        button.setStyle(baseStyle);

        // Drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.4));
        dropShadow.setRadius(8);
        dropShadow.setOffsetY(3);
        button.setEffect(dropShadow);

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(baseStyle.replace(
                    "linear-gradient(to bottom, " + COLOR_PRIMARY + " 0%, " + COLOR_PRIMARY_PRESSED + " 100%)",
                    "linear-gradient(to bottom, " + COLOR_PRIMARY_HOVER + " 0%, " + COLOR_PRIMARY + " 100%)"));
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        button.setOnMousePressed(e -> {
            button.setStyle(baseStyle.replace(
                    "linear-gradient(to bottom, " + COLOR_PRIMARY + " 0%, " + COLOR_PRIMARY_PRESSED + " 100%)",
                    "linear-gradient(to bottom, " + COLOR_PRIMARY_PRESSED + " 0%, #c2410c 100%)"));
            button.setTranslateY(2);
        });

        button.setOnMouseReleased(e -> {
            button.setTranslateY(0);
            // Check if mouse is still over button
            if (button.isHover()) {
                button.setStyle(baseStyle.replace(
                        "linear-gradient(to bottom, " + COLOR_PRIMARY + " 0%, " + COLOR_PRIMARY_PRESSED + " 100%)",
                        "linear-gradient(to bottom, " + COLOR_PRIMARY_HOVER + " 0%, " + COLOR_PRIMARY + " 100%)"));
            } else {
                button.setStyle(baseStyle);
            }
        });
    }

    /**
     * Apply back button style with hover effects
     */
    public static void applyBackButtonStyle(Button button) {
        String baseStyle = "-fx-background-color: " + COLOR_SECONDARY + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 25 10 25;" +
                "-fx-cursor: hand;";

        button.setStyle(baseStyle);

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(baseStyle.replace(COLOR_SECONDARY, COLOR_SECONDARY_HOVER));
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
        });
    }

    /**
     * Apply deck slots background style
     */
    public static void applyDeckSlotsBackground(Node node) {
        node.setStyle(
                "-fx-background-color: " + COLOR_BROWN_DARK + ";" +
                        "-fx-background-radius: 8;");

        // Drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        dropShadow.setRadius(5);
        dropShadow.setOffsetY(2);
        node.setEffect(dropShadow);
    }

    /**
     * Apply average elixir label style
     */
    public static void applyAverageElixirLabelStyle(Label label) {
        label.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                        "-fx-text-fill: " + COLOR_PURPLE + ";");
    }

    /**
     * Apply average elixir value style
     */
    public static void applyAverageElixirValueStyle(Label label) {
        label.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                        "-fx-text-fill: " + COLOR_PURPLE + ";");
    }

    /**
     * Apply cards grid style
     */
    public static void applyCardsGridStyle(Node node) {
        node.setStyle(
                "-fx-background-color: " + COLOR_BROWN_DARK + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-hgap: 15;" +
                        "-fx-vgap: 15;" +
                        "-fx-padding: 20;" +
                        "-fx-pref-width: 800;" +
                        "-fx-max-width: 800;");
    }

    /**
     * Apply scroll pane transparent style with hidden scrollbar
     */
    public static void applyScrollPaneStyle(Node node) {
        if (node instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane scrollPane = (javafx.scene.control.ScrollPane) node;

            // Set ScrollPane to transparent and hide scrollbars
            scrollPane.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-background: transparent;");

            // Hide scrollbars by styling the scroll bar components
            scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

            // Access and style the viewport to be transparent
            if (scrollPane.lookup(".viewport") != null) {
                scrollPane.lookup(".viewport").setStyle("-fx-background-color: transparent;");
            }
        } else {
            node.setStyle("-fx-background-color: transparent;");
        }
    }

    /**
     * Apply card view style with hover effects
     */
    public static void applyCardViewStyle(Node node) {
        String baseStyle = "-fx-background-color: transparent;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0;";

        node.setStyle(baseStyle);

        // Base drop shadow effect
        DropShadow baseDropShadow = new DropShadow();
        baseDropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        baseDropShadow.setRadius(8);
        baseDropShadow.setOffsetY(2);
        node.setEffect(baseDropShadow);

        // Hover effects
        node.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setColor(Color.rgb(59, 130, 246, 0.6));
            hoverShadow.setRadius(12);
            hoverShadow.setOffsetY(3);
            node.setEffect(hoverShadow);
            node.setScaleX(1.02);
            node.setScaleY(1.02);
        });

        node.setOnMouseExited(e -> {
            node.setEffect(baseDropShadow);
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        });
    }

    /**
     * Apply deck slot base style (empty state)
     */
    public static void applyDeckSlotEmptyStyle(Node node) {
        node.setStyle(
                "-fx-background-color: " + COLOR_BROWN_LIGHT + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-width: 0;" +
                        "-fx-border-radius: 8;" +
                        "-fx-pref-width: 120;" +
                        "-fx-pref-height: 160;" +
                        "-fx-alignment: center;" +
                        "-fx-cursor: hand;");
    }

    /**
     * Apply deck slot filled style
     */
    public static void applyDeckSlotFilledStyle(Node node) {
        node.setStyle(
                "-fx-background-color: " + COLOR_BROWN_LIGHT + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-width: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 8;" +
                        "-fx-pref-width: 120;" +
                        "-fx-pref-height: 160;" +
                        "-fx-alignment: center;" +
                        "-fx-cursor: hand;");
    }

    /**
     * Apply deck slot hover style
     */
    public static void applyDeckSlotHoverStyle(Node node) {
        String currentStyle = node.getStyle();
        node.setStyle(currentStyle.replace(COLOR_BROWN_LIGHT, COLOR_BROWN_LIGHTER));
    }

    /**
     * Apply deck slot replace highlight style
     */
    public static void applyDeckSlotReplaceHighlightStyle(Node node) {
        node.setStyle(
                "-fx-background-color: rgba(251, 191, 36, 0.3);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + COLOR_YELLOW + ";" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 8;" +
                        "-fx-pref-width: 120;" +
                        "-fx-pref-height: 160;" +
                        "-fx-alignment: center;" +
                        "-fx-cursor: hand;");

        // Glow effect
        DropShadow glowShadow = new DropShadow();
        glowShadow.setColor(Color.rgb(251, 191, 36, 0.8));
        glowShadow.setRadius(15);
        node.setEffect(glowShadow);
    }

    /**
     * Apply deck slot replace highlight hover style
     */
    public static void applyDeckSlotReplaceHighlightHoverStyle(Node node) {
        node.setStyle(
                "-fx-background-color: rgba(251, 191, 36, 0.5);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + COLOR_YELLOW_DARK + ";" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 8;" +
                        "-fx-pref-width: 120;" +
                        "-fx-pref-height: 160;" +
                        "-fx-alignment: center;" +
                        "-fx-cursor: hand;");

        // Stronger glow effect
        DropShadow glowShadow = new DropShadow();
        glowShadow.setColor(Color.rgb(251, 191, 36, 1.0));
        glowShadow.setRadius(20);
        node.setEffect(glowShadow);

        node.setScaleX(1.05);
        node.setScaleY(1.05);
    }

    /**
     * Apply overlay background style
     */
    public static void applyOverlayBackground(Node node) {
        node.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
    }

    /**
     * Apply overlay card container style
     */
    public static void applyOverlayCardContainer(Node node) {
        node.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 20;");

        // Drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        dropShadow.setRadius(15);
        dropShadow.setOffsetY(5);
        node.setEffect(dropShadow);
    }

    /**
     * Generic helper to apply button style with hover effect
     */
    private static void applyButtonStyle(Button button, String color, String hoverColor) {
        String baseStyle = "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: '" + FONT_FAMILY + "', Arial;" +
                "-fx-background-radius: 8;" +
                "-fx-pref-width: 120;" +
                "-fx-pref-height: 45;" +
                "-fx-cursor: hand;";

        button.setStyle(baseStyle);

        button.setOnMouseEntered(e -> {
            button.setStyle(baseStyle.replace(color, hoverColor));
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
        });
    }

    /**
     * Apply overlay button style (blue)
     */
    public static void applyOverlayButtonStyle(Button button) {
        applyButtonStyle(button, COLOR_BLUE, COLOR_BLUE_DARK);
    }

    /**
     * Apply overlay "Use" button style (green)
     */
    public static void applyOverlayUseButtonStyle(Button button) {
        applyButtonStyle(button, COLOR_GREEN, COLOR_GREEN_DARK);
    }

    /**
     * Apply overlay "Remove" button style (red)
     */
    public static void applyOverlayRemoveButtonStyle(Button button) {
        applyButtonStyle(button, COLOR_RED, COLOR_RED_DARK);
    }

    /**
     * Get inner shadow effect for recessed elements
     */
    public static InnerShadow getInnerShadowEffect() {
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        innerShadow.setRadius(8);
        innerShadow.setOffsetX(2);
        innerShadow.setOffsetY(2);
        return innerShadow;
    }
}
