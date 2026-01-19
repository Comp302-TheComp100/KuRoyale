package com.kuroyale.view.battle.effects;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Roket (Rocket) UI bileşenleri için factory sınıfı.
 * Fireball ile aynı hareket mantığına sahip silindirimsi roket görünümü sağlar.
 */
public class RocketEffect {

        /**
         * Kod ile dinamik bir Roket (Rocket) oluşturur.
         * Silindirimsi görünüm için dikdörtgen gövde, sivri burun ve kuyruk aleviyle.
         *
         * @param size Roketin boyutu (genişlik için referans değer, Örn: 15)
         * @return Oyuna eklenebilecek Node (Group)
         */
        public static Node createProceduralRocket(double size) {
                Group rocket = new Group();

                double bodyWidth = size * 1.2;
                double bodyHeight = size * 3.0;

                // 1. Roket Gövdesi (Silindirimsi dikdörtgen görünüm)
                Rectangle body = new Rectangle(bodyWidth, bodyHeight);
                body.setArcWidth(bodyWidth * 0.6); // Yuvarlatılmış köşeler - silindir etkisi
                body.setArcHeight(bodyWidth * 0.6);
                body.setX(-bodyWidth / 2);
                body.setY(-bodyHeight / 2);

                // Metalik gri-gümüş gradyan (silindir etkisi için)
                LinearGradient bodyGradient = new LinearGradient(
                                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                                new Stop(0.0, Color.rgb(80, 80, 90)), // Sol kenar - koyu
                                new Stop(0.3, Color.rgb(180, 180, 190)), // Açık yansıma
                                new Stop(0.5, Color.rgb(220, 220, 230)), // Merkez - parlak
                                new Stop(0.7, Color.rgb(180, 180, 190)), // Açık yansıma
                                new Stop(1.0, Color.rgb(80, 80, 90)) // Sağ kenar - koyu
                );
                body.setFill(bodyGradient);

                // 2. Roket Burnu (Sivri koni şeklinde)
                Polygon nose = new Polygon();
                nose.getPoints().addAll(
                                0.0, -bodyHeight / 2 - size * 1.2, // Sivri tepe
                                -bodyWidth / 2, -bodyHeight / 2, // Sol alt köşe
                                bodyWidth / 2, -bodyHeight / 2 // Sağ alt köşe
                );

                // Kırmızı burun rengi
                LinearGradient noseGradient = new LinearGradient(
                                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                                new Stop(0.0, Color.rgb(150, 20, 20)),
                                new Stop(0.5, Color.rgb(220, 50, 50)),
                                new Stop(1.0, Color.rgb(150, 20, 20)));
                nose.setFill(noseGradient);

                // 3. Roket Kanatları (Stabilizatör kanatlar)
                double finWidth = bodyWidth * 0.6;
                double finHeight = size * 1.0;

                // Sol kanat
                Polygon leftFin = new Polygon();
                leftFin.getPoints().addAll(
                                -bodyWidth / 2, bodyHeight / 2 - finHeight, // Üst iç
                                -bodyWidth / 2 - finWidth, bodyHeight / 2, // Alt dış
                                -bodyWidth / 2, bodyHeight / 2 // Alt iç
                );
                leftFin.setFill(Color.rgb(60, 60, 70));

                // Sağ kanat
                Polygon rightFin = new Polygon();
                rightFin.getPoints().addAll(
                                bodyWidth / 2, bodyHeight / 2 - finHeight, // Üst iç
                                bodyWidth / 2 + finWidth, bodyHeight / 2, // Alt dış
                                bodyWidth / 2, bodyHeight / 2 // Alt iç
                );
                rightFin.setFill(Color.rgb(60, 60, 70));

                // 4. Kuyruk Alevi (Egzoz alevi)
                Group flame = createFlameEffect(bodyWidth * 0.8, size * 1.5);
                flame.setTranslateY(bodyHeight / 2 + size * 0.3);

                // 5. Tüm parçaları birleştir
                rocket.getChildren().addAll(flame, leftFin, rightFin, body, nose);

                // 6. Parlama Efekti
                DropShadow glow = new DropShadow();
                glow.setColor(Color.ORANGE);
                glow.setRadius(size * 0.8);
                glow.setSpread(0.3);
                glow.setBlurType(BlurType.GAUSSIAN);

                Glow intenseGlow = new Glow(0.4);
                glow.setInput(intenseGlow);

                rocket.setEffect(glow);

                return rocket;
        }

        /**
         * Roket kuyruk alevi efekti oluşturur.
         */
        private static Group createFlameEffect(double width, double height) {
                Group flameGroup = new Group();

                // Dış alev (Turuncu-kırmızı)
                Polygon outerFlame = new Polygon();
                outerFlame.getPoints().addAll(
                                -width / 2, 0.0, // Sol üst
                                0.0, height, // Alt sivri uç
                                width / 2, 0.0 // Sağ üst
                );

                LinearGradient outerFlameGradient = new LinearGradient(
                                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                                new Stop(0.0, Color.ORANGERED),
                                new Stop(0.5, Color.ORANGE),
                                new Stop(1.0, Color.TRANSPARENT));
                outerFlame.setFill(outerFlameGradient);

                // İç alev (Sarı-beyaz - daha sıcak)
                Polygon innerFlame = new Polygon();
                double innerWidth = width * 0.5;
                double innerHeight = height * 0.7;
                innerFlame.getPoints().addAll(
                                -innerWidth / 2, 0.0,
                                0.0, innerHeight,
                                innerWidth / 2, 0.0);

                LinearGradient innerFlameGradient = new LinearGradient(
                                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                                new Stop(0.0, Color.WHITE),
                                new Stop(0.3, Color.YELLOW),
                                new Stop(1.0, Color.TRANSPARENT));
                innerFlame.setFill(innerFlameGradient);

                // Glow efekti
                Glow flameGlow = new Glow(0.8);
                outerFlame.setEffect(flameGlow);

                flameGroup.getChildren().addAll(outerFlame, innerFlame);

                return flameGroup;
        }

        /**
         * Belirtilen konumda bir roket patlama efekti oluşturur, oynatır ve ekrandan
         * siler.
         * Fireball patlamasından biraz farklı - daha yoğun duman ve kıvılcım efekti.
         *
         * @param gamePane Efektin ekleneceği ana oyun paneli.
         * @param x        Patlamanın merkez X koordinatı.
         * @param y        Patlamanın merkez Y koordinatı.
         * @param radius   Patlamanın ulaşacağı maksimum yarıçap (Örn: 80).
         */
        public static void playExplosionEffect(Pane gamePane, double x, double y, double radius) {
                Group explosionGroup = new Group();

                // 1. Ana patlama dairesi (Turuncu-kırmızı)
                Circle mainExplosion = new Circle(radius * 1.8);
                mainExplosion.setCenterX(0);
                mainExplosion.setCenterY(0);

                RadialGradient mainGradient = new RadialGradient(
                                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                                new Stop(0.0, Color.WHITE),
                                new Stop(0.2, Color.YELLOW),
                                new Stop(0.4, Color.ORANGE),
                                new Stop(0.7, Color.ORANGERED),
                                new Stop(1.0, Color.TRANSPARENT));
                mainExplosion.setFill(mainGradient);

                // 2. Duman halkası (Gri-siyah)
                Circle smokeRing = new Circle(radius * 2.2);
                smokeRing.setCenterX(0);
                smokeRing.setCenterY(0);

                RadialGradient smokeGradient = new RadialGradient(
                                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                                new Stop(0.0, Color.TRANSPARENT),
                                new Stop(0.5, Color.rgb(50, 50, 50, 0.4)),
                                new Stop(0.8, Color.rgb(30, 30, 30, 0.2)),
                                new Stop(1.0, Color.TRANSPARENT));
                smokeRing.setFill(smokeGradient);

                // Yoğun parlama efekti
                Glow glowEffect = new Glow(1.0);
                mainExplosion.setEffect(glowEffect);

                explosionGroup.getChildren().addAll(smokeRing, mainExplosion);
                explosionGroup.setTranslateX(x);
                explosionGroup.setTranslateY(y);

                // Başlangıç durumu
                explosionGroup.setScaleX(0.1);
                explosionGroup.setScaleY(0.1);
                explosionGroup.setOpacity(1.0);

                gamePane.getChildren().add(explosionGroup);

                // Animasyonlar
                Duration duration = Duration.millis(450);

                ScaleTransition expand = new ScaleTransition(duration, explosionGroup);
                expand.setToX(1.0);
                expand.setToY(1.0);

                FadeTransition fadeOut = new FadeTransition(duration, explosionGroup);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                ParallelTransition explosionAnim = new ParallelTransition(expand, fadeOut);

                explosionAnim.setOnFinished(event -> {
                        gamePane.getChildren().remove(explosionGroup);
                });

                explosionAnim.play();
        }
}
