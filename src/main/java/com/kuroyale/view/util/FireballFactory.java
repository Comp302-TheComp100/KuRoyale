package com.kuroyale.view.util;

import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

public class FireballFactory {

    /**
     * Kod ile dinamik bir Ateş Topu (Fireball) oluşturur.
     * Resim dosyası (PNG) kullanmaz, JavaFX Shape kullanır.
     *
     * @param radius Ateş topunun yarıçapı (Örn: 20)
     * @return Oyuna eklenebilecek Node (Circle)
     */
    public static Node createProceduralFireball(double radius) {
        // 1. Daireyi oluştur
        Circle fireball = new Circle(2 * radius);

        // 2. Ateş Efekti için Radyal Gradyan (Radial Gradient) Oluştur
        // Merkez beyaz (çok sıcak), dışarısı sarı -> turuncu -> kırmızı -> şeffaf
        RadialGradient fireGradient = new RadialGradient(
                0, // focusAngle
                0, // focusDistance
                0.5, // centerX (0.0 - 1.0 arası oransal)
                0.5, // centerY
                0.5, // radius (dolgu yarıçapı)
                true, // proportional (oransal mı?)
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE), // Merkez: Beyaz (Kor)
                new Stop(0.2, Color.YELLOW), // Orta: Sarı
                new Stop(0.5, Color.ORANGE), // Dış: Turuncu
                new Stop(0.9, Color.RED), // En Dış: Kırmızı
                new Stop(1.0, Color.TRANSPARENT) // Kenar: Şeffaf geçiş
        );

        fireball.setFill(fireGradient);

        // 3. Parlama Efekti (Glow & Bloom)
        // Ateşin etrafına ışık saçması için DropShadow
        DropShadow glow = new DropShadow();
        glow.setColor(Color.ORANGERED);
        glow.setRadius(radius * 1.5); // Topun kendisinden daha geniş bir parlama
        glow.setSpread(0.6); // Parlamanın yoğunluğu
        glow.setBlurType(BlurType.GAUSSIAN);

        // Ekstra parlaklık için Glow efekti
        Glow intenseGlow = new Glow(0.8); // 0.0 ile 1.0 arası parlaklık
        glow.setInput(intenseGlow); // Glow efektini Shadow'un içine bağla

        fireball.setEffect(glow);

        return fireball;
    }

    /**
     * Belirtilen konumda bir patlama efekti oluşturur, oynatır ve ekrandan siler.
     *
     * @param gamePane Efektin ekleneceği ana oyun paneli.
     * @param x        Patlamanın merkez X koordinatı.
     * @param y        Patlamanın merkez Y koordinatı.
     * @param radius   Patlamanın ulaşacağı maksimum yarıçap (Örn: 100).
     */
    public static void playExplosionEffect(javafx.scene.layout.Pane gamePane, double x, double y, double radius) {
        // 1. Patlama dairesini oluştur (Ateş topuna benzer ama daha parlak merkezli)
        Circle explosion = new Circle(2 * radius);
        explosion.setCenterX(x);
        explosion.setCenterY(y);

        RadialGradient explosionGradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE), // Çok parlak merkez
                new Stop(0.3, Color.YELLOW),
                new Stop(0.6, Color.ORANGERED),
                new Stop(1.0, Color.TRANSPARENT) // Kenarlar şeffaf
        );
        explosion.setFill(explosionGradient);

        // Yoğun bir parlama efekti ekle
        Glow glowEffect = new Glow(1.0);
        explosion.setEffect(glowEffect);

        // 2. Başlangıç durumu (Küçük ve görünür)
        explosion.setScaleX(0.1);
        explosion.setScaleY(0.1);
        explosion.setOpacity(1.0);

        // Pane'e ekle ki görünsün
        gamePane.getChildren().add(explosion);

        // 3. Animasyonları Hazırla
        javafx.util.Duration duration = javafx.util.Duration.millis(400); // Patlama süresi (ms)

        // Hızla büyüme animasyonu (Scale)
        javafx.animation.ScaleTransition expand = new javafx.animation.ScaleTransition(duration, explosion);
        expand.setToX(1.0);
        expand.setToY(1.0);

        // Sönerek kaybolma animasyonu (Fade)
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(duration, explosion);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // İkisini aynı anda oynat (Parallel)
        javafx.animation.ParallelTransition explosionAnim = new javafx.animation.ParallelTransition(expand, fadeOut);

        // 4. ÇOK ÖNEMLİ: Animasyon bitince nesneyi ekrandan sil (Temizlik)
        explosionAnim.setOnFinished(event -> {
            gamePane.getChildren().remove(explosion);
        });

        // Animasyonu başlat
        explosionAnim.play();
    }
}
