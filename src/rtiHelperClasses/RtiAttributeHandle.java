package rtiHelperClasses;

import hla.rti1516e.AttributeHandle;

public class RtiAttributeHandle {
    private AttributeHandle handle;
    private String handleString;

    public RtiAttributeHandle(String handleString) {
        this.handleString = handleString;
    }

    public AttributeHandle getHandle() {
        return handle;
    }
}
