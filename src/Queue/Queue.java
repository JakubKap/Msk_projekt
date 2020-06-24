package Queue;

import hla.rti1516e.ObjectInstanceHandle;

import java.util.LinkedList;
import java.util.List;

public class Queue {
    private final int id;
    private final LinkedList<Integer> customerListIds = new LinkedList<>();
    private final int checkoutId;
    private static int currentId = 0;
    private ObjectInstanceHandle handler;
    private final boolean isPrivileged;

    public Queue(boolean isPrivileged) {
        this.id = currentId;
        this.checkoutId = currentId++;
        this.isPrivileged = isPrivileged;
    }

    public int getId() {
        return id;
    }

    public LinkedList<Integer> getCustomerListIds() {
        return customerListIds;
    }

    public int getCheckoutId() {
        return checkoutId;
    }

    public ObjectInstanceHandle getHandler() {
        return handler;
    }

    public void setHandler(ObjectInstanceHandle handler) {
        this.handler = handler;
    }

    public boolean isPrivileged() {
        return isPrivileged;
    }
}
