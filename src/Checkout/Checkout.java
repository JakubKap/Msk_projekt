package Checkout;

import hla.rti1516e.ObjectInstanceHandle;

public class Checkout {
    private int id;
    private boolean isPrivileged;
    private boolean isFree;
    private ObjectInstanceHandle handler;

    public Checkout(int checkoutId, boolean isPrivileged, boolean isFree) {
        this.id = checkoutId;
        this.isPrivileged = isPrivileged;
        this.isFree = isFree;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPrivileged() {
        return isPrivileged;
    }

    public void setPrivileged(boolean privileged) {
        isPrivileged = privileged;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public void setHandler(ObjectInstanceHandle handler) {
        this.handler = handler;
    }

    public ObjectInstanceHandle getHandler() {
        return handler;
    }
}
