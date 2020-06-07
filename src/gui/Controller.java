package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

public class Controller {
    @FXML
    GridPane gridPane;

    NumberTextField[] textFields;

    @FXML
    void initialize () {
        this.textFields = new NumberTextField[3];
        for (int i = 0; i < textFields.length; i++) {
            textFields[i] = new NumberTextField();
            this.gridPane.add(textFields[i], 1, i, 1, 1);
        }
    }

    @FXML
    private void startSimulation(ActionEvent event) {
//        this.textFields[0].getText();
//        this.textFields[1].getText();
//        this.textFields[2].getText();
    }

    @FXML
    private void stopSimulation(ActionEvent event) {

    }
}
