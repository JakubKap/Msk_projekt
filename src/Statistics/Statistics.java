package Statistics;

import hla.rti1516e.LogicalTime;
import hla.rti1516e.time.HLAfloat64Time;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Statistics {
    private int numOfOrdinaryCheckouts = 0;
    private int numOfPrivilegedCheckouts = 0;

    Map<Integer, LogicalTime> clientsEnterShopTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsExitShopTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterCheckoutTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterQueueTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsPayTimes = new HashMap<>();

    Map<Integer, LogicalTime> clientsEnterOrdinaryCheckoutTimes = new HashMap<>();
    Map<Integer, LogicalTime> clientsEnterPrivilegedCheckoutTimes = new HashMap<>();

    List<Integer> clientNumberOfProducts = new LinkedList<>();

    public double countAvgBeingInShopDuration() {
        double sumOfTimes = 0;
        for(Map.Entry<Integer, LogicalTime> entry: clientsExitShopTimes.entrySet()) {
            double enterTime = ((HLAfloat64Time)clientsEnterShopTimes.get(entry.getKey())).getValue();
            double exitTime = ((HLAfloat64Time)clientsExitShopTimes.get(entry.getKey())).getValue();
            sumOfTimes += (exitTime - enterTime);
        }
        return sumOfTimes / clientsExitShopTimes.size();
    }

    public double countAvgBeingInQueueDuration() {
        double sumOfTimes = 0;
        for(Map.Entry<Integer, LogicalTime> entry: clientsEnterCheckoutTimes.entrySet()) {
            double enterQueue = ((HLAfloat64Time)clientsEnterQueueTimes.get(entry.getKey())).getValue();
            double enterCheckout = ((HLAfloat64Time)clientsEnterCheckoutTimes.get(entry.getKey())).getValue();
            sumOfTimes += (enterCheckout - enterQueue);
        }
        return sumOfTimes / clientsEnterCheckoutTimes.size();
    }

    public double countAvgBeingInCheckoutDuration() {
        double sumOfTimes = 0;
        for(Map.Entry<Integer, LogicalTime> entry: clientsPayTimes.entrySet()) {
            double enterQueue = ((HLAfloat64Time)clientsEnterCheckoutTimes.get(entry.getKey())).getValue();
            double endPay = ((HLAfloat64Time)clientsPayTimes.get(entry.getKey())).getValue();
            sumOfTimes += (endPay - enterQueue);
        }
        return sumOfTimes / clientsPayTimes.size();
    }

    public double countAvgBeingInOrdinaryCheckoutDuration() {
        double sumOfTimes = 0;
        Map<Integer, LogicalTime> clientsInOrdinaryCheckoutPayTimes = clientsPayTimes
                .keySet()
                .stream()
                .filter(clientsEnterOrdinaryCheckoutTimes::containsKey)
                .collect(Collectors.toMap(Function.identity(), clientsPayTimes::get));

        for(Map.Entry<Integer, LogicalTime> entry: clientsInOrdinaryCheckoutPayTimes.entrySet()) {
            double enterQueue = ((HLAfloat64Time)clientsEnterOrdinaryCheckoutTimes.get(entry.getKey())).getValue();
            double endPay = ((HLAfloat64Time)clientsInOrdinaryCheckoutPayTimes.get(entry.getKey())).getValue();
            sumOfTimes += (endPay - enterQueue);
        }
        return sumOfTimes / clientsInOrdinaryCheckoutPayTimes.size();
    }

    public double countAvgBeingInPrivilegedCheckoutDuration() {
        double sumOfTimes = 0;
        Map<Integer, LogicalTime> clientsInPrivilegedCheckoutPayTimes = clientsPayTimes
                .keySet()
                .stream()
                .filter(clientsEnterPrivilegedCheckoutTimes::containsKey)
                .collect(Collectors.toMap(Function.identity(), clientsPayTimes::get));

        for(Map.Entry<Integer, LogicalTime> entry: clientsInPrivilegedCheckoutPayTimes.entrySet()) {
            double enterQueue = ((HLAfloat64Time)clientsEnterPrivilegedCheckoutTimes.get(entry.getKey())).getValue();
            double endPay = ((HLAfloat64Time)clientsInPrivilegedCheckoutPayTimes.get(entry.getKey())).getValue();
            sumOfTimes += (endPay - enterQueue);
        }
        return sumOfTimes / clientsInPrivilegedCheckoutPayTimes.size();
    }

    public double countAvgNumberOfProductsInBasket(){
        double sumOfProductsCount=0;

        for(Integer productCount : clientNumberOfProducts)
            sumOfProductsCount += productCount;

        return sumOfProductsCount/(clientNumberOfProducts.size());
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

    public int getFinalNumberOfCheckouts() {
        return numOfOrdinaryCheckouts + numOfPrivilegedCheckouts;
    }
}
