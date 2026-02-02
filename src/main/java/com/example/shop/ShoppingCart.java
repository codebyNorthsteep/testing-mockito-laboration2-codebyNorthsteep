package com.example.shop;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    private List<Item> shoppingList = new ArrayList<>();
    private double baseDiscountRate = 0.0;

    public void addItem(Item item) {

        shoppingList.add(item);
    }

    public List<Item> getShoppingList() {
        return this.shoppingList;
    }

    public void removeItem(Item item) {

        shoppingList.remove(item);

    }

    public double getTotalPrice() {
        double totalBeforeDiscount = shoppingList.stream()
                .mapToDouble(Item::getSubTotalPrice)
                .sum();

        return totalBeforeDiscount * (1 - baseDiscountRate);
    }

    public void addDiscount(double discountRate) {
        this.baseDiscountRate = discountRate;
    }
}
