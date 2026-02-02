package com.example;

import com.example.shop.Item;
import com.example.shop.ShoppingCart;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShoppingCartTest {
    //Lägg till en vara i shoppingCart
    @Test
    void add_one_item_to_shopping_cart(){

        ShoppingCart shoppingCart = new ShoppingCart();
        Item item = new Item("Juice", 34.00,1);

        shoppingCart.addItem(item);

        assertThat(shoppingCart.getShoppingList()).contains(item);


    }

    @Test
    void remove_an_item_from_shopping_cart(){
        ShoppingCart shoppingCart = new ShoppingCart();

        Item item = new Item("Milk", 19.00, 1);
        shoppingCart.addItem(item);

        shoppingCart.removeItem(item);

        assertThat(shoppingCart.getShoppingList()).isEmpty();
    }


}
