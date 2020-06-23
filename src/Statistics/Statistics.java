package Statistics;

import hla.rti1516e.LogicalTime;
import hla.rti1516e.time.HLAfloat64Time;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Statistics {
    private double avgPayingDuration;
    private double avgBeingInShopDuration;
    private double avgBeingInQueueDuration;
    private double avgBeingInCheckoutDuration;
    private int avgNumberOfProductsInBasket;
    private int percentOfPrivilegedCheckouts;
    private int avgNumberOfClientsInQueue;

    private int numOfOrdinaryCheckouts = 0;
    private int numOfPrivilegedCheckouts = 0;

    Map<Integer, LogicalTime> clientsEnterShopTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsExitShopTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterCheckoutTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterQueueTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsPayTimes = new HashMap<>();

    List<Integer> clientNumberOfProducts = new LinkedList<>();

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

    public double getAvgNumberOfProductsInBasket(){
        double sumOfProductsCount=0;

        for(Integer productCount : clientNumberOfProducts)
            sumOfProductsCount += productCount;

        return sumOfProductsCount/(clientNumberOfProducts.size());
    }

    public void setAvgBeingInCheckoutDuration(float avgBeingInCheckoutDuration) {
        this.avgBeingInCheckoutDuration = avgBeingInCheckoutDuration;
    }

    public void setAvgNumberOfProductsInBasket(int avgNumberOfProductsInBasket) {
        this.avgNumberOfProductsInBasket = avgNumberOfProductsInBasket;
    }

    public int getAvgNumberOfClientsInQueue() {
        return avgNumberOfClientsInQueue;
    }

    public void setAvgNumberOfClientsInQueue(int avgNumberOfClientsInQueue) {
        this.avgNumberOfClientsInQueue = avgNumberOfClientsInQueue;
    }

    public void incNumOfOrdinaryCheckouts(){
        numOfOrdinaryCheckouts++;
    }

    public void incNumOfPrivilegedCheckouts(){
        numOfPrivilegedCheckouts++;
    }

    public double getPercentageOfPrivilegedCheckouts(){
        return (double)(100*numOfPrivilegedCheckouts)/(numOfOrdinaryCheckouts + numOfPrivilegedCheckouts);
    }
}
