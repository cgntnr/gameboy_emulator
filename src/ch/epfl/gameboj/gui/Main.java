package ch.epfl.gameboj.gui;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application{

    
    //CONSTANTS
    private final static int BUTTON_NUM_PER_ROW = 5;  
    private final static String[] currentGames =  {"2048","batman","bomberman","donkey kong"
            ,"fifa98","hugo","mario1","mario2","mortal kombat"
            ,"pacman","spiderman","street fighter 2" 
            ,"tasmania story","tetris" , "zelda"};
    private final static int BUTTON_LENGTH = 175;
   
    //METHODS
    
    /* (non-Javadoc)
     * @see javafx.application.Application#start(javafx.stage.Stage)
     * initializing the graphical user interface
     */
    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane mainPane = new BorderPane();
        BorderPane topPane = new BorderPane();
        GridPane centerPane = new GridPane();
        Scene scene = new Scene(mainPane);
        Label label = new Label("Welcome to the Gameboy Emulator");
        
        //anonymous class of event handler for buttons to listen action events
        EventHandler<ActionEvent> eventHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent ae) {
                GameBoyImageButton b  = (GameBoyImageButton)ae.getSource();
                GameStage game = new GameStage("games/" + b.text() + ".gb");
                label.setText("Current Game: " + b.text());           
            }
        }; 
        
        //creating and adding the image buttons to the grid pane
        int colIndex,rowIndex;
        for(int i = 0 ; i < currentGames.length ; ++i ) {
            colIndex = i % BUTTON_NUM_PER_ROW;
            rowIndex = i / BUTTON_NUM_PER_ROW;
            String text = currentGames[i];
            GameBoyImageButton button = new GameBoyImageButton(text,BUTTON_LENGTH,BUTTON_LENGTH);
            button.setOnAction(eventHandler);
            centerPane.add(button, colIndex, rowIndex);
        }
        
        //arranging the fonts and the colors of the nodes
        label.setTextFill(Color.BLUE); 
        label.setFont(new Font("Arial", 30));
        topPane.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
        
        //adding nodes together
        mainPane.setCenter(centerPane);
        topPane.setCenter(label);
        mainPane.setTop(topPane);
        
        //arranging the stage and finally make it visible
        primaryStage.setScene(scene);
        primaryStage.setTitle("Gameboy Emulator");
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    /**
     * The main method
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

}