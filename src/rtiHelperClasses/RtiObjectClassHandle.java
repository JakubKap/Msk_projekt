package rtiHelperClasses;

import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIexception;

import java.util.ArrayList;
import java.util.List;

public class RtiObjectClassHandle extends RtiClassHandle {
    private ObjectClassHandle handle;
    private List<RtiAttributeHandle> attributes = new ArrayList<>();

    public RtiObjectClassHandle(RTIambassador rtiamb, String handleString) throws RTIexception {
        super(rtiamb, handleString);
        this.handle = rtiamb.getObjectClassHandle(handleString);
    }

    public void addAttribute(String attributeString) throws RTIexception {
        attributes.add(new RtiAttributeHandle(rtiamb, handle, attributeString));
    }

    public void addAttributes(String... attributes) throws RTIexception {
        for (String attribute : attributes) {
            addAttribute(attribute);
        }
    }

    @Override
    public void subscribe() throws RTIexception {
        rtiamb.subscribeObjectClassAttributes(handle, attributeToHandleSet());
    }

    @Override
    public void publish() throws RTIexception {
        rtiamb.publishObjectClassAttributes(handle, attributeToHandleSet());
    }

    private AttributeHandleSet attributeToHandleSet() throws FederateNotExecutionMember, NotConnected {
        AttributeHandleSet attributeHandleSet = rtiamb.getAttributeHandleSetFactory().create();
        for (RtiAttributeHandle attribute : attributes) {
            attributeHandleSet.add(attribute.getHandle());
        }
        return attributeHandleSet;
    }

    public ObjectClassHandle getHandle() {
        return handle;
    }
}
