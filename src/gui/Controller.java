package gui;

import Manager.ManagerFederate;
import Manager.SimulationParameters;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

public class Controller {
    @FXML
    GridPane gridPane;

    NumberTextField[] textFields;

    ManagerFederate managerFederate;


    @FXML
    void initialize () {
        this.textFields = new NumberTextField[3];
        for (int i = 0; i < textFields.length; i++) {
            textFields[i] = new NumberTextField();
            this.gridPane.add(textFields[i], 1, i, 1, 1);
        }
        new Manager().start();
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

    @FXML
    private void startSimulation(ActionEvent event) {
        SimulationParameters simulationParameters = new SimulationParameters();
        simulationParameters.setMaxQueueSize(Integer.parseInt(this.textFields[0].getText()));
        simulationParameters.setPercentageOfCustomersDoingSmallShopping(Integer.parseInt(this.textFields[1].getText()));
        simulationParameters.setInitialNumberOfCheckouts(Integer.parseInt(this.textFields[2].getText()));
        managerFederate.setSimulationParameters(simulationParameters);
        managerFederate.setSimulationStarted(true);
    }

    @FXML
    private void stopSimulation(ActionEvent event) {

    }
}
