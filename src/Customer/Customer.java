package Customer;

import hla.rti1516e.ObjectInstanceHandle;

public class Customer {
    private int id;
    private int numberOfProductsInBasket;
    private int valueOfProducts;
    private boolean endShopping;
    private static int currentId = 0;
    private ObjectInstanceHandle handler;

    public Customer() {
        this.id = currentId++;
    }

    public Customer(int id) {
        this.id = id;
    }

    public Customer(ObjectInstanceHandle handler) {
        this.id = currentId++;
        this.handler = handler;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumberOfProductsInBasket() {
        return numberOfProductsInBasket;
    }

    public void setNumberOfProductsInBasket(int numberOfProductsInBasket) {
        this.numberOfProductsInBasket = numberOfProductsInBasket;
    }

    public int getValueOfProducts() {
        return valueOfProducts;
    }

    public void setValueOfProducts(int valueOfProducts) {
        this.valueOfProducts = valueOfProducts;
    }

    public boolean isEndedShopping() {
        return endShopping;
    }

    public void setEndShopping(boolean endShopping) {
        this.endShopping = endShopping;
    }

    public ObjectInstanceHandle getHandler() {
        return handler;
    }

    public void setHandler(ObjectInstanceHandle handler) {
        this.handler = handler;
    }
}
