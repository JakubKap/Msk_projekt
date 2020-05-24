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

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import rtiHelperClasses.RtiHelper;
import rtiHelperClasses.RtiObjectClassHandle;
import utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class ProductFederate
{
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------
    /** The sync point all federates will sync up on before starting */
    public static final String READY_TO_RUN = "ReadyToRun";

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private RtiHelper rtiHelper = new RtiHelper(rtiamb);
    private ProductFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected ObjectClassHandle customerHandle;
    protected AttributeHandle customerIdHandle;
    protected AttributeHandle numberOfProductsInBasketHandle;
    protected AttributeHandle valueOfProductsHandle;
    protected InteractionClassHandle endShoppingHandle;
    protected InteractionClassHandle enterShopHandle;

    private static Random random = new Random();

    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// Main Simulation Method /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    public void runFederate( String federateName ) throws Exception
    {
        if (createRTIAndFederation(federateName)) return;
        this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        evokeMultipleCallbacksIfNotAnnounced();
        waitForUser();
        synchronizationPointAchieved();
        evokeMultipleCallbacksIfNotReadyToRun();
        enableTimePolicy();
        publishAndSubscribe();
        ObjectInstanceHandle objectHandle = registerObject();

        while( fedamb.isRunning) {
//            updateAttributeValues( objectHandle );
            endShopping(5);
//            advanceTime( 1.0 );
            advanceTime( random.nextInt(9) + 1 );
        }

        deleteObject(objectHandle);
        resignFederation();
        destroyFederation();
    }

    private void publishAndSubscribe() throws RTIexception
    {
        rtiHelper.addInteraction("HLAinteractionRoot.EndShopping").publish();
        rtiHelper.addInteraction("HLAinteractionRoot.EnterShop").subscribe();

        RtiObjectClassHandle customerHandle = rtiHelper.addObject("HLAobjectRoot.Customer");
        customerHandle.addAttributes("id", "numberOfProductsInBasket", "valueOfProducts");
        customerHandle.publish();
//        // get all the handle information for the attributes of customer
//        this.customerHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Customer");
//        this.customerIdHandle = rtiamb.getAttributeHandle( customerHandle, "id" );
//        this.numberOfProductsInBasketHandle = rtiamb.getAttributeHandle( customerHandle, "numberOfProductsInBasket" );
//        this.valueOfProductsHandle = rtiamb.getAttributeHandle( customerHandle, "valueOfProducts" );
//
//        // package the information into a handle set
//        AttributeHandleSet customerAttributes = rtiamb.getAttributeHandleSetFactory().create();
//        customerAttributes.add( customerIdHandle );
//        customerAttributes.add( numberOfProductsInBasketHandle );
//        customerAttributes.add( valueOfProductsHandle );
//
//        // do the customerHandle publication
//        rtiamb.publishObjectClassAttributes( customerHandle, customerAttributes );
//
//        //////////////////////////////////////////////////////////
//
//        // publish the interaction class EndShopping //
//        this.endShoppingHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.EndShopping" );
//        rtiamb.publishInteractionClass(endShoppingHandle);
//
//        // publish the interaction class EnterShop //
//        this.enterShopHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.EnterShop" );
//        rtiamb.subscribeInteractionClass(enterShopHandle);
        log( "Published and Subscribed" );
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectInstanceHandle objectInstanceHandle = rtiamb.registerObjectInstance(customerHandle);
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

    private void deleteObject( ObjectInstanceHandle handle ) throws RTIexception{
        rtiamb.deleteObjectInstance( handle, generateTag() );
        log( "Deleted Object, handle=" + handle );
    }

    protected void endShopping(int customerId) throws RTIexception {
        InteractionClassHandle interactionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.EndShopping");

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(interactionHandle, "customerId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory ,customerId));

        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( interactionHandle, parameters, generateTag(), time );
        log("koniec kupowania: "+ customerId + " time: "+ fedamb.federateTime);
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private boolean createRTIAndFederation(String federateName) throws Exception {
        /////////////////////////////////////////////////
        // 1 & 2. create the RTIambassador and Connect //
        /////////////////////////////////////////////////
        log( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        // connect
        log( "Connecting..." );
        fedamb = new ProductFederateAmbassador( this );
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
                "product",   // federate type
                "nameOfFederation",     // name of federation
                joinModules );           // modules we want to add

        log( "Joined Federation as " + federateName );
        return false;
    }

    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println( "ProductFederate   : " + message );
    }

    /**
     * This method will block until the user presses enter
     */
    private void waitForUser()
    {
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

        log( "Time Advanced to " + fedamb.federateTime );
    }

    private void evokeMultipleCallbacksIfNotReadyToRun() throws CallNotAllowedFromWithinCallback, RTIinternalError {
        while(!fedamb.isReadyToRun) {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    private void evokeMultipleCallbacksIfNotAnnounced() throws CallNotAllowedFromWithinCallback, RTIinternalError {
        while(!fedamb.isAnnounced) {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    private void synchronizationPointAchieved() throws SynchronizationPointLabelNotAnnounced, SaveInProgress, RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError {
        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
    }

    private void destroyFederation() throws NotConnected, RTIinternalError {
        try
        {
            rtiamb.destroyFederationExecution( "Federation" );
            log( "Destroyed Federation" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }
    }

    private void resignFederation() throws Exception {
        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        log( "Resigned from Federation" );
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
        while(!fedamb.isRegulating)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while(!fedamb.isConstrained)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        log( "Time Policy Enabled" );
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        // get a federate name, use "exampleFederate" as default
        String federateName = "Product";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            new ProductFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
