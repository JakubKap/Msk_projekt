package rtiHelperClasses;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

import java.util.ArrayList;
import java.util.List;

public class RtiHelper {
    RTIambassador rtiamb;
    private List<RtiObjectClassHandle> objectClassHandles = new ArrayList<>();
    private List<RtiInteractionClassHandle> interactionClassHandles = new ArrayList<>();

    public RtiHelper(RTIambassador rtiamb) {
        this.rtiamb = rtiamb;
    }

    public RtiObjectClassHandle addObject(String handleString) throws RTIexception {
        RtiObjectClassHandle handle = new RtiObjectClassHandle(rtiamb, handleString);
        objectClassHandles.add(handle);
        return handle;
    }

    public RtiInteractionClassHandle addInteraction(String handleString) throws RTIexception {
        RtiInteractionClassHandle handle = new RtiInteractionClassHandle(rtiamb, handleString);
        interactionClassHandles.add(handle);
        return handle;
    }

    public List<RtiObjectClassHandle> getObjectClassHandles() {
        return objectClassHandles;
    }

    public List<RtiInteractionClassHandle> getInteractionClassHandles() {
        return interactionClassHandles;
    }
}
