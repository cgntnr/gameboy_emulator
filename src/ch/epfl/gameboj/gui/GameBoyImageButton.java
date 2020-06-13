package ch.epfl.gameboj.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 *class of the button objects on the gameboy opening menu 
 */
public final class GameBoyImageButton extends JButton {

    //CONSTRUCTORS
    /**
     * @param text: initializes the text field
     * @param width : for the width of the button
     * @param height : for the height of the button
     */

    public GameBoyImageButton(String text, int width , int height) {
        super(text);
        File file = new File("images/" + text + ".png");
        try {
            BufferedImage img = ImageIO.read(file);
            BufferedImage resizedImg = getScaledImage(img, width, height);
            setIcon(new ImageIcon(resizedImg));
            setPreferredSize(new Dimension(width, height));
        } catch (IOException ex) {
             throw new IllegalArgumentException("File not found : " + text);
        }
    }

    public static BufferedImage getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }
}