package ch.epfl.gameboj.gui;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.Objects;

/**
 *class of the button objects on the gameboy opening menu 
 */
public final class GameBoyImageButton extends Button {
    //FIELDS
    private final String text;
    private final ImageView imageView;
    
    //CONSTRUCTORS
    /**
     * @param text: initializes the text field
     * @param width : for the width of the button
     * @param height : for the height of the button
     */
    public GameBoyImageButton(String text,int width , int height) {
        this.text = text;

        // File file = new File("images/" + text + ".png");
        InputStream is = getClass().getResourceAsStream("/images/" + text + ".png");
        Objects.requireNonNull(is);

        Image image = new Image(is);
        imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        setGraphic(imageView);

    }
    
    //METHODS
    /**
     * @return the text represents the path behind the image
     */
    public String text() {
        return text;
    }
}