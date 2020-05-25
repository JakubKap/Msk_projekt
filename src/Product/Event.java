package Product;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandleValueMap;

public class Event  {
    private InteractionClassHandle interactionClassHandle;
    private ParameterHandleValueMap parameterHandleValueMap;

    public Event(InteractionClassHandle interactionClassHandle, ParameterHandleValueMap parameterHandleValueMap) {
        this.interactionClassHandle = interactionClassHandle;
        this.parameterHandleValueMap = parameterHandleValueMap;
    }

    public InteractionClassHandle getInteractionClassHandle() {
        return interactionClassHandle;
    }

    public void setInteractionClassHandle(InteractionClassHandle interactionClassHandle) {
        this.interactionClassHandle = interactionClassHandle;
    }

    public ParameterHandleValueMap getParameterHandleValueMap() {
        return parameterHandleValueMap;
    }

    public void setParameterHandleValueMap(ParameterHandleValueMap parameterHandleValueMap) {
        this.parameterHandleValueMap = parameterHandleValueMap;
    }
}
