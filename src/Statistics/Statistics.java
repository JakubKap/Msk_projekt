package Statistics;

public class Statistics {
    private float avgPayingDuration;
    private float avgBeingInShopDuration;
    private float avgBeingInQueueDuration;
    private float avgBeingInCheckoutDuration;
    private int avgNumberOfProductsInBasket;
    private int percentOfPrivilegedCheckouts;
    private int avgNumberOfClientsInQueue;

    public Statistics(float avgPayingDuration, float avgBeingInShopDuration, float avgBeingInQueueDuration, float avgBeingInCheckoutDuration, int avgNumberOfProductsInBasket, int percentOfPrivilegedCheckouts, int avgNumberOfClientsInQueue) {
        this.avgPayingDuration = avgPayingDuration;
        this.avgBeingInShopDuration = avgBeingInShopDuration;
        this.avgBeingInQueueDuration = avgBeingInQueueDuration;
        this.avgBeingInCheckoutDuration = avgBeingInCheckoutDuration;
        this.avgNumberOfProductsInBasket = avgNumberOfProductsInBasket;
        this.percentOfPrivilegedCheckouts = percentOfPrivilegedCheckouts;
        this.avgNumberOfClientsInQueue = avgNumberOfClientsInQueue;
    }

    public float getAvgPayingDuration() {
        return avgPayingDuration;
    }

    public void setAvgPayingDuration(float avgPayingDuration) {
        this.avgPayingDuration = avgPayingDuration;
    }

    public float getAvgBeingInShopDuration() {
        return avgBeingInShopDuration;
    }

    public void setAvgBeingInShopDuration(float avgBeingInShopDuration) {
        this.avgBeingInShopDuration = avgBeingInShopDuration;
    }

    public float getAvgBeingInQueueDuration() {
        return avgBeingInQueueDuration;
    }

    public void setAvgBeingInQueueDuration(float avgBeingInQueueDuration) {
        this.avgBeingInQueueDuration = avgBeingInQueueDuration;
    }

    public float getAvgBeingInCheckoutDuration() {
        return avgBeingInCheckoutDuration;
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
