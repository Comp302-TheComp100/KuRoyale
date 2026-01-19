package com.kuroyale.view.draft;

import com.kuroyale.model.entities.Card;
import com.kuroyale.view.card.CardView;

import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Simple UI for draft card selection.
 * Shows two cards in center with timer, cards animate away on selection.
 */
public class DraftCardChoiceView extends VBox {
    
    private static final int TIMER_SECONDS = 15;
    
    private final Label timerLabel;
    private final Label titleLabel;
    private final HBox cardContainer;
    
    private Timeline countdownTimer;
    private int remainingSeconds;
    
    private Card leftCard;
    private Card rightCard;
    private CardView leftCardView;
    private CardView rightCardView;
    
    private Consumer<Card> onCardSelected;
    private Runnable onTimeout;
    
    public DraftCardChoiceView() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setPadding(new Insets(50));
        
        // Title
        titleLabel = new Label("Choose Your Card");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #fbbf24; " +
                           "-fx-effect: dropshadow(gaussian, black, 10, 0.5, 0, 2);");
        
        // Timer
        timerLabel = new Label("15 sec");
        timerLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #22c55e; " +
                           "-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10 25; -fx-background-radius: 10;");
        
        // Card container
        cardContainer = new HBox(60);
        cardContainer.setAlignment(Pos.CENTER);
        
        getChildren().addAll(titleLabel, cardContainer, timerLabel);
    }
    
    /**
     * Presents two cards for the player to choose from.
     */
    public void presentChoice(Card card1, Card card2, int round, int totalRounds) {
        this.leftCard = card1;
        this.rightCard = card2;
        
        // Update title
        titleLabel.setText("Choose Your Card (" + round + "/" + totalRounds + ")");
        
        // Clear previous cards
        cardContainer.getChildren().clear();
        
        // Create clickable card views
        leftCardView = createClickableCard(card1, true);
        rightCardView = createClickableCard(card2, false);
        
        cardContainer.getChildren().addAll(leftCardView, rightCardView);
        
        // Entrance animation
        playEntranceAnimation();
        
        // Start countdown
        startCountdown();
    }
    
    private CardView createClickableCard(Card card, boolean isLeft) {
        CardView cardView = new CardView(card);
        cardView.setScaleX(1.5);
        cardView.setScaleY(1.5);
        cardView.setStyle(cardView.getStyle() + "-fx-cursor: hand;");
        
        // Hover effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(30);
        glow.setSpread(0.5);
        
        cardView.setOnMouseEntered(e -> {
            cardView.setEffect(glow);
            cardView.setScaleX(1.65);
            cardView.setScaleY(1.65);
        });
        
        cardView.setOnMouseExited(e -> {
            cardView.setEffect(null);
            cardView.setScaleX(1.5);
            cardView.setScaleY(1.5);
        });
        
        // Click handler
        cardView.setOnMouseClicked(e -> {
            stopCountdown();
            animateCardSelection(card, isLeft);
        });
        
        return cardView;
    }
    
    /**
     * Plays entrance animation for cards appearing.
     */
    private void playEntranceAnimation() {
        // Start from center, scale up
        leftCardView.setScaleX(0);
        leftCardView.setScaleY(0);
        rightCardView.setScaleX(0);
        rightCardView.setScaleY(0);
        
        ScaleTransition scaleLeft = new ScaleTransition(Duration.millis(300), leftCardView);
        scaleLeft.setToX(1.5);
        scaleLeft.setToY(1.5);
        
        ScaleTransition scaleRight = new ScaleTransition(Duration.millis(300), rightCardView);
        scaleRight.setToX(1.5);
        scaleRight.setToY(1.5);
        scaleRight.setDelay(Duration.millis(100));
        
        scaleLeft.play();
        scaleRight.play();
    }
    
    /**
     * Animates cards flying away: chosen goes down, rejected goes up.
     */
    private void animateCardSelection(Card chosenCard, boolean choseLeft) {
        CardView chosenCardView = choseLeft ? leftCardView : rightCardView;
        CardView rejectedCardView = choseLeft ? rightCardView : leftCardView;
        
        // Chosen card flies DOWN and fades
        TranslateTransition moveChosen = new TranslateTransition(Duration.millis(500), chosenCardView);
        moveChosen.setToY(400);
        
        FadeTransition fadeChosen = new FadeTransition(Duration.millis(500), chosenCardView);
        fadeChosen.setToValue(0);
        
        ScaleTransition shrinkChosen = new ScaleTransition(Duration.millis(500), chosenCardView);
        shrinkChosen.setToX(0.8);
        shrinkChosen.setToY(0.8);
        
        // Rejected card flies UP and fades
        TranslateTransition moveRejected = new TranslateTransition(Duration.millis(500), rejectedCardView);
        moveRejected.setToY(-400);
        
        FadeTransition fadeRejected = new FadeTransition(Duration.millis(500), rejectedCardView);
        fadeRejected.setToValue(0);
        
        ScaleTransition shrinkRejected = new ScaleTransition(Duration.millis(500), rejectedCardView);
        shrinkRejected.setToX(0.8);
        shrinkRejected.setToY(0.8);
        
        // Play animations
        ParallelTransition parallel = new ParallelTransition(
            moveChosen, fadeChosen, shrinkChosen,
            moveRejected, fadeRejected, shrinkRejected
        );
        
        parallel.setOnFinished(e -> {
            // Notify callback
            if (onCardSelected != null) {
                onCardSelected.accept(chosenCard);
            }
        });
        
        parallel.play();
    }
    
    private void startCountdown() {
        remainingSeconds = TIMER_SECONDS;
        updateTimerDisplay();
        
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerDisplay();
            
            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                if (onTimeout != null) {
                    onTimeout.run();
                }
            }
        }));
        countdownTimer.setCycleCount(TIMER_SECONDS);
        countdownTimer.play();
    }
    
    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }
    
    private void updateTimerDisplay() {
        timerLabel.setText(remainingSeconds + " sec");
        
        // Change color based on time remaining
        if (remainingSeconds <= 5) {
            timerLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ef4444; " +
                               "-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10 25; -fx-background-radius: 10;");
        } else if (remainingSeconds <= 10) {
            timerLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #f59e0b; " +
                               "-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10 25; -fx-background-radius: 10;");
        } else {
            timerLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #22c55e; " +
                               "-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10 25; -fx-background-radius: 10;");
        }
    }
    
    public void setOnCardSelected(Consumer<Card> callback) {
        this.onCardSelected = callback;
    }
    
    public void setOnTimeout(Runnable callback) {
        this.onTimeout = callback;
    }
    
    public Card getLeftCard() {
        return leftCard;
    }
    
    public Card getRightCard() {
        return rightCard;
    }
    
    public void cleanup() {
        stopCountdown();
    }
}
