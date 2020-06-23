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
package Product;

import Customer.Customer;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import utils.Event;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@SuppressWarnings("Duplicates")
public class ProductFederate {
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------
    /**
     * The sync point all federates will sync up on before starting
     */
    public static final String READY_TO_RUN = "ReadyToRun";
    private static final String FEDERATE_NAME_TO_LOGGING = "ProductFederate";
    protected ObjectInstanceHandle simulationParametersObjectInstanceHandle;

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private ProductFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected RtiObjectClassHandleWrapper simulationParametersWrapper;
    protected RtiInteractionClassHandleWrapper endShoppingHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterShopHandleWrapper;
    protected RtiObjectClassHandleWrapper customerHandleWrapper;

    protected RtiInteractionClassHandleWrapper stopSimulationHandleWrapper;

    public PriorityQueue<Event> eventList = new PriorityQueue<>();

    public int customerCounter = 0;
    protected int percentageOfCustomersDoingSmallShopping;
    protected ParameterHandle percentageOfCustomersDoingSmallShoppingParameterHandle;

    private static Random random = new Random();

    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// Main Simulation Method /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    public void runFederate(String federateName) throws Exception {
        if (
                createRTIAndFederation(
                        new ProductFederateAmbassador(this),
                        federateName,
                        "product",
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
//        ObjectInstanceHandle objectHandle = registerObject();

        while (fedamb.isRunning) {

            Event event = null;
            if (!eventList.isEmpty()) {
                event = eventList.peek();
            }

            if (event != null && event.getInteractionClassHandle().equals(this.enterShopHandleWrapper.getHandle())) {
                int customerId = 0;
                ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
                for(ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                    byte[] bytes = parameterHandleValueMap.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                    endShopping(customerId);
                }
                eventList.poll();
            }

            advanceTime( 1.0 );
//            advanceTime( random.nextInt(9) + 1 );
        }

//        deleteObject(objectHandle);
        resignFederation();
        destroyFederation();
    }

    private void publishAndSubscribe() throws RTIexception {
        this.endShoppingHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EndShopping");
        this.endShoppingHandleWrapper.publish();

        this.enterShopHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterShop");
        this.enterShopHandleWrapper.subscribe();

        this.simulationParametersWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.SimulationParameters");
        simulationParametersWrapper.addAttributes("percentageOfCustomersDoingSmallShopping");
        simulationParametersWrapper.subscribe();

        this.stopSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StopSimulation");
        this.stopSimulationHandleWrapper.subscribe();

        log("Published and Subscribed");
    }

    private ObjectInstanceHandle registerObject() throws RTIexception, ClassNotFoundException {
        ObjectInstanceHandle objectInstanceHandle = rtiamb.registerObjectInstance(customerHandleWrapper.getHandle());
        log("Registered Object, handle=" + objectInstanceHandle);
        return objectInstanceHandle;
    }

    private void updateAttributeValues(ObjectInstanceHandle objectHandle) throws RTIexception {
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(2);

//        int randomValue = 101 + new Random().nextInt(3);
        byte[] numberOfProductsInBasket = Utils.intToByte(encoderFactory, 3);
        byte[] valueOfProducts = Utils.intToByte(encoderFactory, 5);

        attributes.put(customerHandleWrapper.getAttribute("numberOfProductsInBasket"), numberOfProductsInBasket);
        attributes.put(customerHandleWrapper.getAttribute("valueOfProducts"), valueOfProducts);



        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(objectHandle, attributes, generateTag(), time);

        log("Zmodyfikowano obiekt klienta " + objectHandle + " " + attributes);
    }

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
        log("Deleted Object, handle=" + handle);
    }

    protected void endShopping(int customerId) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(3);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(endShoppingHandleWrapper.getHandle(), "customerId");
        ParameterHandle numberOfProductsInBasketHandle = rtiamb.getParameterHandle(endShoppingHandleWrapper.getHandle(), "numberOfProductsInBasket");
        ParameterHandle valueOfProductsHandle = rtiamb.getParameterHandle(endShoppingHandleWrapper.getHandle(), "valueOfProducts");


        int numberOfProductsInBasket;
        if((random.nextDouble() *100) <= percentageOfCustomersDoingSmallShopping)
            numberOfProductsInBasket = random.nextInt(5) + 1;
        else
            numberOfProductsInBasket = random.nextInt(5) + 6;

        int valueOfProducts = (random.nextInt(5) + 10) * numberOfProductsInBasket;

        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory, customerId));
        parameters.put(numberOfProductsInBasketHandle, Utils.intToByte(encoderFactory, numberOfProductsInBasket));
        parameters.put(valueOfProductsHandle, Utils.intToByte(encoderFactory, valueOfProducts));

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);

        rtiamb.sendInteraction(endShoppingHandleWrapper.getHandle(), parameters, generateTag(), time);
        log("(EndShopping) sent, customerId: " + customerId +  ", numberOfProductsInBasket: " + numberOfProductsInBasket
                + ", valueOfProductsHandle: " + valueOfProducts +  ", time: " + fedamb.federateTime);
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private boolean createRTIAndFederation(ProductFederateAmbassador federateAmbassador, String federateName, String federateType, String nameOfFederation) throws Exception {
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
        System.out.println("ProductFederate   : " + message);
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

    /**
     * This method will request a time advance to the current time, plus the given
     * timestep. It will then wait until a notification of the time advance grant
     * has been received.
     */
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
        while (!fedamb.isRegulating) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while (!fedamb.isConstrained) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        log("Time Policy Enabled");
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args) {
        // get a federate name, use "exampleFederate" as default
        String federateName = "Product";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            // run the example federate
            new ProductFederate().runFederate(federateName);
        } catch (Exception rtie) {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
