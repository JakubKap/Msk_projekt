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
package Customer;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import utils.Utils;
import utils.Event;
import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.Random;

@SuppressWarnings("Duplicates")
public class CustomerFederate
{
    public static final String READY_TO_RUN = "ReadyToRun";
    private static final String FEDERATE_NAME_TO_LOGGING = "CustomerFederate";

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private CustomerFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected RtiObjectClassHandleWrapper customerHandleWrapper;
    protected RtiObjectClassHandleWrapper queueHandleWrapper;
    protected RtiObjectClassHandleWrapper checkoutHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterShopHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterQueueHandleWrapper;
    protected RtiInteractionClassHandleWrapper startSimulationHandleWrapper;
    protected RtiInteractionClassHandleWrapper endShoppingHandleWrapper;
    protected RtiInteractionClassHandleWrapper servicingCustomerHandleWrapper;
    protected RtiInteractionClassHandleWrapper exitShopHandleWrapper;

    public LinkedList<Event> eventList = new LinkedList<>();
    private static Random random = new Random();
    protected RtiInteractionClassHandleWrapper payHandleWrapper;

    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------

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
        if (createRTIAndFederation(new CustomerFederateAmbassador( this ), federateName, "customer", "Federation")) return;
        this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        evokeMultipleCallbacksIfNotAnnounced();
        waitForUser();
        synchronizationPointAchieved();
        evokeMultipleCallbacksIfNotReadyToRun();
        enableTimePolicy();
        publishAndSubscribe();
        ObjectInstanceHandle objectHandle = registerObject();

        while( fedamb.isRunning)
        {
            // 9.1 update the attribute values of the instance //
            updateAttributeValues( objectHandle );

            // 9.2 send an interaction
            enterShop();

            Event event = null;
            if (!eventList.isEmpty()) {
                event = eventList.getFirst();
            }

            if (event != null && event.getInteractionClassHandle().equals(this.endShoppingHandleWrapper.getHandle())) {
                int customerId = 0;
                ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
                for(ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                    byte[] bytes = parameterHandleValueMap.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                    enterQueue(customerId);
                }
                eventList.removeFirst();
            }

            // 9.3 request a time advance and wait until we get it
            advanceTime( random.nextInt(9) + 1 );
            log( "Time Advanced to " + fedamb.federateTime );
        }

        deleteObject(objectHandle);
        resignFederation();
        destroyFederation();
    }

    private void publishAndSubscribe() throws RTIexception {
        this.customerHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Customer");
        customerHandleWrapper.addAttributes("id", "numberOfProductsInBasket", "valueOfProducts");
        customerHandleWrapper.publish();

        this.queueHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Queue");
        this.queueHandleWrapper.addAttributes("id", "maxLimit", "customerListIds", "checkoutId");
        this.queueHandleWrapper.subscribe();

        this.checkoutHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Checkout");
        checkoutHandleWrapper.addAttributes("id", "isPrivileged", "isFree");
        checkoutHandleWrapper.subscribe();

        this.enterShopHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterShop");
        this.enterShopHandleWrapper.publish();

        this.enterQueueHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterQueue");
        this.enterQueueHandleWrapper.publish();

        this.payHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.Pay");
        this.payHandleWrapper.publish();

        this.startSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StartSimulation");
        this.startSimulationHandleWrapper.subscribe();

        this.endShoppingHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EndShopping");
        this.endShoppingHandleWrapper.subscribe();

        this.servicingCustomerHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.ServicingCustomer");
        this.servicingCustomerHandleWrapper.subscribe();

        this.exitShopHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.ExitShop");
        this.exitShopHandleWrapper.subscribe();
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectInstanceHandle objectInstanceHandle = rtiamb.registerObjectInstance(customerHandleWrapper.getHandle());
        log( "Registered Object, handle=" + objectInstanceHandle );
        return objectInstanceHandle;
    }

    private void updateAttributeValues( ObjectInstanceHandle objectHandle ) throws RTIexception {
        ///////////////////////////////////////////////
        // create the necessary container and values //
        ///////////////////////////////////////////////
        // create a new map with an initial capacity - this will grow as required
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(2);

        // create the collection to store the values in, as you can see
        // this is quite a lot of work. You don't have to use the encoding
        // helpers if you don't want. The RTI just wants an arbitrary byte[]

        // generate the value for the number of cups (same as the timestep)
        HLAinteger16BE cupsValue = encoderFactory.createHLAinteger16BE( getTimeAsShort() );
//        attributes.put( cupsHandle, cupsValue.toByteArray() );

        // generate the value for the flavour on our magically flavour changing drink
        // the values for the enum are defined in the FOM
        int randomValue = 101 + new Random().nextInt(3);
        HLAinteger32BE flavValue = encoderFactory.createHLAinteger32BE( randomValue );
//        attributes.put( flavHandle, flavValue.toByteArray() );

        //////////////////////////
        // do the actual update //
        //////////////////////////
        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag() );

        // note that if you want to associate a particular timestamp with the
        // update. here we send another update, this time with a timestamp:
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag(), time );
    }

    private void deleteObject( ObjectInstanceHandle handle ) throws RTIexception {
        rtiamb.deleteObjectInstance( handle, generateTag() );
        log( "Deleted Object, handle=" + handle );
    }

    private void enterShop() throws RTIexception {
        Customer customer = new Customer();
        int customerId = customer.getId();
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(enterShopHandleWrapper.getHandle(), "customerId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory, customerId));
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( enterShopHandleWrapper.getHandle(), parameters, generateTag(), time );

        log("Dodano nowego kilenta, id: "+ customerId + " time: "+ fedamb.federateTime);
    }

    private void enterQueue(int customerId) throws RTIexception {
        InteractionClassHandle interactionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.EnterQueue");

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(interactionHandle, "customerId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory , customerId));

        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( interactionHandle, parameters, generateTag(), time );
        log("wejście do kolejki: "+ customerId + " time: "+ fedamb.federateTime);
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private boolean createRTIAndFederation(CustomerFederateAmbassador federateAmbassador, String federateName, String federateType, String nameOfFederation) throws Exception {
        /////////////////////////////////////////////////
        // 1 & 2. create the RTIambassador and Connect //
        /////////////////////////////////////////////////
        log( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        // connect
        log( "Connecting..." );
        fedamb = federateAmbassador;
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

        //////////////////////////////
        // 3. create the federation //
        //////////////////////////////
        log( "Creating Federation..." );
        // We attempt to create a new federation with the first three of the
        // restaurant FOM modules covering processes, food and drink
        try
        {
            URL[]modules = new URL[]{
                    (new File("foms/ShopFom.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution( "Federation", modules );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
            urle.printStackTrace();
            return true;
        }

        URL[] joinModules = new URL[]{
                (new File("foms/ShopFom.xml")).toURI().toURL()
        };

        rtiamb.joinFederationExecution( federateName,            // name for the federate
                federateType,   // federate type
                nameOfFederation,     // name of federation
                joinModules );           // modules we want to add

        log( "Joined Federation as " + federateName );
        return false;
    }

    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println(FEDERATE_NAME_TO_LOGGING + message );
    }

    /**
     * This method will block until the user presses enter
     */
    private void waitForUser() {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
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

    /**
     * This method will request a time advance to the current time, plus the given
     * timestep. It will then wait until a notification of the time advance grant
     * has been received.
     */
    private void advanceTime( double timestep ) throws RTIexception {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( time );

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while( fedamb.isAdvancing )
        {
            System.out.println("DKSLLKDFSJSDK");
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
    private void enableTimePolicy() throws Exception {
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
        String federateName = "Customer";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            new CustomerFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
