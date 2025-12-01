import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageChecker {
    public static void main(String[] args) {
        File dir = new File("src/main/resources/images/cards");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));

        if (files == null) {
            System.out.println("No files found.");
            return;
        }

        for (File file : files) {
            try {
                BufferedImage img = ImageIO.read(file);
                System.out.println(file.getName() + ": " + img.getWidth() + "x" + img.getHeight());
            } catch (IOException e) {
                System.out.println("Error reading " + file.getName());
            }
        }
    }
}
