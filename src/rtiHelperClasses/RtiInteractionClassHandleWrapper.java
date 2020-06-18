package rtiHelperClasses;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.*;

public class RtiInteractionClassHandleWrapper extends RtiClassHandle{
    private InteractionClassHandle handle;

    public RtiInteractionClassHandleWrapper(RTIambassador rtiamb, String handleString) throws RTIexception {
        super(rtiamb, handleString);
        this.handle = rtiamb.getInteractionClassHandle( handleString );
    }

    public ParameterHandle getParameter(String parameterString) {
        ParameterHandle parameterHandle = null;
        try {
            parameterHandle = rtiamb.getParameterHandle(handle, parameterString);
        } catch (NameNotFound nameNotFound) {
            nameNotFound.printStackTrace();
        } catch (InvalidInteractionClassHandle invalidInteractionClassHandle) {
            invalidInteractionClassHandle.printStackTrace();
        } catch (FederateNotExecutionMember federateNotExecutionMember) {
            federateNotExecutionMember.printStackTrace();
        } catch (NotConnected notConnected) {
            notConnected.printStackTrace();
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        }
        return parameterHandle;
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
