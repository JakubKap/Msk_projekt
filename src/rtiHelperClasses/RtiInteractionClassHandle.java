package rtiHelperClasses;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

public class RtiInteractionClassHandle extends RtiClassHandle{
    private InteractionClassHandle handle;

    public RtiInteractionClassHandle(RTIambassador rtiamb, String handleString) throws RTIexception {
        super(rtiamb, handleString);
        this.handle = rtiamb.getInteractionClassHandle( handleString );
    }

    @Override
    public void subscribe() throws RTIexception {
        rtiamb.subscribeInteractionClass(handle);
    }

    @Override
    public void publish() throws RTIexception {
        rtiamb.publishInteractionClass(handle);
    }

    public InteractionClassHandle getHandle() {
        return handle;
    }
}
