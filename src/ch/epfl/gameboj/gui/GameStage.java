package ch.epfl.gameboj.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public final class GameStage extends Stage {
    //CONSTANTS
    private static final Map<KeyCode, Joypad.Key> codeMap = createCodeMap();
    private static final Map<String, Joypad.Key> textMap = createTextMap();
    
    //FIELDS
    private final String path;
    private static int screenShotNum = 1;
    
    /**
     * @param str: path of the game stage wanted to be created
     * creates a new stage including the game of the specified path
     */
    public GameStage(String str) {
        path = str;
        //instantiates local variables
        BorderPane pane = new BorderPane();
        ImageView iv = new  ImageView();    
        File romFile = new File(getClass().getResource(path));
         
         try {
             GameBoy gb = new GameBoy(Cartridge.ofFile(romFile));
             long start = System.nanoTime();      
             iv.setFitWidth(LcdController.LCD_WIDTH * 3);
             iv.setFitHeight(LcdController.LCD_HEIGHT * 3);
             
             //anonymous class for the timer object
             AnimationTimer timer = new AnimationTimer() {
                 @Override
                 public void handle(long now) {           
                   long elapsed = now - start;      
                   LcdImage currentImage = gb.lcdController().currentImage(); 
                   iv.setImage(ImageConverter.convert(currentImage));       
                   gb.runUntil((long)(GameBoy.CYCLES_PER_NANOSECOND * elapsed ));  
              
               }
             };
             
             //listens the keyboard events
             pane.setCenter(iv);        
             pane.setOnKeyPressed(ke -> {                  
                 String text = ke.getText();
                 KeyCode code = ke.getCode();
                 Key joypadKey;
                 
                 //reset logic
                 if(text.equals("R") || text.equals("r")) {
                     this.close();
                     new GameStage(path);
                 }
                 
                 //screenshot logic
                 else if(text.equals("P") || text.equals("p")) {
                     LcdImage lcdImage = gb.lcdController().currentImage();
                     BufferedImage buffered = ImageConverter.toBufferedImage(lcdImage);                
                     try {
                        ImageIO.write(buffered, "png", new File("screenshots/screenshot " + screenShotNum + ".png"));
                    } 
                     //exits the program in case of an exception
                     catch (IOException e) {
                          System.exit(1);
                    }
                     ++screenShotNum;
                 }
                 else if(textMap.containsKey(text)) {
                     joypadKey = textMap.get(text);
                     gb.joypad().keyPressed(joypadKey);
                 }
                 else if(codeMap.containsKey(code)) {
                     joypadKey = codeMap.get(ke.getCode());
                     gb.joypad().keyPressed(joypadKey);
                 }
                
             });
             
             
             pane.setOnKeyReleased(ke -> {
                 String text = ke.getText();
                 KeyCode code = ke.getCode();
                 Key joypadKey;
                 if(textMap.containsKey(text)) {
                     joypadKey = textMap.get(text);
                     gb.joypad().keyReleased(joypadKey);
                 }
                 else if(codeMap.containsKey(code)) {
                     joypadKey = codeMap.get(ke.getCode());
                     gb.joypad().keyReleased(joypadKey);
                 }          
             });
             //starts the timer arranges the scene and the panel then make the game visible
             timer.start();       
             Scene scene = new Scene(pane);
             setScene(scene);
             setResizable(false);
             setTitle("Gameboi");
             show();
             iv.requestFocus();
         }
         //exits the program in case of an exception
         catch(IOException e){
             System.exit(1);
         }
        
    }
    
    /**
     * @return the hash map associates the key codes and joypad keys
     */

    private static Map<KeyCode, Joypad.Key> createCodeMap(){
        Map<KeyCode, Joypad.Key> codeMap = new HashMap<>();
        codeMap.put(KeyCode.LEFT, Key.LEFT);
        codeMap.put(KeyCode.UP, Key.UP);
        codeMap.put(KeyCode.RIGHT, Key.RIGHT);
        codeMap.put(KeyCode.DOWN, Key.DOWN);
        return codeMap;
    }

    /**
     * @return the hash map associates the key texts and joypad keys
     */

    private static Map<String, Joypad.Key> createTextMap(){
        Map<String, Joypad.Key> textMap = new HashMap<>();
        
        textMap.put("a", Key.A);
        textMap.put("b", Key.B);
        textMap.put(" ", Key.START);
        textMap.put("s", Key.SELECT);
        
        textMap.put("A", Key.A);
        textMap.put("B", Key.B);
        textMap.put("S", Key.SELECT);
        return textMap;
    }
}