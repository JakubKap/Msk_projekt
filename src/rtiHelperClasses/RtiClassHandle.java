package rtiHelperClasses;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

public abstract class RtiClassHandle {
    protected String handleString;
    protected RTIambassador rtiamb;

    public RtiClassHandle(RTIambassador rtiamb, String handleString) {
        this.handleString = handleString;
        this.rtiamb = rtiamb;
    }

    public abstract void subscribe() throws RTIexception;
    public abstract void publish() throws RTIexception;
    protected final void publishAndSubscribe() throws RTIexception {
        publish();
        subscribe();
    }
}
