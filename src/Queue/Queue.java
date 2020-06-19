package Queue;

import hla.rti1516e.ObjectInstanceHandle;

import java.util.LinkedList;
import java.util.List;

public class Queue {
    private int id;
    private int maxLimit;
    private LinkedList<Integer> customerListIds = new LinkedList<>();
    private int checkoutId;
    private static int currentId = 0;
    private ObjectInstanceHandle handler;
    private boolean isPrivileged;

    public Queue(int maxLimit, boolean isPrivileged) {
        this.maxLimit = maxLimit;
        this.id = currentId;
        this.checkoutId = currentId++;
        this.isPrivileged = isPrivileged;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public LinkedList<Integer> getCustomerListIds() {
        return customerListIds;
    }

    public void setCustomerListIds(LinkedList<Integer> customerListIds) {
        this.customerListIds = customerListIds;
    }

    public int getCheckoutId() {
        return checkoutId;
    }

    public void setCheckoutId(int checkoutId) {
        this.checkoutId = checkoutId;
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

    public void setPrivileged(boolean privileged) {
        isPrivileged = privileged;
    }
}
