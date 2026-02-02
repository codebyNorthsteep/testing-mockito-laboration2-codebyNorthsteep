package com.example;

import com.example.shop.Item;
import com.example.shop.ShoppingCart;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    //Test for calculation totalprice

    @Test
    void get_total_price_for_items_in_shopping_cart(){
        ShoppingCart shoppingCart = new ShoppingCart();

        Stream.of(
                new Item("Juice", 34.00, 1),
                new Item("Milk", 19.00, 2),
                new Item("Sugar", 28.00, 1)
        ).forEach(shoppingCart::addItem);

        assertEquals(100.00, shoppingCart.getTotalPrice(), "Summan av varorna ska bli 100kr");
    }

}
