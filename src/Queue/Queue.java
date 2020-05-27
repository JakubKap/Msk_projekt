package Queue;

import hla.rti1516e.ObjectInstanceHandle;

import java.util.LinkedList;
import java.util.List;

public class Queue {
    private int id;
    private int maxLimit;
    private List<Integer> customerListIds = new LinkedList<>();
    private int checkoutId;
    private static int currentId = 0;
    private ObjectInstanceHandle handler;

    public Queue(int maxLimit) {
        this.maxLimit = maxLimit;
        this.id = currentId;
        this.checkoutId = currentId++;
        // TODO zsynchronizuj ID kolejki z ID kasy
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

    public List<Integer> getCustomerListIds() {
        return customerListIds;
    }

    public void setCustomerListIds(List<Integer> customerListIds) {
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
}
