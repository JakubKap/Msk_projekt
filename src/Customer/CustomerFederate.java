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
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import rtiHelperClasses.RtiInteractionClassHandleWrapper;
import rtiHelperClasses.RtiObjectClassHandleWrapper;
import utils.Utils;
import utils.Event;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

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

    protected ObjectInstanceHandle simulationParametersObjectInstanceHandle;

    protected RtiObjectClassHandleWrapper simulationParametersWrapper;
    protected RtiObjectClassHandleWrapper customerHandleWrapper;
    protected RtiObjectClassHandleWrapper queueHandleWrapper;
    protected RtiObjectClassHandleWrapper checkoutHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterShopHandleWrapper;
    protected RtiInteractionClassHandleWrapper enterQueueHandleWrapper;
    protected RtiInteractionClassHandleWrapper startSimulationHandleWrapper;
    protected RtiInteractionClassHandleWrapper stopSimulationHandleWrapper;
    protected RtiInteractionClassHandleWrapper endShoppingHandleWrapper;
    protected RtiInteractionClassHandleWrapper servicingCustomerHandleWrapper;
    protected RtiInteractionClassHandleWrapper exitShopHandleWrapper;

    public PriorityQueue<Event> doingShoppingCustomers = new PriorityQueue<>();
    public PriorityQueue<Event> servicingCustomers = new PriorityQueue<>();
    private static Random random = new Random();
    protected RtiInteractionClassHandleWrapper payHandleWrapper;
    private List<Customer> customers = new ArrayList<>();
    protected ParameterHandle customerIdParameterHandleEndShopping;
    protected ParameterHandle numberOfProductsInBasketParameterHandleEndShopping;
    protected ParameterHandle valueOfProductsParameterHandleEndShopping;
    protected ParameterHandle customerIdParameterHandleServicingCustomer;
    protected ParameterHandle checkoutIdParameterHandleServicingCustomer;
    protected ParameterHandle customerIdParameterHandlePay;
    protected ParameterHandle checkoutIdParameterHandlePay;
    protected ParameterHandle priceParameterHandlePay;
    private boolean simulationStarted;


    public void setSimulationStarted(boolean simulationStarted) {
        this.simulationStarted = simulationStarted;
    }

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

        while( fedamb.isRunning)
        {
            if (simulationStarted) {
                if(random.nextInt(3) == 0){
                    Customer customer = createCustomer();

                    //            updateAttributeValues( objectHandle );

                    enterShop(customer.getId());
//
                }

                updateCustomersWithBoughtProducts();

                Event event = null;
                if (!servicingCustomers.isEmpty()) {
                    event = servicingCustomers.peek();
                }

                if (event != null && event.getInteractionClassHandle().equals(this.servicingCustomerHandleWrapper.getHandle())) {
                    int customerId = 0;
                    int checkoutId = 0;
                    int valueOfProducts = 0;
                    ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
                    for (ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                        if (parameter.equals(this.customerIdParameterHandleServicingCustomer)) {
                            byte[] bytes = parameterHandleValueMap.get(parameter);
                            customerId = Utils.byteToInt(bytes);
                        } else {
                            byte[] bytes = parameterHandleValueMap.get(parameter);
                            checkoutId = Utils.byteToInt(bytes);
                        }
                    }
                    Customer servicingCustomer = customers.get(customerId);
                    pay(customerId, checkoutId, servicingCustomer.getValueOfProducts());
                    servicingCustomers.poll();
                }
            }
            advanceTime(1.0);
        }
        deleteObject();
        resignFederation();
        destroyFederation();
    }

    private void updateCustomersWithBoughtProducts() throws RTIexception {
        Event event = null;
        if (!doingShoppingCustomers.isEmpty()) {
            event = doingShoppingCustomers.peek();
        }

        if (event != null && event.getInteractionClassHandle().equals(this.endShoppingHandleWrapper.getHandle())) {
            int customerId = 0;
            int numberOfProductsInBasket = 0;
            int valueOfProducts = 0;
            ParameterHandleValueMap parameterHandleValueMap = event.getParameterHandleValueMap();
            for(ParameterHandle parameter : parameterHandleValueMap.keySet()) {
                if (parameter.equals(this.customerIdParameterHandleEndShopping)) {
                    byte[] bytes = parameterHandleValueMap.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                } else if (parameter.equals(this.numberOfProductsInBasketParameterHandleEndShopping)){
                    byte[] bytes = parameterHandleValueMap.get(parameter);
                    numberOfProductsInBasket = Utils.byteToInt(bytes);
                } else {
                    byte[] bytes = parameterHandleValueMap.get(parameter);
                    valueOfProducts = Utils.byteToInt(bytes);
                }
            }
            Customer customer = customers.get(customerId);
            updateAttributeValues(customer, numberOfProductsInBasket, valueOfProducts);
            enterQueue(customerId, customer.getNumberOfProductsInBasket());
            doingShoppingCustomers.poll();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        this.simulationParametersWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.SimulationParameters");
        simulationParametersWrapper.addAttributes("maxQueueSize", "percentageOfCustomersDoingSmallShopping", "initialNumberOfCheckouts");
        simulationParametersWrapper.subscribe();

        this.customerHandleWrapper = new RtiObjectClassHandleWrapper(rtiamb, "HLAobjectRoot.Customer");
        customerHandleWrapper.addAttributes("id", "numberOfProductsInBasket", "valueOfProducts");
        customerHandleWrapper.publish();

        this.enterShopHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterShop");
        this.enterShopHandleWrapper.publish();

        this.enterQueueHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EnterQueue");
        this.enterQueueHandleWrapper.publish();

        this.payHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.Pay");
        this.payHandleWrapper.publish();

        this.customerIdParameterHandlePay = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "customerId");
        this.checkoutIdParameterHandlePay = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "checkoutId");
        this.priceParameterHandlePay = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "price");

        this.startSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StartSimulation");
        this.startSimulationHandleWrapper.subscribe();

        this.stopSimulationHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.StopSimulation");
        this.stopSimulationHandleWrapper.subscribe();

        this.endShoppingHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.EndShopping");
        this.endShoppingHandleWrapper.subscribe();

        this.customerIdParameterHandleEndShopping = rtiamb.getParameterHandle(endShoppingHandleWrapper.getHandle(), "customerId");
        this.numberOfProductsInBasketParameterHandleEndShopping = rtiamb.getParameterHandle(endShoppingHandleWrapper.getHandle(), "numberOfProductsInBasket");
        this.valueOfProductsParameterHandleEndShopping = rtiamb.getParameterHandle(endShoppingHandleWrapper.getHandle(), "valueOfProducts");

        this.servicingCustomerHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.ServicingCustomer");
        this.servicingCustomerHandleWrapper.subscribe();

        this.customerIdParameterHandleServicingCustomer = rtiamb.getParameterHandle(servicingCustomerHandleWrapper.getHandle(), "customerId");
        this.checkoutIdParameterHandleServicingCustomer = rtiamb.getParameterHandle(servicingCustomerHandleWrapper.getHandle(), "checkoutId");

        this.exitShopHandleWrapper = new RtiInteractionClassHandleWrapper(this.rtiamb, "HLAinteractionRoot.ExitShop");
        this.exitShopHandleWrapper.subscribe();
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectInstanceHandle objectInstanceHandle = rtiamb.registerObjectInstance(customerHandleWrapper.getHandle());
//        log( "Registered Object, handle=" + objectInstanceHandle );
        return objectInstanceHandle;
    }

    private Customer createCustomer() throws RTIexception {
        Customer customer = new Customer();
        customers.add(customer);
        ObjectInstanceHandle customerInstanceHandler = registerObject();
        customer.setHandler(customerInstanceHandler);
        return customer;
    }

    private void updateAttributeValues( Customer customer, int numberOfProductsInBasket, int valueOfProducts) throws RTIexception {
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(2);

//        int randomValue = 101 + new Random().nextInt(3);
        byte[] numberOfProductsInBasketArray = Utils.intToByte(encoderFactory, numberOfProductsInBasket);
        byte[] valueOfProductsArray = Utils.intToByte(encoderFactory, valueOfProducts);

        attributes.put(customerHandleWrapper.getAttribute("numberOfProductsInBasket"), numberOfProductsInBasketArray);
        attributes.put(customerHandleWrapper.getAttribute("valueOfProducts"), valueOfProductsArray);

        customer.setValueOfProducts(valueOfProducts);
        customer.setNumberOfProductsInBasket(numberOfProductsInBasket);

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(customer.getHandler(), attributes, generateTag(), time);

        log("Customer " + customer.getId() +  " modified: " + "numberOfProductsInBasket: " + numberOfProductsInBasket
                + ", valueOfProducts: " + valueOfProducts );
    }

    private void deleteObject() throws RTIexception {
        for (Customer customer : customers) {
            rtiamb.deleteObjectInstance(customer.getHandler(), generateTag());
            log( "Deleted Object, handle=" + customer.getHandler() );
        }
    }

    private void enterShop(int customerId) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(enterShopHandleWrapper.getHandle(), "customerId");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory, customerId));
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( enterShopHandleWrapper.getHandle(), parameters, generateTag(), time );

        log("(EnterShop) sent, customerId: "+ customerId + " time: "+ fedamb.federateTime);
    }

    private void enterQueue(int customerId, int numberOfProductsInBasket) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(enterQueueHandleWrapper.getHandle(), "customerId");
        ParameterHandle numberOfProductsInBasketHandle = rtiamb.getParameterHandle(enterQueueHandleWrapper.getHandle(), "numberOfProductsInBasket");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory , customerId));
        parameters.put(numberOfProductsInBasketHandle, Utils.intToByte(encoderFactory , numberOfProductsInBasket));

        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( enterQueueHandleWrapper.getHandle(), parameters, generateTag(), time );
        log("(EnterQueue) sent, customerId: " + customerId + "numberOfProductsInBasket: " + numberOfProductsInBasket
                +  ", time: "+ fedamb.federateTime);
    }

    private void pay(int customerId, int checkoutId, int valueOfProducts) throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(3);
        ParameterHandle customerIdHandle = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "customerId");
        ParameterHandle checkoutIdHandle = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "checkoutId");
        ParameterHandle priceHandle = rtiamb.getParameterHandle(payHandleWrapper.getHandle(), "price");
        parameters.put(customerIdHandle, Utils.intToByte(encoderFactory , customerId));
        parameters.put(checkoutIdHandle, Utils.intToByte(encoderFactory , checkoutId));
        parameters.put(priceHandle, Utils.intToByte(encoderFactory , valueOfProducts));

        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        rtiamb.sendInteraction( payHandleWrapper.getHandle(), parameters, generateTag(), time );
        log("(Pay) sent, customerId: " + customerId + ", checkoutId: " + checkoutId + ", price: " + valueOfProducts +  " time: "+ fedamb.federateTime);
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
        System.out.println("CustomerFederate   : " + message );
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
