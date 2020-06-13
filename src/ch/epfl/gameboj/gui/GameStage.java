package ch.epfl.gameboj.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;

public final class GameStage extends JFrame implements KeyListener, ActionListener { // extends  Stage{
    //CONSTANTS
    private static final Map<Integer, Joypad.Key> codeMap = createCodeMap();
    private static final Map<Integer, Joypad.Key> textMap = createTextMap();
    private static final int DELAY_IN_MS = 200;
    private static final int DELAY_IN_NS = DELAY_IN_MS * 1000;
    
    //FIELDS
    private final String path;
    private static int screenShotNum = 1;
    private Timer timer;
    private GameBoy gb;
    private ImagePanel panel;
    private Image image;
    
    /**
     * @param str: path of the game stage wanted to be created
     * creates a new stage including the game of the specified path
     */

    public GameStage(String str) {
        super("Gameboi");
        path = str;
        try {
            timer = new Timer(DELAY_IN_MS, this);
            // LcdImage currentImage = gb.lcdController().currentImage();
            BufferedImage temp = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
            panel = new ImagePanel(temp);

            File romFile = new File(path);
            gb = new GameBoy(Cartridge.ofFile(romFile));
            panel.addKeyListener(this);
            timer.start();
            add(panel);
            setResizable(false);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            pack();
            setVisible(true);
         }
         // exits the program in case of an exception
         catch(IOException e){
             System.exit(1);
         }
        
    }
    /**
     * @return the hash map associates the key codes and joypad keys
     */
    private static Map<Integer, Joypad.Key> createCodeMap(){
        Map<Integer, Joypad.Key> codeMap = new HashMap<>();
        codeMap.put(KeyEvent.VK_LEFT, Key.LEFT);
        codeMap.put(KeyEvent.VK_UP, Key.UP);
        codeMap.put(KeyEvent.VK_RIGHT, Key.RIGHT);
        codeMap.put(KeyEvent.VK_DOWN, Key.DOWN);
        return codeMap;
    }
    /**
     * @return the hash map associates the key texts and joypad keys
     */

    private static Map<Integer, Joypad.Key> createTextMap(){
        Map<Integer, Joypad.Key> textMap = new HashMap<>();
        textMap.put(KeyEvent.VK_A, Key.A);
        textMap.put(KeyEvent.VK_B, Key.B);
        textMap.put(KeyEvent.VK_SPACE, Key.START);
        textMap.put(KeyEvent.VK_S, Key.SELECT);
        return textMap;
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        int code = keyEvent.getKeyCode();
        Key joypadKey;

        // reset logic
        if (code == KeyEvent.VK_R) {
            dispose();
           new GameStage(path);
        }
        // screenshot logic
        else if (code == KeyEvent.VK_P) {
            LcdImage lcdImage = gb.lcdController().currentImage();
            BufferedImage buffered = ImageConverter.toBufferedImage(lcdImage);
            try {
                ImageIO.write(buffered, "png",
                        new File("screenshots/screenshot " + screenShotNum + ".png"));
            }
            //exits the program in case of an exception
            catch (IOException e) {
                System.exit(1);
            }
            ++screenShotNum;
        }

        else if(textMap.containsKey(code)) {
            joypadKey = textMap.get(code);
            gb.joypad().keyPressed(joypadKey);
        }
        else if(codeMap.containsKey(code)) {
            joypadKey = codeMap.get(code);
            gb.joypad().keyPressed(joypadKey);
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        int code = keyEvent.getKeyCode();
        Key joypadKey;
        if (textMap.containsKey(code)) {
            joypadKey = textMap.get(code);
            gb.joypad().keyReleased(joypadKey);
        }
        else if (codeMap.containsKey(code)) {
            joypadKey = codeMap.get(code);
            gb.joypad().keyReleased(joypadKey);
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source == timer) {
            LcdImage currentImage = gb.lcdController().currentImage();
            // setIconImage(ImageConverter.convert(currentImage));
            panel.setImage((BufferedImage) ImageConverter.toBufferedImage(currentImage));
            panel.repaint();
            gb.runUntil((long) (GameBoy.CYCLES_PER_NANOSECOND * DELAY_IN_NS));

        }
    }

    private class ImagePanel extends JPanel {
        private BufferedImage image;

        public ImagePanel(BufferedImage img) {
            image = img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, this);
        }

        public void setImage(BufferedImage img) {
            image = img;
        }
    }

}