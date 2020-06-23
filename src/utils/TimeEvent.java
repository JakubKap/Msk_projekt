package utils;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.ParameterHandleValueMap;

public class TimeEvent extends Event {
    private int intervalTime;
    private int currentTime;

    public TimeEvent(InteractionClassHandle interactionClassHandle, ParameterHandleValueMap parameterHandleValueMap, LogicalTime time, int intervalTime) {
        super(interactionClassHandle, parameterHandleValueMap, time);
        this.intervalTime = intervalTime;
    }

    public boolean isReadyToHandle() {
        return currentTime >= intervalTime;
    }

    public void incrementTime() {
        this.currentTime++;
    }

    public int getIntervalTime() {
        return intervalTime;
    }

    public int getCurrentTime() {
        return currentTime;
    }
}
