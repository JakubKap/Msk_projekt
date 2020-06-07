package Statistics;

import hla.rti1516e.LogicalTime;
import hla.rti1516e.time.HLAfloat64Time;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
    private double avgPayingDuration;
    private double avgBeingInShopDuration;
    private double avgBeingInQueueDuration;
    private double avgBeingInCheckoutDuration;
    private int avgNumberOfProductsInBasket;
    private int percentOfPrivilegedCheckouts;
    private int avgNumberOfClientsInQueue;

    Map<Integer, LogicalTime> clientsEnterShopTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsExitShopTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterCheckoutTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterQueueTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsPayTimes = new HashMap<>();


    public double getAvgPayingDuration() {
        return avgPayingDuration;
    }

    public void setAvgPayingDuration(float avgPayingDuration) {
        this.avgPayingDuration = avgPayingDuration;
    }

    public double getAvgBeingInShopDuration() {
        double sumOfTimes = 0;
        for(Map.Entry<Integer, LogicalTime> entry: clientsExitShopTimes.entrySet()) {
            double enterTime = ((HLAfloat64Time)clientsEnterShopTimes.get(entry.getKey())).getValue();
            double exitTime = ((HLAfloat64Time)clientsExitShopTimes.get(entry.getKey())).getValue();
            sumOfTimes += (exitTime - enterTime);
        }
        return sumOfTimes / clientsExitShopTimes.size();
    }

    public void setAvgBeingInShopDuration(float avgBeingInShopDuration) {
        this.avgBeingInShopDuration = avgBeingInShopDuration;
    }

    public double getAvgBeingInQueueDuration() {
        double sumOfTimes = 0;
        for(Map.Entry<Integer, LogicalTime> entry: clientsEnterCheckoutTimes.entrySet()) {
            double enterQueue = ((HLAfloat64Time)clientsEnterQueueTimes.get(entry.getKey())).getValue();
            double enterCheckout = ((HLAfloat64Time)clientsEnterCheckoutTimes.get(entry.getKey())).getValue();
            sumOfTimes += (enterCheckout - enterQueue);
        }
        return sumOfTimes / clientsEnterCheckoutTimes.size();
    }

    public void setAvgBeingInQueueDuration(float avgBeingInQueueDuration) {
        this.avgBeingInQueueDuration = avgBeingInQueueDuration;
    }

    public double getAvgBeingInCheckoutDuration() {
        double sumOfTimes = 0;
        for(Map.Entry<Integer, LogicalTime> entry: clientsPayTimes.entrySet()) {
            double enterQueue = ((HLAfloat64Time)clientsEnterCheckoutTimes.get(entry.getKey())).getValue();
            double endPay = ((HLAfloat64Time)clientsPayTimes.get(entry.getKey())).getValue();
            sumOfTimes += (endPay - enterQueue);
        }
        return sumOfTimes / clientsPayTimes.size();
    }

    public void setAvgBeingInCheckoutDuration(float avgBeingInCheckoutDuration) {
        this.avgBeingInCheckoutDuration = avgBeingInCheckoutDuration;
    }

    public int getAvgNumberOfProductsInBasket() {
        return avgNumberOfProductsInBasket;
    }

    public void setAvgNumberOfProductsInBasket(int avgNumberOfProductsInBasket) {
        this.avgNumberOfProductsInBasket = avgNumberOfProductsInBasket;
    }

    public int getPercentOfPrivilegedCheckouts() {
        return percentOfPrivilegedCheckouts;
    }

    public void setPercentOfPrivilegedCheckouts(int percentOfPrivilegedCheckouts) {
        this.percentOfPrivilegedCheckouts = percentOfPrivilegedCheckouts;
    }

    public int getAvgNumberOfClientsInQueue() {
        return avgNumberOfClientsInQueue;
    }

    public void setAvgNumberOfClientsInQueue(int avgNumberOfClientsInQueue) {
        this.avgNumberOfClientsInQueue = avgNumberOfClientsInQueue;
    }
}
