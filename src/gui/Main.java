package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Pane pane = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        Scene scene = new Scene(pane, 1280, 660);
        primaryStage.setScene(scene);
//        primaryStage.setMaximized(true);
        primaryStage.show();

        pane.setId("pane");
    }


    public static void main(String[] args) {
        launch(args);
    }
}
