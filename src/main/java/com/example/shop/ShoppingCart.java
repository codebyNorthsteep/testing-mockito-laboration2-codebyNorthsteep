package com.example.shop;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    private List<Item> shoppingList = new ArrayList<>();
    private double baseDiscountRate = 0.0;

    public void addItem(Item newItem) {
        if (newItem.getPrice() < 0)
            throw new IllegalArgumentException("Pris kan ej vara negativt");

        if (newItem.getQuantity() <= 0)
            throw new IllegalArgumentException("Kvantitet måste vara större än 0");

        if (newItem.getItemName() == null || newItem.getItemName().isBlank())
            throw new IllegalArgumentException("Ej giltigt varunamn");

        for (Item existingItem : shoppingList) {
            if (existingItem.equals(newItem)) {
                int updatedQuantity = existingItem.getQuantity() + newItem.getQuantity();
                existingItem.setQuantity(updatedQuantity);
                return;
            }
        }

        shoppingList.add(newItem);
    }

    public List<Item> getShoppingList() {
        return this.shoppingList;
    }

    public void removeItem(Item item) {

        if (!shoppingList.contains(item))
            throw new IllegalStateException("Kan ej ta bort ikke-existerande vara, listan lämnas oförändrad.");

        shoppingList.remove(item);

    }

    public double getTotalPrice() {
        double totalBeforeDiscount = shoppingList.stream()
                .mapToDouble(Item::getSubTotalPrice)
                .sum();

        return totalBeforeDiscount * (1 - baseDiscountRate);
    }

    public void addDiscount(double discountRate) {
        if (discountRate < 0.0 || discountRate >= 1.0) {
            throw new IllegalArgumentException("Ogiltig rabatt.");
        }

        this.baseDiscountRate = discountRate;
    }
}
