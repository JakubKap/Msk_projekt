package Queue;

import java.util.List;

public class Queue {
    private int id;
    private int maxLimit;
    private List<Integer> customerListIds;
    private int checkoutId;

    public Queue(int id, int maxLimit, List<Integer> customerListIds, int checkoutId) {
        this.id = id;
        this.maxLimit = maxLimit;
        this.customerListIds = customerListIds;
        this.checkoutId = checkoutId;
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
}
