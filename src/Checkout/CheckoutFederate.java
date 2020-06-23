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
package Checkout;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import utils.Event;
import utils.TimeEvent;
import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CheckoutFederate {
    /**
     * The sync point all federates will sync up on before starting
     */
    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private CheckoutFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected ObjectClassHandle checkoutHandle;
    protected AttributeHandle checkoutIdHandle;
    protected AttributeHandle isPrivilegedHandle;
    protected AttributeHandle isFreeHandle;
    protected ObjectInstanceHandle simulationParametersObjectInstanceHandle;

    protected ParameterHandle customerIdParameterHandleEnterCheckout;
    protected ParameterHandle checkoutIdParameterHandleEnterCheckout;

    private InteractionClassHandle servicingCustomerHandle;
    protected InteractionClassHandle enterCheckoutHandle;
    private InteractionClassHandle createCheckoutHandle;
    protected InteractionClassHandle payHandle;
    protected InteractionClassHandle exitShopHandle;
    protected RtiInteractionClassHandleWrapper createCheckoutHandleWrapper;
    protected RtiInteractionClassHandleWrapper stopSimulationHandleWrapper;

    protected ParameterHandle customerIdParameterHandlePay;
    protected ParameterHandle checkoutIdParameterHandlePay;
    protected ParameterHandle priceParameterHandlePay;


    public int numberOfCheckouts;
    public List<Checkout> checkouts;
    public Collection<TimeEvent> servicingCustomers = new CopyOnWriteArrayList<>();
    public PriorityQueue<Event> customersToExit = new PriorityQueue<>();
    public PriorityQueue<Event> createCheckoutEvents = new PriorityQueue<>();
    protected RtiObjectClassHandleWrapper simulationParametersWrapper;
    private Random random = new Random();

    private Checkout getCheckoutById(int id) {
        Optional<Checkout> optionalCheckout = checkouts.stream()
                .filter(checkout -> checkout.getId() == id)
                .findFirst();
        return optionalCheckout.orElse(null);
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
                        new CheckoutFederateAmbassador(this),
                        federateName,
                        "checkout",
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
            boolean simulationStarted = numberOfCheckouts != 0;
            if (simulationStarted) {
                if (checkouts == null) {
                    checkouts = new LinkedList<>();
                }

                for (Event createCheckoutEvent : createCheckoutEvents) {
                    handleCreateCheckoutEvent(createCheckoutEvent);
                }
                createCheckoutEvents.clear();

                for (TimeEvent event : servicingCustomers) {
                    if (event.getInteractionClassHandle().equals(this.enterCheckoutHandle)) {
                        int customerId = 0;
                        int checkoutId = 0;
                        ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
                        for (ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                            if (parameter.equals(this.customerIdParameterHandleEnterCheckout)) {
                                byte[] bytes = parameterHandleValueMap.get(parameter);
                                customerId = Utils.byteToInt(bytes);
                            } else if (parameter.equals(this.checkoutIdParameterHandleEnterCheckout)) {
                                byte[] bytes = parameterHandleValueMap.get(parameter);
                                checkoutId = Utils.byteToInt(bytes);
                            }
                        }
                        if (event.getCurrentTime() == 0) {
                            Checkout checkout = getCheckoutById(checkoutId);
                            checkout.setFree(false);
                            log("Kasa " + checkout.getId() + " jest zajmowana");
                            updateAttributeValues(checkout);
                        }

                        event.incrementTime();

                        if (event.isReadyToHandle()) {
                            sendInteractionServicingCustomer(customerId, checkoutId);
                            servicingCustomers.remove(event);
                        }
                    }
                }


            Event event = null;

            if (!customersToExit.isEmpty()) {
                event = customersToExit.peek();
            }

            if (event != null && event.getInteractionClassHandle().equals(this.payHandle)) {
                int customerId = 0;
                int checkoutId = 0;
                ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
                for (ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                    if (parameter.equals(this.customerIdParameterHandlePay)) {
                        byte[] bytes = parameterHandleValueMap.get(parameter);
                        customerId = Utils.byteToInt(bytes);
                    } else if (parameter.equals(this.checkoutIdParameterHandlePay)) {
                        byte[] bytes = parameterHandleValueMap.get(parameter);
                        checkoutId = Utils.byteToInt(bytes);
                    }
                }
                Checkout checkout = getCheckoutById(checkoutId);
                checkout.setFree(true);
                log("Kasa " + checkout.getId() + " jest zwalniana");
                updateAttributeValues(checkout);
                sendInteractionExitShop(customerId);

                customersToExit.poll();
            }

        }
        advanceTime(1.0);
    }


        deleteObject();

        resignFederation();

        destroyFederation();
    }

    private void handleCreateCheckoutEvent(Event event) throws RTIexception {
        int checkoutId = 0;
        boolean isPrivileged = false;
        ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
        for (ParameterHandle parameter : parameterHandleValueMap.keySet()) {
            if (parameter.equals(this.createCheckoutHandleWrapper.getParameter("checkoutId"))) {
                byte[] bytes = parameterHandleValueMap.get(parameter);
                checkoutId = Utils.byteToInt(bytes);
            } else if (parameter.equals(this.createCheckoutHandleWrapper.getParameter("isPrivileged"))) {
                byte[] bytes = parameterHandleValueMap.get(parameter);
                isPrivileged = Utils.byteToBoolean(bytes);
            }
        }
        Checkout checkout = createCheckout(checkoutId, isPrivileged);
        updateAttributeValues(checkout);
    }

    private Checkout createCheckout(int checkoutId, boolean isPrivileged) throws RTIexception {
        Checkout checkout = new Checkout(checkoutId, isPrivileged, true);
        checkouts.add(checkout);
        ObjectInstanceHandle checkoutInstanceHandler = registerObject();
        checkout.setHandler(checkoutInstanceHandler);
        return checkout;
    }

    private void publishAndSubscribe() throws RTIexception {
        this.checkoutHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Customer");
        this.checkoutIdHandle = rtiamb.getAttributeHandle(checkoutHandle, "id");
        this.isPrivilegedHandle = rtiamb.getAttributeHandle(checkoutHandle, "numberOfProductsInBasket");
        this.isFreeHandle = rtiamb.getAttributeHandle(checkoutHandle, "valueOfProducts");

        AttributeHandleSet customerAttributes = rtiamb.getAttributeHandleSetFactory().create();
        customerAttributes.add(checkoutIdHandle);
        customerAttributes.add(isPrivilegedHandle);
        customerAttributes.add(isFreeHandle);

        rtiamb.publishObjectClassAttributes(checkoutHandle, customerAttributes);

        this.checkoutHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Checkout");
        this.checkoutIdHandle = rtiamb.getAttributeHandle(checkoutHandle, "id");
        this.isPrivilegedHandle = rtiamb.getAttributeHandle(checkoutHandle, "isPrivileged");
        this.isFreeHandle = rtiamb.getAttributeHandle(checkoutHandle, "isFree");

        AttributeHandleSet checkoutAttributes = rtiamb.getAttributeHandleSetFactory().create();
        checkoutAttributes.add(checkoutIdHandle);
        checkoutAttributes.add(isPrivilegedHandle);
        checkoutAttributes.add(isFreeHandle);

        rtiamb.publishObjectClassAttributes(checkoutHandle, checkoutAttributes);

        servicingCustomerHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ServicingCustomer");
        rtiamb.publishInteractionClass(servicingCustomerHandle);

        enterCheckoutHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.EnterCheckout");
        rtiamb.subscribeInteractionClass(enterCheckoutHandle);

        this.customerIdParameterHandleEnterCheckout = rtiamb.getParameterHandle(enterCheckoutHandle, "customerId");
        this.checkoutIdParameterHandleEnterCheckout = rtiamb.getParameterHandle(enterCheckoutHandle, "checkoutId");

        createCheckoutHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CreateCheckout");
        rtiamb.subscribeInteractionClass(createCheckoutHandle);

        payHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.Pay");
        rtiamb.subscribeInteractionClass(payHandle);

        this.customerIdParameterHandlePay = rtiamb.getParameterHandle(payHandle, "customerId");
        this.checkoutIdParameterHandlePay = rtiamb.getParameterHandle(payHandle, "checkoutId");
        this.priceParameterHandlePay = rtiamb.getParameterHandle(payHandle, "price");

        this.exitShopHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ExitShop");
        rtiamb.publishInteractionClass(exitShopHandle);

        this.createCheckoutHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.CreateCheckout");
        this.createCheckoutHandleWrapper.subscribe();

        this.simulationParametersWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.SimulationParameters");
        simulationParametersWrapper.addAttributes("initialNumberOfCheckouts");
        simulationParametersWrapper.subscribe();

        this.stopSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StopSimulation");
        this.stopSimulationHandleWrapper.subscribe();

        log("Published and Subscribed");
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectInstanceHandle objectInstanceHandle = rtiamb.registerObjectInstance(checkoutHandle);
        log("Registered Object, handle=" + objectInstanceHandle);
        return objectInstanceHandle;
    }

    private void updateAttributeValues(Checkout checkout) throws RTIexception {
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);

        byte[] checkoutIdArray = Utils.intToByte(encoderFactory, checkout.getId());
        byte[] checkoutIsFreeArray = Utils.booleanToByte(encoderFactory, checkout.isFree());
        byte[] checkoutIsPriviligedArray = Utils.booleanToByte(encoderFactory, checkout.isPrivileged());

        attributes.put(rtiamb.getAttributeHandle(checkoutHandle, "id"), checkoutIdArray);
        attributes.put(rtiamb.getAttributeHandle(checkoutHandle, "isFree"), checkoutIsFreeArray);
        attributes.put(rtiamb.getAttributeHandle(checkoutHandle, "isPrivileged"), checkoutIsPriviligedArray);

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(checkout.getHandler(), attributes, generateTag(), time);
    }

    private void sendInteractionServicingCustomer(int customerId, int checkoutId) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(servicingCustomerHandle, "customerId");
        ParameterHandle checkoutIdHandle = rtiamb.getParameterHandle(servicingCustomerHandle, "checkoutId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory, customerId));
        parameters.put(checkoutIdHandle, Utils.intToByte(encoderFactory, checkoutId));
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);

        rtiamb.sendInteraction(servicingCustomerHandle, parameters, generateTag(), time);

        log("(ServicingCustomer) sent, customerId: " + customerId + ", checkoutId: " + checkoutId + " time: " + fedamb.federateTime);
    }

    private void sendInteractionExitShop(int customerId) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(exitShopHandle, "customerId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory, customerId));

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);

        rtiamb.sendInteraction(exitShopHandle, parameters, generateTag(), time);
        log("(ExitShop) sent, customerId: " + customerId + " time: " + fedamb.federateTime);
    }

    private void deleteObject() throws RTIexception {
        for(Checkout checkout : checkouts) {
            rtiamb.deleteObjectInstance(checkout.getHandler(), generateTag());
            log("Deleted Object, handle=" + checkout.getHandler());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private boolean createRTIAndFederation(CheckoutFederateAmbassador federateAmbassador, String federateName, String federateType, String nameOfFederation) throws Exception {
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
        System.out.println("CheckoutFederate   : " + message);
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
        String federateName = "Checkout";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            // run the example federate
            new CheckoutFederate().runFederate(federateName);
        } catch (Exception rtie) {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
