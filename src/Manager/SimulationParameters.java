package Manager;

import hla.rti1516e.ObjectInstanceHandle;

public class SimulationParameters {
    private int maxQueueSize;
    private int percentageOfCustomersDoingSmallShopping;
    private int initialNumberOfCheckouts;
    private ObjectInstanceHandle handler;

    public void setHandler(ObjectInstanceHandle handler) {
        this.handler = handler;
    }

    public ObjectInstanceHandle getHandler() {
        return handler;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public int getPercentageOfCustomersDoingSmallShopping() {
        return percentageOfCustomersDoingSmallShopping;
    }

    public void setPercentageOfCustomersDoingSmallShopping(int percentageOfCustomersDoingSmallShopping) {
        this.percentageOfCustomersDoingSmallShopping = percentageOfCustomersDoingSmallShopping;
    }

    public int getInitialNumberOfCheckouts() {
        return initialNumberOfCheckouts;
    }

    public void setInitialNumberOfCheckouts(int initialNumberOfCheckouts) {
        this.initialNumberOfCheckouts = initialNumberOfCheckouts;
    }
}
