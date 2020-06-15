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
package Queue;

import Checkout.Checkout;
import Customer.Customer;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAvariableArray;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import utils.Event;
import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("Duplicates")
public class QueueFederate
{
    public static final String READY_TO_RUN = "ReadyToRun";

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private QueueFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    protected ObjectInstanceHandle simulationParametersObjectInstanceHandle;
    protected RtiObjectClassHandleWrapper customerHandleWrapper;
    protected RtiObjectClassHandleWrapper queueHandleWrapper;
    protected RtiObjectClassHandleWrapper checkoutHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterQueueHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterCheckoutHandleWrapper;
    public int numberOfQueues;
    public int maxQueueSize;
    public List<Queue> queues;
    public LinkedList<Event> customersIds = new LinkedList<>();
    public LinkedList<Checkout> checkouts = new LinkedList<>();
    private Random random = new Random();
    protected RtiObjectClassHandleWrapper simulationParametersWrapper;
    protected ParameterHandle customerIdParameterHandleEnterQueue;
    protected ParameterHandle numberOfProductsParameterHandleEnterQueue;

    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println( "QueueFederate   : " + message );
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

        log( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        // connect
        log( "Connecting..." );
        fedamb = new QueueFederateAmbassador( this );
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

        log( "Creating Federation..." );

        try
        {
            URL []modules = new URL[]{
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
            return;
        }

        URL[] joinModules = new URL[]{
                (new File("foms/ShopFom.xml")).toURI().toURL()
        };

        rtiamb.joinFederationExecution( federateName,            // name for the federate
                "queue",   // federate type
                "Federation",     // name of federation
                joinModules );           // modules we want to add

        log( "Joined Federation as " + federateName );

        // cache the time factory for easy access
        this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while( fedamb.isAnnounced == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        enableTimePolicy();
        log( "Time Policy Enabled" );

        publishAndSubscribe();
        log( "Published and Subscribed" );

//        ObjectInstanceHandle objectHandle = registerObject();
//        log( "Registered Object, handle=" + objectHandle );

        while( fedamb.isRunning )
        {
//            updateAttributeValues( objectHandle );
        if(numberOfQueues != 0) {
            if (queues == null) {
                queues = new ArrayList<>();
                for (int i = 0; i < numberOfQueues; i++) {
                    Queue queue = new Queue(maxQueueSize);
                    queues.add(queue);
                    ObjectInstanceHandle customerInstanceHandler = registerObject();
                    queue.setHandler(customerInstanceHandler);
                }
            }

            Event event = null;
            if (!customersIds.isEmpty()) {
                event = customersIds.getFirst();
            }

            if (event != null && event.getInteractionClassHandle().equals(this.enterQueueHandleWrapper.getHandle())) {
                List<Integer> freeQueuesNumbers = new LinkedList<>();
                int customerId = 0;
                int numberOfProductsInBasket = 0;

                ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
                for (ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                    if (parameter.equals(this.customerIdParameterHandleEnterQueue)) {
                        byte[] bytes = parameterHandleValueMap.get(parameter);
                        customerId = Utils.byteToInt(bytes);
                    } else {
                        byte[] bytes = parameterHandleValueMap.get(parameter);
                        numberOfProductsInBasket = Utils.byteToInt(bytes);
                    }
                }

                queues.forEach(queue -> {
                    if(queue.getCustomerListIds().size() < maxQueueSize)
                        freeQueuesNumbers.add(queue.getId());
                });

                if(freeQueuesNumbers.size() == 0){

                }
                else {
                    int index = random.nextInt(freeQueuesNumbers.size());
                    int numberOfQueue = freeQueuesNumbers.get(index);
                    Queue queue = queues.get(numberOfQueue);
                    updateAttributeValues(queue, customerId);
                    customersIds.removeFirst();
                }

            }
//            enterCheckout(event, enteredQueue.getCheckoutId());
        }
            advanceTime( 1.0 );
            log( "Time Advanced to " + fedamb.federateTime );
        }

//        deleteObject( objectHandle );
//        log( "Deleted Object, handle=" + objectHandle );

        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        log( "Resigned from Federation" );

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

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
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
    }

    /**
     * This method will inform the RTI about the types of data that the federate will
     * be creating, and the types of data we are interested in hearing about as other
     * federates produce it.
     */
    private void publishAndSubscribe() throws RTIexception {
        this.queueHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Queue");
        this.queueHandleWrapper.addAttributes("id", "maxLimit", "customerListIds", "checkoutId");
        this.queueHandleWrapper.publish();

        this.customerHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Customer");
        this.customerHandleWrapper.addAttributes("id", "numberOfProductsInBasket", "valueOfProducts");
        this.customerHandleWrapper.subscribe();

        this.checkoutHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb,"HLAobjectRoot.Checkout" );
        this.checkoutHandleWrapper.addAttributes("id", "isPrivileged", "isFree");
        this.checkoutHandleWrapper.subscribe();

        this.enterQueueHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterQueue");
        enterQueueHandleWrapper.subscribe();

        this.customerIdParameterHandleEnterQueue = rtiamb.getParameterHandle(enterQueueHandleWrapper.getHandle(), "customerId");
        this.numberOfProductsParameterHandleEnterQueue = rtiamb.getParameterHandle(enterQueueHandleWrapper.getHandle(), "numberOfProductsInBasket");

        this.enterCheckoutHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterCheckout");
        this.enterCheckoutHandleWrapper.publish();

        this.simulationParametersWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.SimulationParameters");
        simulationParametersWrapper.addAttributes("maxQueueSize", "initialNumberOfCheckouts");
        simulationParametersWrapper.subscribe();

    }

    /**
     * This method will register an instance of the Soda class and will
     * return the federation-wide unique handle for that instance. Later in the
     * simulation, we will update the attribute values for this instance
     */
    private ObjectInstanceHandle registerObject() throws RTIexception
    {
        return rtiamb.registerObjectInstance(queueHandleWrapper.getHandle());
    }

    /**
     * This method will update all the values of the given object instance. It will
     * set the flavour of the soda to a random value from the options specified in
     * the FOM (Cola - 101, Orange - 102, RootBeer - 103, Cream - 104) and it will set
     * the number of cups to the same value as the current time.
     * <p/>
     * Note that we don't actually have to update all the attributes at once, we
     * could update them individually, in groups or not at all!
     */
    private void updateAttributeValues(Queue queue, int numberOfClient) throws RTIexception
    {
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);

        List<Integer> customerIds = queue.getCustomerListIds();
        customerIds.add(numberOfClient);
        log("Queue number: " + queue.getId() + " customerList: " + queue.getCustomerListIds());

        HLAvariableArray customerIdsList = new CustomersArray(customerIds);

        attributes.put(queueHandleWrapper.getAttribute("customerListIds"), customerIdsList.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(queue.getHandler(), attributes, generateTag(), time);

        log("Customer: " + numberOfClient + " was added to queue: " + queue.getId());
    }

    private void enterCheckout(int customerId, int checkoutId) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(enterCheckoutHandleWrapper.getHandle(), "customerId");
        ParameterHandle checkoutIdHandle = rtiamb.getParameterHandle(enterCheckoutHandleWrapper.getHandle(), "checkoutId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory, customerId));
        parameters.put(checkoutIdHandle, Utils.intToByte(encoderFactory, checkoutId));
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( enterCheckoutHandleWrapper.getHandle(), parameters, generateTag(), time );

        log("(EnterCheckout) sent, customerId: "+ customerId + ", checkoutId: " + checkoutId + " time: "+ fedamb.federateTime);
    }

    /**
     * This method will request a time advance to the current time, plus the given
     * timestep. It will then wait until a notification of the time advance grant
     * has been received.
     */
    private void advanceTime( double timestep ) throws RTIexception
    {
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
    }

    /**
     * This method will attempt to delete the object instance of the given
     * handle. We can only delete objects we created, or for which we own the
     * privilegeToDelete attribute.
     */
    private void deleteObject( ObjectInstanceHandle handle ) throws RTIexception
    {
        rtiamb.deleteObjectInstance( handle, generateTag() );
    }

    private short getTimeAsShort()
    {
        return (short)fedamb.federateTime;
    }

    private byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        // get a federate name, use "exampleFederate" as default
        String federateName = "Queue";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            new QueueFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}

