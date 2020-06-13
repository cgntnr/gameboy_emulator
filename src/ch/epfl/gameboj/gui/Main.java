package ch.epfl.gameboj.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class Main extends JFrame implements ActionListener {
    // CONSTANTS
    private final static int BUTTON_NUM_PER_ROW = 5;
    private final static String[] currentGames =  {"2048","batman","bomberman","donkey kong"
            ,"fifa98","hugo","mario1","mario2","mortal kombat"
            ,"pacman","spiderman","street fighter 2"
            ,"tasmania story","tetris" , "zelda"};
    private final static int BUTTON_LENGTH = 175;
    private static final Font FONT = new Font("Arial", Font.BOLD, 30);

    // FIELDS
    private JLabel label;
    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel mainPanel;

    // CONSTRUCTORS
    public Main() {
        super("Gameboy Emulator");
        topPanel = new JPanel();
        centerPanel = new JPanel();
        mainPanel = new JPanel();
        label = new JLabel("Welcome to the Gameboy Emulator");

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        centerPanel.setLayout(new GridLayout(currentGames.length / BUTTON_NUM_PER_ROW, BUTTON_NUM_PER_ROW));

        //creating and adding the image buttons to the grid pane
        for(String text: currentGames) {
            GameBoyImageButton button = new GameBoyImageButton(text, BUTTON_LENGTH, BUTTON_LENGTH);
            button.addActionListener(this);
            centerPanel.add(button);
        }

        //arranging the fonts and the colors of the nodes
        label.setForeground(Color.BLUE);
        label.setFont(FONT);
        topPanel.setBackground(Color.YELLOW);
        topPanel.add(label);


        //arranging the stage and finally make it visible
        add(mainPanel);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if(source instanceof GameBoyImageButton) {
            GameBoyImageButton button  = (GameBoyImageButton) source;
            GameStage game = new GameStage("games/" + button.getText() + ".gb");
            label.setText("Current Game: " + button.getText());
            // dispose();
        }
    }

    // Main Method
    public static void main(String[] args){
        new Main();
    }
}
