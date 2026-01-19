package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.entities.Vector2;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.enums.ComboType;
import com.kuroyale.view.battle.component.PngSequenceSprite;
import com.kuroyale.view.battle.effects.ActiveSpellVisual;
import com.kuroyale.view.battle.effects.ArrowsEffect;
import com.kuroyale.view.battle.effects.FireballEffect;
import com.kuroyale.view.battle.effects.RocketEffect;
import com.kuroyale.view.battle.effects.ZapEffect;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Manages all visual effects in the battle arena.
 * Responsibilities:
 * - Spell effects (Fireball, Zap, Arrows, Rocket)
 * - Combo effects (Tank Support, Swarm, etc.)
 * - Tower damage and death effects
 * - Combo text animations
 */
public class ArenaEffectManager {
    private static final int TILE_SIZE = com.kuroyale.util.GameConstants.TILE_SIZE;

    private final Pane arenaPane;
    private final Pane unitLayer;
    private final Pane effectLayer;

    private final java.util.List<ActiveSpellVisual> activeSpellVisuals = new java.util.ArrayList<>();

    public ArenaEffectManager(Pane arenaPane, Pane unitLayer, Pane effectLayer) {
        this.arenaPane = arenaPane;
        this.unitLayer = unitLayer;
        this.effectLayer = effectLayer;
    }

    /**
     * Updates active spell effects, removing expired ones.
     */
    public void update(double deltaTime) {
        java.util.Iterator<ActiveSpellVisual> it = activeSpellVisuals.iterator();
        while (it.hasNext()) {
            ActiveSpellVisual visual = it.next();
            visual.timeRemaining -= deltaTime;
            if (visual.timeRemaining <= 0) {
                if (visual.node instanceof PngSequenceSprite) {
                    ((PngSequenceSprite) visual.node).stop();
                }
                unitLayer.getChildren().remove(visual.node);
                it.remove();
            }
        }
    }

    /**
     * Shows combo text animation in the center of the arena.
     */
    public void showComboText(String text) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(text + "!");
        label.setStyle(
                "-fx-font-size: 32px; -fx-text-fill: gold; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);");

        label.layoutXProperty().bind(arenaPane.widthProperty().subtract(label.widthProperty()).divide(2));
        label.layoutYProperty()
                .bind(arenaPane.heightProperty().subtract(label.heightProperty()).divide(2).subtract(200));

        arenaPane.getChildren().add(label);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2.0),
                label);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> arenaPane.getChildren().remove(label));

        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(2.0), label);
        tt.setByY(-50);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
        pt.play();
    }

    /**
     * Plays damage flash effect on a tower.
     */
    public void playTowerDamageEffect(Tower tower, javafx.scene.Node towerNode) {
        if (towerNode != null && towerNode instanceof StackPane) {
            StackPane stack = (StackPane) towerNode;

            // Red overlay flash
            Rectangle overlay = new Rectangle(stack.getWidth(), stack.getHeight());
            overlay.setFill(Color.RED);
            overlay.setOpacity(0.0);
            overlay.setMouseTransparent(true);

            stack.getChildren().add(overlay);

            javafx.animation.FadeTransition flash = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(100), overlay);
            flash.setFromValue(0.0);
            flash.setToValue(0.3);
            flash.setCycleCount(2);
            flash.setAutoReverse(true);
            flash.setOnFinished(e -> stack.getChildren().remove(overlay));
            flash.play();

            // Shake effect
            javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(50), towerNode);
            shake.setByX(2);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }

    /**
     * Plays explosion effect when a tower is destroyed.
     */
    public void playTowerDeathEffect(javafx.geometry.Point2D center) {
        if (center == null)
            return;

        double startX = center.getX();
        double startY = center.getY();

        // Procedural explosion
        Circle explosionCore = new Circle(10, Color.ORANGE);
        explosionCore.setStroke(Color.RED);
        explosionCore.setStrokeWidth(2);
        explosionCore.setTranslateX(startX);
        explosionCore.setTranslateY(startY);

        Circle explosionRing = new Circle(10, Color.TRANSPARENT);
        explosionRing.setStroke(Color.YELLOW);
        explosionRing.setStrokeWidth(4);
        explosionRing.setTranslateX(startX);
        explosionRing.setTranslateY(startY);

        effectLayer.getChildren().addAll(explosionCore, explosionRing);

        javafx.animation.Timeline explodeAnim = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(explosionCore.radiusProperty(), 10),
                        new javafx.animation.KeyValue(explosionCore.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400),
                        new javafx.animation.KeyValue(explosionCore.radiusProperty(), 60),
                        new javafx.animation.KeyValue(explosionCore.opacityProperty(), 0.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(explosionRing.radiusProperty(), 10),
                        new javafx.animation.KeyValue(explosionRing.opacityProperty(), 1.0),
                        new javafx.animation.KeyValue(explosionRing.strokeWidthProperty(), 4)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600),
                        new javafx.animation.KeyValue(explosionRing.radiusProperty(), 80),
                        new javafx.animation.KeyValue(explosionRing.opacityProperty(), 0.0),
                        new javafx.animation.KeyValue(explosionRing.strokeWidthProperty(), 0)));

        explodeAnim.setOnFinished(e -> effectLayer.getChildren().removeAll(explosionCore, explosionRing));
        explodeAnim.play();
    }

    /**
     * Plays area effect spell animations (Fireball, Zap, Arrows, Rocket).
     */
    public void playAreaEffect(boolean isPlayerSource, Vector2 center, double radius,
            double duration, String effectType) {
        javafx.application.Platform.runLater(() -> {
            boolean isFireball = "Fireball".equalsIgnoreCase(effectType);
            boolean isRocket = "Rocket".equalsIgnoreCase(effectType);
            boolean isZap = "Zap".equalsIgnoreCase(effectType);
            boolean isArrow = "Arrows".equalsIgnoreCase(effectType);

            double cx = center.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = center.getY() * TILE_SIZE + (TILE_SIZE / 2.0);
            double rPixels = radius * TILE_SIZE;

            if (isFireball) {
                FireballEffect.playExplosionEffect(unitLayer, cx, cy, rPixels);
            } else if (isRocket) {
                RocketEffect.playExplosionEffect(unitLayer, cx, cy, rPixels);
            } else if (isZap) {
                ZapEffect.play(unitLayer, cx, cy, rPixels, duration, activeSpellVisuals);
            } else if (isArrow) {
                ArrowsEffect.play(unitLayer, isPlayerSource, cx, cy, rPixels, duration, activeSpellVisuals);
            }
        });
    }

    /**
     * Plays spell cast animation (PNG sequence).
     */
    public void playSpellCast(boolean isPlayer, Card spell, GridPosition center) {
        if (spell == null || center == null || spell.getType() != CardType.SPELL) {
            return;
        }

        javafx.application.Platform.runLater(() -> {
            String cardKey = PngSequenceSprite.toCardKey(spell.getName());
            String side = isPlayer ? "player" : "enemy";
            String folder = "/images/animations/spells/" + cardKey + "/" + side + "/attack";

            double rPixels = Math.max(1.0, spell.getRange()) * TILE_SIZE;
            double size = rPixels * 2.0;

            double totalDurationSeconds = 0.7;

            PngSequenceSprite sprite = new PngSequenceSprite(folder, size, size);
            sprite.setLoop(false);
            sprite.setLoopDurationSeconds(totalDurationSeconds);
            sprite.setMouseTransparent(true);

            double cx = center.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = center.getY() * TILE_SIZE + (TILE_SIZE / 2.0);
            sprite.setLayoutX(cx - size / 2.0);
            sprite.setLayoutY(cy - size / 2.0);

            unitLayer.getChildren().add(sprite);

            final ActiveSpellVisual[] visualRef = new ActiveSpellVisual[1];
            ActiveSpellVisual visual = new ActiveSpellVisual(sprite, totalDurationSeconds + 0.2);
            visualRef[0] = visual;
            activeSpellVisuals.add(visual);

            sprite.setOnFinished(() -> {
                if (visualRef[0] != null) {
                    visualRef[0].timeRemaining = 0.0;
                }
            });
        });
    }

    /**
     * Shows combo-specific visual effects.
     */
    public void showComboEffect(ComboType combo, java.util.List<ICombatant> affectedUnits) {
        double duration = 2.0;

        switch (combo) {
            case TANK_SUPPORT:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addVisualEffect(unit, duration, node -> {
                            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                            glow.setColor(Color.GOLD);
                            glow.setRadius(20);
                            glow.setSpread(0.5);
                            node.setEffect(glow);
                        });
                    }
                }
                break;

            case SPELL_SYNERGY:
                playSpellSynergyEffect();
                break;

            case SWARM_ATTACK:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addSwarmLines(unit, duration);
                    }
                }
                break;

            case BUILDING_DEFENSE:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addShieldEffect(unit, duration);
                    }
                }
                break;

            case AIR_ASSAULT:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addLightningEffect(unit, duration);
                    }
                }
                break;

            case ROYAL_COMBO:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addCrownEffect(unit, duration);
                    }
                }
                break;

            case SIEGE_MODE:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addRangeCircle(unit, duration);
                    }
                }
                break;

            case RUSH_ATTACK:
                if (affectedUnits != null) {
                    for (ICombatant unit : affectedUnits) {
                        addDustTrail(unit, duration);
                    }
                }
                break;

            default:
                if (affectedUnits != null && !affectedUnits.isEmpty()) {
                    for (ICombatant unit : affectedUnits) {
                        addDefaultComboRing(unit, duration);
                    }
                }
                break;
        }
    }

    private void playSpellSynergyEffect() {
        javafx.application.Platform.runLater(() -> {
            double cx = com.kuroyale.model.entities.Arena.WIDTH * TILE_SIZE / 2.0;
            double cy = com.kuroyale.model.entities.Arena.HEIGHT * TILE_SIZE / 2.0;

            for (int i = 0; i < 10; i++) {
                Circle sparkle = new Circle(cx, cy, 5, Color.CYAN);
                sparkle.setEffect(new javafx.scene.effect.Glow(1.0));
                unitLayer.getChildren().add(sparkle);

                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                        javafx.util.Duration.seconds(1.0), sparkle);
                tt.setByX((Math.random() - 0.5) * 100);
                tt.setByY((Math.random() - 0.5) * 100);

                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                        javafx.util.Duration.seconds(1.0), sparkle);
                ft.setToValue(0);

                javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
                pt.setOnFinished(e -> unitLayer.getChildren().remove(sparkle));
                pt.play();
            }
        });
    }

    private void addVisualEffect(ICombatant unit, double duration,
            java.util.function.Consumer<javafx.scene.Node> effectApplier) {
        if (unit instanceof com.kuroyale.model.entities.Troop) {
            GridPosition pos = unit.getCenterPosition();
            if (pos != null) {
                double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                Circle highlight = new Circle(cx, cy, TILE_SIZE / 2);
                highlight.setFill(null);
                highlight.setStroke(Color.GOLD);
                highlight.setStrokeWidth(3);

                effectApplier.accept(highlight);

                unitLayer.getChildren().add(highlight);
                activeSpellVisuals.add(new ActiveSpellVisual(highlight, duration));
            }
        }
    }

    private void addSwarmLines(ICombatant unit, double duration) {
        GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(cx, cy + 15, cx - 25, cy + 15);
            line1.setStroke(Color.WHITE);
            line1.setStrokeWidth(1.5);
            line1.getStrokeDashArray().addAll(4d, 6d);

            javafx.scene.shape.Line line2 = new javafx.scene.shape.Line(cx, cy - 15, cx - 25, cy - 15);
            line2.setStroke(Color.WHITE);
            line2.setStrokeWidth(1.5);
            line2.getStrokeDashArray().addAll(4d, 6d);

            unitLayer.getChildren().addAll(line1, line2);
            activeSpellVisuals.add(new ActiveSpellVisual(line1, duration));
            activeSpellVisuals.add(new ActiveSpellVisual(line2, duration));
        }
    }

    private void addShieldEffect(ICombatant unit, double duration) {
        GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            javafx.scene.shape.SVGPath shield = new javafx.scene.shape.SVGPath();
            shield.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
            shield.setFill(Color.LIGHTBLUE);
            shield.setStroke(Color.BLUE);
            shield.setScaleX(1.5);
            shield.setScaleY(1.5);
            shield.setLayoutX(cx - 12);
            shield.setLayoutY(cy - 12);

            unitLayer.getChildren().add(shield);
            activeSpellVisuals.add(new ActiveSpellVisual(shield, duration));
        }
    }

    private void addLightningEffect(ICombatant unit, double duration) {
        GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            javafx.scene.shape.Polyline lightning = new javafx.scene.shape.Polyline();
            lightning.getPoints().addAll(
                    cx, cy - 20.0,
                    cx + 5.0, cy - 10.0,
                    cx - 5.0, cy,
                    cx + 5.0, cy + 10.0,
                    cx, cy + 20.0);
            lightning.setStroke(Color.YELLOW);
            lightning.setStrokeWidth(2);
            lightning.setEffect(new javafx.scene.effect.Glow(0.8));

            unitLayer.getChildren().add(lightning);
            activeSpellVisuals.add(new ActiveSpellVisual(lightning, duration));
        }
    }

    private void addCrownEffect(ICombatant unit, double duration) {
        GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/images/crown.png")));
            crown.setFitWidth(24);
            crown.setFitHeight(24);
            crown.setLayoutX(cx - 12);
            crown.setLayoutY(cy - 30);

            unitLayer.getChildren().add(crown);
            activeSpellVisuals.add(new ActiveSpellVisual(crown, duration));
        }
    }

    private void addRangeCircle(ICombatant unit, double duration) {
        GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            Circle rangeCircle = new Circle(cx, cy, TILE_SIZE * 2);
            rangeCircle.setFill(null);
            rangeCircle.setStroke(Color.ORANGE);
            rangeCircle.setStrokeWidth(2);
            rangeCircle.getStrokeDashArray().addAll(10d, 10d);

            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                    javafx.util.Duration.seconds(1.0), rangeCircle);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.5);
            st.setToY(1.5);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();

            unitLayer.getChildren().add(rangeCircle);
            activeSpellVisuals.add(new ActiveSpellVisual(rangeCircle, duration));
        }
    }

    private void addDustTrail(ICombatant unit, double duration) {
        final javafx.animation.Timeline dust = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> {
                    GridPosition pos = unit.getCenterPosition();
                    if (pos == null)
                        return;
                    double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                    double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                    Circle dustParticle = new Circle(cx, cy, 5, Color.GRAY);
                    dustParticle.setOpacity(0.6);
                    unitLayer.getChildren().add(dustParticle);

                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                            javafx.util.Duration.seconds(0.5), dustParticle);
                    ft.setToValue(0);
                    ft.setOnFinished(ev -> unitLayer.getChildren().remove(dustParticle));
                    ft.play();
                }));
        dust.setCycleCount(10);
        dust.play();
    }

    private void addDefaultComboRing(ICombatant unit, double duration) {
        GridPosition pos = unit.getCenterPosition();
        if (pos == null)
            return;

        double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
        double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

        Circle ring = new Circle(cx, cy, TILE_SIZE * 0.8);
        ring.setFill(null);
        ring.setStroke(Color.GOLD);
        ring.setStrokeWidth(3.0);
        ring.setEffect(new javafx.scene.effect.Glow(0.8));

        unitLayer.getChildren().add(ring);
        activeSpellVisuals.add(new ActiveSpellVisual(ring, duration));

        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                javafx.util.Duration.seconds(0.5), ring);
        st.setFromX(0.5);
        st.setFromY(0.5);
        st.setToX(1.2);
        st.setToY(1.2);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}
