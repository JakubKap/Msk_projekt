package rtiHelperClasses;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

public class RtiAttributeHandle {
    private AttributeHandle handle;
    private String handleString;

    public RtiAttributeHandle(RTIambassador rtiamb, ObjectClassHandle objectHandle, String handleString) throws RTIexception {
        this.handle = rtiamb.getAttributeHandle( objectHandle, handleString );
        this.handleString = handleString;
    }

    public AttributeHandle getHandle() {
        return handle;
    }
}
