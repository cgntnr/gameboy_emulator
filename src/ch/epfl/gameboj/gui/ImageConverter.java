package ch.epfl.gameboj.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Objects;

import ch.epfl.gameboj.component.lcd.LcdImage;

public final class ImageConverter {

    // array represents ARGB values of the gameboy colors
    private static final int[] COLOR_MAP = new int[] {
            //0xFF_00_43_33, 0xFF_0D_88_33, 0xFF_A1_BC_00, 0xFF_EB_DD_77};
            0xFF_FF_FF_FF, 0xFF_D3_D3_D3, 0xFF_A9_A9_A9, 0xFF_00_00_00 };

    /**
     * @param lcdImage: LCD image of the gameboy
     * @return the javafx.scene.image.Image version of the lcdImage
     */
    /*
    public static Image convert(LcdImage lcdImage) {
        Objects.requireNonNull(lcdImage);
        final int width = lcdImage.width();
        final int height = lcdImage.height();
        // WritableImage writableIm = new WritableImage(width, height);
        // PixelWriter pw = writableIm.getPixelWriter();
        /*
        BufferedImage in = ImageIO.read(img);

        BufferedImage newImage = new BufferedImage(
                in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = newImage.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                pw.setArgb(x, y, COLOR_MAP[lcdImage.get(x, y)]);
            }
        }
        return writableIm;


        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                img.setRGB(x, y, ImageConverter.COLOR_MAP[lcdImage.get(x, y)]);
            }
        }
        return img;
    }
     */
    
    
    
    /**
     * @param lcdImage: LCD image of the gameboy
     * @return buffered image version of the lcdImage
     */
    public static BufferedImage toBufferedImage(LcdImage lcdImage) {     
        int width = lcdImage.width();
        int height = lcdImage.height();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; ++y) {
          for (int x = 0; x < width; ++x) {
              bufferedImage.setRGB(x, y, ImageConverter.COLOR_MAP[lcdImage.get(x, y)]);
          }                       
        }
        return bufferedImage;
    }
}