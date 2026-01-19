package com.kuroyale.view.battle;

import javafx.scene.control.Button;
import javafx.scene.text.Font;

/**
 * A 3-dot menu button that opens the emoji panel.
 * Positioned in the top-left corner of the arena.
 */
public class EmojiButton extends Button {

    public EmojiButton() {
        super("..."); // Horizontal ellipsis (3 dots)

        getStyleClass().add("emoji-menu-button");

        // Set fixed size
        setPrefSize(40, 40);
        setMinSize(40, 40);
        setMaxSize(40, 40);

        // Increase font size for the dots
        setFont(new Font(24));
    }
}
