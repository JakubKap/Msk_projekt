package utils;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.time.HLAfloat64Time;

public class Event implements Comparable<Event> {
    private final InteractionClassHandle interactionClassHandle;
    private final ParameterHandleValueMap parameterHandleValueMap;
    private LogicalTime time;

    public Event(InteractionClassHandle interactionClassHandle, ParameterHandleValueMap parameterHandleValueMap, LogicalTime time) {
        this.interactionClassHandle = interactionClassHandle;
        this.parameterHandleValueMap = parameterHandleValueMap;
        this.time = time;
    }

    public InteractionClassHandle getInteractionClassHandle() {
        return interactionClassHandle;
    }

    public ParameterHandleValueMap getParameterHandleValueMap() {
        return parameterHandleValueMap;
    }

    @Override
    public int compareTo(Event event) {
        double time1 = ((HLAfloat64Time)(this.time)).getValue();
        double time2 = ((HLAfloat64Time)(event.time)).getValue();
        return (time1 < time2) ? -1: 1;
    }
}
