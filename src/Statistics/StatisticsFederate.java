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
package Statistics;

import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("Duplicates")
public class StatisticsFederate
{
    /** The sync point all federates will sync up on before starting */
    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private StatisticsFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected RtiObjectClassHandleWrapper statisticsHandleWrapper;
    protected RtiObjectClassHandleWrapper checkoutHandleWrapper;
    protected RtiObjectClassHandleWrapper queueHandleWrapper;
    protected RtiObjectClassHandleWrapper customerHandleWrapper;

    protected RtiInteractionClassHandleWrapper enterQueueHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterCheckoutHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterShopHandleWrapper;
    protected RtiInteractionClassHandleWrapper servicingCustomerHandleWrapper;
    protected RtiInteractionClassHandleWrapper payHandleWrapper;
    protected RtiInteractionClassHandleWrapper exitShopHandleWrapper;
    protected RtiInteractionClassHandleWrapper createCheckoutHandleWrapper;

    public ParameterHandle customerIdParameterHandleEnterCheckout;
    public ParameterHandle customerIdParameterHandleEnterQueue;
    public ParameterHandle customerIdParameterHandlePay;
    protected ParameterHandle numberOfProductsInBasketParameterHandleEnterQueue;
    protected ParameterHandle isPrivilegedParameterHandleCreateCheckout;
    protected ParameterHandle isPrivilegedParameterHandleEnterCheckout;

    protected RtiInteractionClassHandleWrapper stopSimulationHandleWrapper;

    protected Statistics statistics = new Statistics();


    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// Main Simulation Method /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * This is the main simulation loop. It can be thought of as the main method of
     * the federate. For a description of the basic flow of this federate, see the
     * class level comments
     */
    public void runFederate( String federateName ) throws Exception
    {
        if (
                createRTIAndFederation(
                        new StatisticsFederateAmbassador(this),
                        federateName,
                        "statistics",
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

        while( fedamb.isRunning )
        {

            advanceTime( 1.0 );
        }

        System.out.println("AvgBeingInShopDuration: " + statistics.getAvgBeingInShopDuration());
        System.out.println("AvgBeingInQueueDuration: " + statistics.getAvgBeingInQueueDuration());
        System.out.println("AvgBeingInCheckoutDuration: " + statistics.getAvgBeingInCheckoutDuration());
        System.out.println("AvgBeingInOrdinaryCheckoutDuration: " + statistics.getAvgBeingInOrdinaryCheckoutDuration());
        System.out.println("AvgBeingInPrivilegedCheckoutDuration: " + statistics.getAvgBeingInPrivilegedCheckoutDuration());
        System.out.println("avgNumberOfProductsInBasket: " + statistics.getAvgNumberOfProductsInBasket());
        System.out.println("percentOfPrivilegedCheckouts: " + statistics.getPercentageOfPrivilegedCheckouts());

        resignFederation();
        destroyFederation();
    }

    private void publishAndSubscribe() throws RTIexception {
        this.queueHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Queue");
        this.queueHandleWrapper.addAttributes("id", "maxLimit", "checkoutId");
        this.queueHandleWrapper.subscribe();

        this.customerHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Customer");
        this.customerHandleWrapper.addAttributes("id", "numberOfProductsInBasket", "valueOfProducts");
        this.customerHandleWrapper.subscribe();

        this.checkoutHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Checkout");
        this.checkoutHandleWrapper.addAttributes("id", "isPrivileged", "isFree");
        this.checkoutHandleWrapper.subscribe();

        enterShopHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.EnterShop");
        enterShopHandleWrapper.subscribe();

        enterQueueHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.EnterQueue");
        enterQueueHandleWrapper.subscribe();

        customerIdParameterHandleEnterQueue = rtiamb.getParameterHandle(enterQueueHandleWrapper.getHandle(), "customerId");

        numberOfProductsInBasketParameterHandleEnterQueue = rtiamb.getParameterHandle(enterQueueHandleWrapper.getHandle(), "numberOfProductsInBasket");

        enterCheckoutHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.EnterCheckout");
        enterCheckoutHandleWrapper.subscribe();

        isPrivilegedParameterHandleEnterCheckout = rtiamb.getParameterHandle(enterCheckoutHandleWrapper.getHandle(), "isPrivileged");

        createCheckoutHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.CreateCheckout");
        createCheckoutHandleWrapper.subscribe();

        isPrivilegedParameterHandleCreateCheckout = rtiamb.getParameterHandle(createCheckoutHandleWrapper.getHandle(), "isPrivileged");

        customerIdParameterHandleEnterCheckout = rtiamb.getParameterHandle(enterCheckoutHandleWrapper.getHandle(), "customerId");

        servicingCustomerHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.ServicingCustomer");
        servicingCustomerHandleWrapper.subscribe();

        payHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.Pay");
        payHandleWrapper.subscribe();

        customerIdParameterHandlePay = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "customerId");

        exitShopHandleWrapper = new RtiInteractionClassHandleWrapper(rtiamb, "HLAinteractionRoot.ExitShop");
        exitShopHandleWrapper.subscribe();

        this.stopSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StopSimulation");
        this.stopSimulationHandleWrapper.subscribe();
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private boolean createRTIAndFederation(StatisticsFederateAmbassador federateAmbassador, String federateName, String federateType, String nameOfFederation) throws Exception {
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
    private void log( String message )
    {
        System.out.println( "StatisticsFederate   : " + message );
    }

    /**
     * This method will block until the user presses enter
     */
    private void waitForUser() {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try {
            reader.readLine();
        }
        catch( Exception e ) {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private short getTimeAsShort()
    {
        return (short)fedamb.federateTime;
    }

    private byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }

    private void advanceTime( double timestep ) throws RTIexception {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( time );

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while( fedamb.isAdvancing )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
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
    private void enableTimePolicy() throws Exception
    {
        // NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code is
        //       Portico specific. You will have to alter this if you move to a
        //       different RTI implementation. As such, we've isolated it into a
        //       method so that any change only needs to happen in a couple of spots
        HLAfloat64Interval lookahead = timeFactory.makeInterval( fedamb.federateLookahead );

        ////////////////////////////
        // enable time regulation //
        ////////////////////////////
        this.rtiamb.enableTimeRegulation( lookahead );

        // tick until we get the callback
        while( fedamb.isRegulating == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while( fedamb.isConstrained == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        log("Time Policy Enabled");
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        // get a federate name, use "exampleFederate" as default
        String federateName = "Statistics";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            new StatisticsFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}

