package tn.esprit.utils;

import javafx.scene.image.Image;
import java.io.File;

public class ImageUtils {

    public static Image loadImage(String relativePath, double width, double height) {
        if (relativePath == null || relativePath.isBlank()) return null;

        String projectRoot = System.getProperty("user.dir");
        File file = new File(projectRoot + relativePath);

        if (!file.exists()) return null;

        return new Image(file.toURI().toString(), width, height, true, true);
    }
}
