package rtiHelperClasses;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

public class RtiInteractionClassHandleWrapper extends RtiClassHandle{
    private InteractionClassHandle handle;

    public RtiInteractionClassHandleWrapper(RTIambassador rtiamb, String handleString) throws RTIexception {
        super(rtiamb, handleString);
        this.handle = rtiamb.getInteractionClassHandle( handleString );
    }

    public ParameterHandle getParameter(String parameterString) throws RTIexception {
        return rtiamb.getParameterHandle(handle, parameterString);
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
