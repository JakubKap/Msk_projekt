package gui;

import Manager.ManagerFederate;
import Manager.SimulationParameters;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

public class Controller {
    @FXML
    GridPane gridPane;

    @FXML
    Button startSimulationBtn;

    @FXML
    Button stopSimulationBtn;

    NumberTextField[] textFields;

    ManagerFederate managerFederate;


    @FXML
    void initialize () {
        this.textFields = new NumberTextField[3];
        for (int i = 0; i < textFields.length; i++) {
            textFields[i] = new NumberTextField();
            this.gridPane.add(textFields[i], 1, i, 1, 1);
        }

        disableStartStopSimulationBtns(true);

        new Manager().start();

        for (NumberTextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                startSimulationBtn.setDisable(!areAllParametersSet(textField));
            });
        }
    }

    class Manager extends Thread {
        public void run() {
            try {
                // run the example federate
                managerFederate = new ManagerFederate();
                managerFederate.runFederate("Manager");
            } catch (Exception rtie) {
                // an exception occurred, just log the information and exit
                rtie.printStackTrace();
            }
        }
    }

    private void disableStartStopSimulationBtns(boolean value){
        startSimulationBtn.setDisable(value);
        stopSimulationBtn.setDisable(value);
    }

    private boolean areAllParametersSet(NumberTextField currentTextField){
        if(currentTextField.getText().isEmpty())
            return false;

        for (NumberTextField textField : textFields) {
            if(textField.getText().isEmpty())
                return false;
        }

        return true;
    }

    private void clearTextFields(){
        for (NumberTextField textField : textFields) {
            textField.clear();
        }
    }

    @FXML
    private void startSimulation(ActionEvent event) {
        SimulationParameters simulationParameters = new SimulationParameters();
        simulationParameters.setMaxQueueSize(Integer.parseInt(this.textFields[0].getText()));
        simulationParameters.setPercentageOfCustomersDoingSmallShopping(Integer.parseInt(this.textFields[1].getText()));
        simulationParameters.setInitialNumberOfCheckouts(Integer.parseInt(this.textFields[2].getText()));
        managerFederate.setSimulationParameters(simulationParameters);
        managerFederate.setSimulationStarted(true);
        stopSimulationBtn.setDisable(false);
        startSimulationBtn.setDisable(true);
    }

    @FXML
    private void stopSimulation(ActionEvent event) {
        disableStartStopSimulationBtns(true);
        clearTextFields();
    }
}
