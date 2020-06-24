/*
 *   Copyright 2012 The Portico Project
 *
 *   This file is part of portico.
 *
 *   portico is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL)
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package Manager;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("Duplicates")
public class ManagerFederate extends Application {
    /**
     * The sync point all federates will sync up on before starting
     */
    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private ManagerFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected RtiObjectClassHandleWrapper simulationParametersWrapper;
    protected RtiInteractionClassHandleWrapper startSimulationHandleWrapper;
    protected RtiInteractionClassHandleWrapper stopSimulationHandleWrapper;

    private boolean simulationStarted = false;
    private boolean simulationStopped = false;

    private SimulationParameters simulationParameters;

    public void setSimulationParameters(SimulationParameters simulationParameters) {
        this.simulationParameters = simulationParameters;
    }

    public void setSimulationStarted(boolean simulationStarted) {
        this.simulationStarted = simulationStarted;
    }

    public void setSimulationStopped(boolean simulationStopped) {
        this.simulationStopped = simulationStopped;
    }

    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// Main Simulation Method /////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This is the main simulation loop. It can be thought of as the main method of
     * the federate. For a description of the basic flow of this federate, see the
     * class level comments
     */
    public void runFederate(String federateName) throws Exception {
        if (
                createRTIAndFederation(
                        new ManagerFederateAmbassador(this),
                        federateName,
                        "manager",
                        "Federation")
        ) return;
        this.timeFactory = (HLAfloat64TimeFactory) rtiamb.getTimeFactory();
        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        evokeMultipleCallbacksIfNotAnnounced();
        waitForUser();
        synchronizationPointAchieved();
        evokeMultipleCallbacksIfNotReadyToRun();
        enableTimePolicy();
        publishAndSubscribe();

        while (fedamb.isRunning) {
            if (simulationStarted) {
                startSimulation();
                ObjectInstanceHandle simulationParametersInstanceHandler = registerObject();
                simulationParameters.setHandler(simulationParametersInstanceHandler);
                updateAttributeValues(simulationParameters);
                simulationStarted = false;
            }
            if(simulationStopped){
                    fedamb.isRunning = false;
                    stopSimulation();
            }

            advanceTime(1.0);
        }

        deleteObject(simulationParameters.getHandler());
        resignFederation();
        destroyFederation();
    }



    private void publishAndSubscribe() throws RTIexception {
        this.simulationParametersWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.SimulationParameters");
        simulationParametersWrapper.addAttributes("maxQueueSize", "percentageOfCustomersDoingSmallShopping", "initialNumberOfCheckouts");
        simulationParametersWrapper.publish();

        this.startSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StartSimulation");
        this.startSimulationHandleWrapper.publish();

        this.stopSimulationHandleWrapper =  new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StopSimulation");
        this.stopSimulationHandleWrapper.publish();
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectInstanceHandle objectInstanceHandle = rtiamb.registerObjectInstance(simulationParametersWrapper.getHandle());
        log("Registered Object SimulationParameters, handle=" + objectInstanceHandle);
        return objectInstanceHandle;
    }

    private void updateAttributeValues(SimulationParameters simulationParameters) throws RTIexception {
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);

        byte[] initialNumberOfCheckouts = Utils.intToByte(encoderFactory, simulationParameters.getInitialNumberOfCheckouts());
        byte[] maxQueueSize = Utils.intToByte(encoderFactory, simulationParameters.getMaxQueueSize());
        byte[] percentageOfCustomersDoingSmallShopping = Utils.intToByte(encoderFactory, simulationParameters.getPercentageOfCustomersDoingSmallShopping());

        attributes.put(simulationParametersWrapper.getAttribute("initialNumberOfCheckouts"), initialNumberOfCheckouts);
        attributes.put(simulationParametersWrapper.getAttribute("maxQueueSize"), maxQueueSize);
        attributes.put(simulationParametersWrapper.getAttribute("percentageOfCustomersDoingSmallShopping"), percentageOfCustomersDoingSmallShopping);
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(simulationParameters.getHandler(), attributes, generateTag(), time);
    }

    private void startSimulation() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( startSimulationHandleWrapper.getHandle(), parameters, generateTag(), time );

        log("(StartSimulation) sent" + fedamb.federateTime);
    }

    private void stopSimulation() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( stopSimulationHandleWrapper.getHandle(), parameters, generateTag(), time );

        log("(StopSimulation) sent" + fedamb.federateTime);
    }

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
        log("Deleted Object, handle=" + handle);
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private boolean createRTIAndFederation(ManagerFederateAmbassador federateAmbassador, String federateName, String federateType, String nameOfFederation) throws Exception {
        /////////////////////////////////////////////////
        // 1 & 2. create the RTIambassador and Connect //
        /////////////////////////////////////////////////
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        // connect
        log("Connecting...");

        fedamb = federateAmbassador;
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);

        //////////////////////////////
        // 3. create the federation //
        //////////////////////////////
        log("Creating Federation...");
        // We attempt to create a new federation with the first three of the
        // restaurant FOM modules covering processes, food and drink
        try {
            URL[] modules = new URL[]{
                    (new File("foms/ShopFom.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution("Federation", modules);
            log("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return true;
        }

        URL[] joinModules = new URL[]{
                (new File("foms/ShopFom.xml")).toURI().toURL()
        };

        rtiamb.joinFederationExecution(federateName,            // name for the federate
                federateType,   // federate type
                nameOfFederation,     // name of federation
                joinModules);           // modules we want to add

        log("Joined Federation as " + federateName);
        return false;
    }

    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log(String message) {
        System.out.println("ManagerFederate   : " + message);
    }

    /**
     * This method will block until the user presses enter
     */
    private void waitForUser() {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private short getTimeAsShort() {
        return (short) fedamb.federateTime;
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    private void advanceTime(double timestep) throws RTIexception {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        log("Time Advanced to " + fedamb.federateTime);
    }

    private void evokeMultipleCallbacksIfNotReadyToRun() throws CallNotAllowedFromWithinCallback, RTIinternalError {
        while (!fedamb.isReadyToRun) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void evokeMultipleCallbacksIfNotAnnounced() throws CallNotAllowedFromWithinCallback, RTIinternalError {
        while (!fedamb.isAnnounced) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void synchronizationPointAchieved() throws RTIexception {
        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
    }

    private void destroyFederation() throws NotConnected, RTIinternalError {
        try {
            rtiamb.destroyFederationExecution("Federation");
            log("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        }
    }

    private void resignFederation() throws Exception {
        rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        log("Resigned from Federation");
    }

    /**
     * This method will attempt to enable the various time related properties for
     * the federate
     */
    private void enableTimePolicy() throws Exception {
        // NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code is
        //       Portico specific. You will have to alter this if you move to a
        //       different RTI implementation. As such, we've isolated it into a
        //       method so that any change only needs to happen in a couple of spots
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);

        ////////////////////////////
        // enable time regulation //
        ////////////////////////////
        this.rtiamb.enableTimeRegulation(lookahead);

        // tick until we get the callback
        while (fedamb.isRegulating == false) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while (fedamb.isConstrained == false) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        log("Time Policy Enabled");
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args) {
        // get a federate name, use "exampleFederate" as default
        String federateName = "Manager";
        if (args.length != 0) {
            federateName = args[0];
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane pane = FXMLLoader.load(getClass().getResource("../gui/sample.fxml"));
        primaryStage.setTitle("Hello World");
        Scene scene = new Scene(pane, 500, 300);
        primaryStage.setScene(scene);
//        primaryStage.setMaximized(true);
        primaryStage.show();

        pane.setId("pane");
    }
}
