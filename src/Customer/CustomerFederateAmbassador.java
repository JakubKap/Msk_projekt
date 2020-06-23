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
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.Event;
import utils.Utils;

/**
 * This class handles all incoming callbacks from the RTI regarding a particular
 * {@link Customer}. It will log information about any callbacks it
 * receives, thus demonstrating how to deal with the provided callback information.
 */
class CustomerFederateAmbassador extends NullFederateAmbassador
{
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private CustomerFederate federate;

    // these variables are accessible in the package
    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;
    protected boolean isRunning       = true;

    //----------------------------------------------------------
    //                      CONSTRUCTORS
    //----------------------------------------------------------

    public CustomerFederateAmbassador( CustomerFederate federate)
    {
        this.federate = federate;
    }

    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------
    private void log( String message )
    {
        System.out.println( "FederateAmbassador: " + message );
    }

    //////////////////////////////////////////////////////////////////////////
    ////////////////////////// RTI Callback Methods //////////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void synchronizationPointRegistrationFailed( String label,
                                                        SynchronizationPointFailureReason reason )
    {
        log( "Failed to register sync point: " + label + ", reason="+reason );
    }

    @Override
    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label );
    }

    @Override
    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(CustomerFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized( String label, FederateHandleSet failed )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(CustomerFederate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    @Override
    public void timeRegulationEnabled( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance( ObjectInstanceHandle theObject,
                                        ObjectClassHandle theObjectClass,
                                        String objectName )
            throws FederateInternalError
    {
        if (theObjectClass.equals(federate.simulationParametersWrapper.getHandle())) {
            federate.simulationParametersObjectInstanceHandle = theObject;
        }
//        log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
//                theObjectClass + ", name=" + objectName );
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrder,
                                        TransportationTypeHandle transport,
                                        SupplementalReflectInfo reflectInfo )
            throws FederateInternalError
    {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        reflectAttributeValues( theObject,
                theAttributes,
                tag,
                sentOrder,
                transport,
                null,
                sentOrder,
                reflectInfo );
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrdering,
                                        TransportationTypeHandle theTransport,
                                        LogicalTime time,
                                        OrderType receivedOrdering,
                                        SupplementalReflectInfo reflectInfo )
            throws FederateInternalError
    {
        StringBuilder builder = new StringBuilder( "Reflection for object:" );

        // print the handle
        builder.append( " handle=" + theObject );
        // print the tag
        builder.append( ", tag=" + new String(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( time != null )
        {
            builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
        }

        // print the attribute information
        builder.append( ", attributeCount=" + theAttributes.size() );
        builder.append( "\n" );

        if (theObject.equals(federate.simulationParametersObjectInstanceHandle)) {
            for (AttributeHandle attribute : theAttributes.keySet()) {
                int maxQueueSize = 0;
                int percentageOfCustomersDoingSmallShopping = 0;
                int initialNumberOfCheckouts = 0;
                try {
                    if (attribute.equals(federate.simulationParametersWrapper.getAttribute("maxQueueSize"))) {
                        byte[] bytes = theAttributes.get(attribute);
                        maxQueueSize = Utils.byteToInt(bytes);
                        builder.append(", maxQueueSize = " + maxQueueSize);
                    } else if (attribute.equals(federate.simulationParametersWrapper.getAttribute("percentageOfCustomersDoingSmallShopping"))) {
                        byte[] bytes = theAttributes.get(attribute);
                        percentageOfCustomersDoingSmallShopping = Utils.byteToInt(bytes);
                        builder.append(", percentageOfCustomersDoingSmallShopping = " + percentageOfCustomersDoingSmallShopping);
                    } else {
                        byte[] bytes = theAttributes.get(attribute);
                        initialNumberOfCheckouts = Utils.byteToInt(bytes);
                        builder.append(", initialNumberOfCheckouts = " + initialNumberOfCheckouts);
                    }
                } catch (RTIexception rtIexception) {
                    rtIexception.printStackTrace();
                }
            }
        }

        log( builder.toString() );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    SupplementalReceiveInfo receiveInfo )
            throws FederateInternalError
    {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        this.receiveInteraction( interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    LogicalTime time,
                                    OrderType receivedOrdering,
                                    SupplementalReceiveInfo receiveInfo )
            throws FederateInternalError
    {
        StringBuilder builder = new StringBuilder( "customer federate - Interaction Received:" );

        if( interactionClass.equals(federate.startSimulationHandleWrapper.getHandle())) {
            builder.append(" (StartSimulation) received");
            federate.setSimulationStarted(true);
        } else if(interactionClass.equals(federate.stopSimulationHandleWrapper.getHandle())) {
            builder.append(" (StopSimulation) received");
            this.isRunning = false;
        }
        else if( interactionClass.equals(federate.endShoppingHandleWrapper.getHandle()))
        {
            builder.append( " (EndShopping) received" );
            int customerId = 0;
            int numberOfProductsInBasket = 0;
            int valueOfProducts = 0;
            for(ParameterHandle parameter : theParameters.keySet()){
                if (parameter.equals(federate.customerIdParameterHandleEndShopping)) {
                    byte[] bytes = theParameters.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                    builder.append(", customerId = " + customerId);
                } else if (parameter.equals(federate.numberOfProductsInBasketParameterHandleEndShopping)) {
                    byte[] bytes = theParameters.get(parameter);
                    numberOfProductsInBasket = Utils.byteToInt(bytes);
                    builder.append(", numberOfProductsInBasket = " + numberOfProductsInBasket);
                } else {
                    byte[] bytes = theParameters.get(parameter);
                    valueOfProducts = Utils.byteToInt(bytes);
                    builder.append(", valueOfProducts = " + valueOfProducts);
                }
            }

            federate.doingShoppingCustomers.add(new Event(interactionClass, theParameters, time));
        } else if( interactionClass.equals(federate.servicingCustomerHandleWrapper.getHandle())) {
            builder.append( " (ServivingCustomer) received" );
            int customerId = 0;
            int checkoutId = 0;
            for(ParameterHandle parameter : theParameters.keySet()){
                if (parameter.equals(federate.customerIdParameterHandleServicingCustomer)) {
                    byte[] bytes = theParameters.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                    builder.append(", customerId = " + customerId);
                } else {
                    byte[] bytes = theParameters.get(parameter);
                    checkoutId = Utils.byteToInt(bytes);
                    builder.append(", checkoutId = " + checkoutId);
                }
            }

            federate.servicingCustomers.add(new Event(interactionClass, theParameters, time));
        } else if( interactionClass.equals(federate.exitShopHandleWrapper.getHandle())) {
            builder.append( " (ExitShop) received" );
            int customerId = 0;
            for(ParameterHandle parameter : theParameters.keySet()){
                byte[] bytes = theParameters.get(parameter);
                customerId = Utils.byteToInt(bytes);
                builder.append(", customerId = " + customerId);
            }
        }

        // print the handle

        // print the tag
        builder.append( ", tag=" + new String(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( time != null )
        {
            builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
        }

        // print the parameer information
        builder.append( ", parameterCount=" + theParameters.size() );
        builder.append( "\n" );
        for( ParameterHandle parameter : theParameters.keySet() )
        {
            // print the parameter handle
            builder.append( "\tparamHandle=" );
            builder.append( parameter );
            // print the parameter value
            builder.append( ", paramValue=" );
            builder.append( theParameters.get(parameter).length );
            builder.append( " bytes" );
            builder.append( "\n" );
        }

        log( builder.toString() );
    }

    @Override
    public void removeObjectInstance( ObjectInstanceHandle theObject,
                                      byte[] tag,
                                      OrderType sentOrdering,
                                      SupplementalRemoveInfo removeInfo )
            throws FederateInternalError
    {
        log( "Object Removed: handle=" + theObject );
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
}
