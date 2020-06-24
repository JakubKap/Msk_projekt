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
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.Event;
import utils.TimeEvent;
import utils.Utils;

import java.util.Optional;
import java.util.Random;

/**
 * This class handles all incoming callbacks from the RTI regarding a particular
 * {@link Customer}. It will log information about any callbacks it
 * receives, thus demonstrating how to deal with the provided callback information.
 */
class CheckoutFederateAmbassador extends NullFederateAmbassador {
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private CheckoutFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean isRunning = true;

    private final Random random = new Random();

    //----------------------------------------------------------
    //                      CONSTRUCTORS
    //----------------------------------------------------------

    public CheckoutFederateAmbassador(CheckoutFederate checkoutFederate) {
        this.federate = checkoutFederate;
    }

    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------
    private void log(String message) {
        System.out.println("CheckoutFederateAmbassador   : " + message);
    }

    //////////////////////////////////////////////////////////////////////////
    ////////////////////////// RTI Callback Methods //////////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void synchronizationPointRegistrationFailed(String label,
                                                       SynchronizationPointFailureReason reason) {
        log("Failed to register sync point: " + label + ", reason=" + reason);
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Synchronization point announced: " + label);
        if (label.equals(CheckoutFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(CheckoutFederate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    @Override
    public void timeRegulationEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance(ObjectInstanceHandle theObject,
                                       ObjectClassHandle theObjectClass,
                                       String objectName)
            throws FederateInternalError {
        log("Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName);

        if (theObjectClass.equals(federate.simulationParametersWrapper.getHandle())) {
            federate.simulationParametersObjectInstanceHandle = theObject;
        }
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrder,
                                       TransportationTypeHandle transport,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        reflectAttributeValues(theObject,
                theAttributes,
                tag,
                sentOrder,
                transport,
                null,
                sentOrder,
                reflectInfo);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrdering,
                                       TransportationTypeHandle theTransport,
                                       LogicalTime time,
                                       OrderType receivedOrdering,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        StringBuilder builder = new StringBuilder("Reflection for object:");

        // print the handle
        builder.append(" handle=" + theObject);
        // print the tag
        builder.append(", tag=" + new String(tag));
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if (time != null) {
            builder.append(", time=" + ((HLAfloat64Time) time).getValue());
        }

        // print the attribute information
        builder.append(", attributeCount=" + theAttributes.size());
        builder.append("\n");

        builder.append("name: ");
        if (theObject.equals(this.federate.simulationParametersObjectInstanceHandle)) {
            builder.append("SimulationParameters");
            for (AttributeHandle attribute : theAttributes.keySet()) {
                try {
                    if (attribute.equals
                            (this.federate.simulationParametersWrapper.getAttribute("initialNumberOfCheckouts"))) {
                        byte[] bytes = theAttributes.get(attribute);
                        int numberOfCheckouts = Utils.byteToInt(bytes);
                        this.federate.numberOfCheckouts = numberOfCheckouts;
                        builder.append(" received, numberOfCheckouts = " + numberOfCheckouts);
                    }
                } catch (RTIexception rtIexception) {
                    rtIexception.printStackTrace();
                }
            }
        }

        log(builder.toString());
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        this.receiveInteraction(interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        StringBuilder builder = new StringBuilder("Interaction Received");
        builder.append("\ttag=" + new String(tag));
        if (time != null) {
            builder.append(", time=" + ((HLAfloat64Time) time).getValue());
        }
        builder.append( "\n" );

        if (interactionClass.equals(federate.enterCheckoutHandle)) {
            builder.append(" (EnterCheckout) received");
            builder.append(", parameterCount=" + theParameters.size());

            int customerId = 0;
            int checkoutId = 0;

            for (ParameterHandle parameter : theParameters.keySet()) {

                builder.append( "\tparamHandle=" );
                builder.append( parameter );

                if (parameter.equals(federate.customerIdParameterHandleEnterCheckout)) {
                    byte[] bytes = theParameters.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                    builder.append(", customerId = " + customerId);
                } else if (parameter.equals(federate.checkoutIdParameterHandleEnterCheckout)){
                    byte[] bytes = theParameters.get(parameter);
                    checkoutId = Utils.byteToInt(bytes);
                    builder.append(", checkoutId = " + checkoutId);
                }
            }

            int searchedCheckoutId = checkoutId;

            Optional<Checkout> optionalCheckout = this.federate.checkouts.stream()
                    .filter(checkout -> checkout.getId() == searchedCheckoutId)
                    .findFirst();
            if (optionalCheckout.isPresent()) {
                Checkout checkout = optionalCheckout.get();
                if (checkout.isPrivileged()) {
                    builder.append(", checkoutType: privileged");
                    federate.servicingCustomers.add(new TimeEvent(interactionClass, theParameters, time, random.nextInt(5) + 5));
                } else {
                    builder.append(", checkoutType: ordinary");
                    federate.servicingCustomers.add(new TimeEvent(interactionClass, theParameters, time,random.nextInt(5) + 20));
                }
            }


        } else if (interactionClass.equals(federate.payHandle)) {
            builder.append(" (Pay) received");
            builder.append(", parameterCount=" + theParameters.size());

            int customerId = 0;
            int checkoutId = 0;
            int price = 0;
            for (ParameterHandle parameter : theParameters.keySet()) {
                builder.append( "\tparamHandle=" );
                builder.append( parameter );

                if (parameter.equals(federate.customerIdParameterHandlePay)) {
                    byte[] bytes = theParameters.get(parameter);
                    customerId = Utils.byteToInt(bytes);
                    builder.append(", customerId = " + customerId);
                } else if (parameter.equals(federate.checkoutIdParameterHandlePay)) {
                    byte[] bytes = theParameters.get(parameter);
                    checkoutId = Utils.byteToInt(bytes);
                    builder.append(", checkoutId = " + checkoutId);
                } else {
                    byte[] bytes = theParameters.get(parameter);
                    price = Utils.byteToInt(bytes);
                    builder.append(", valueOfProducts = " + price);
                }
            }

            federate.customersToExit.add(new Event(interactionClass, theParameters, time));
        } else if (interactionClass.equals(federate.createCheckoutHandleWrapper.getHandle())) {
            builder.append(" (createCheckout) received");
            builder.append(", parameterCount=" + theParameters.size());

            int checkoutId = 0;
            boolean isPrivileged = false;
            for (ParameterHandle parameter : theParameters.keySet()) {
                builder.append( "\tparamHandle=" );
                builder.append( parameter );

                if (parameter.equals(federate.createCheckoutHandleWrapper.getParameter("checkoutId"))) {
                    byte[] bytes = theParameters.get(parameter);
                    checkoutId = Utils.byteToInt(bytes);
                    builder.append(", checkoutId = " + checkoutId);
                } else if (parameter.equals(federate.createCheckoutHandleWrapper.getParameter("isPrivileged"))) {
                    byte[] bytes = theParameters.get(parameter);
                    isPrivileged = Utils.byteToBoolean(bytes);
                    builder.append(", isPrivileged = " + isPrivileged);
                }
            }
            federate.createCheckoutEvents.add(new Event(interactionClass, theParameters, time));
        } else if(interactionClass.equals(federate.stopSimulationHandleWrapper.getHandle())){
            builder.append(" (StopSimulation) received");
            builder.append(", parameterCount=" + theParameters.size());

            this.isRunning = false;
        }

        log(builder.toString());
    }

    @Override
    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError {
        log("Object Removed: handle=" + theObject);
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
}
