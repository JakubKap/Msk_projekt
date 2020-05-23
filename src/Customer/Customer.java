package Customer;

public class Customer {
    private int id;
    private int numberOfProductsInBasket;
    private int valueOfProducts;
    private static int currentId = 0;

    public Customer(int numberOfProductsInBasket, int valueOfProducts) {
        this.id = currentId++;
        this.numberOfProductsInBasket = numberOfProductsInBasket;
        this.valueOfProducts = valueOfProducts;
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
}
