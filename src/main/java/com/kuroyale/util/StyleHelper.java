package com.kuroyale.util;

import javafx.css.PseudoClass;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

/**
 * Utility class for dynamic styling effects and constants
 * Most styling has been migrated to CSS - this class now only contains:
 * - Color and font constants (for inline styles)
 * - Complex programmatic effects that CSS cannot handle
 */
public class StyleHelper {

    // PseudoClass definitions for state management
    public static final PseudoClass HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("hover");
    public static final PseudoClass FILLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("filled");
    public static final PseudoClass HIGHLIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlight");

    // Color constants (used in inline styles where CSS variables aren't accessible)
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

    // Font family (used in inline styles)
    public static final String FONT_FAMILY = "Clash";

    /**
     * Get inner shadow effect for recessed elements
     * This complex effect with specific parameters is kept programmatic
     * as CSS cannot easily replicate the exact InnerShadow parameters
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
