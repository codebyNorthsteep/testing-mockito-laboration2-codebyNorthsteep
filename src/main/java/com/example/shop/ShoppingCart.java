package com.example.shop;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    List<Item> shoppingList = new ArrayList<>();

    public void addItem(Item item) {

        shoppingList.add(item);
    }

    public List<Item> getShoppingList() {
        return this.shoppingList;
    }
}
