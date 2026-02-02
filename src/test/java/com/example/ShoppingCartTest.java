package com.example;

import com.example.shop.Item;
import com.example.shop.ShoppingCart;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

public class ShoppingCartTest {
    //Lägg till en vara i shoppingCart
    @Test
    void add_one_item_to_shopping_cart() {

        ShoppingCart shoppingCart = new ShoppingCart();
        Item item = new Item("Juice", 34.00, 1);

        shoppingCart.addItem(item);

        assertThat(shoppingCart.getShoppingList()).contains(item);


    }

    @Test
    void remove_an_item_from_shopping_cart() {
        ShoppingCart shoppingCart = new ShoppingCart();

        Item item = new Item("Milk", 19.00, 1);
        shoppingCart.addItem(item);

        shoppingCart.removeItem(item);

        assertThat(shoppingCart.getShoppingList()).isEmpty();
    }

    //Test for calculation totalprice
    @Test
    void get_total_price_for_items_in_shopping_cart() {
        ShoppingCart shoppingCart = new ShoppingCart();

        Stream.of(
                new Item("Juice", 34.00, 1),
                new Item("Milk", 19.00, 2),
                new Item("Sugar", 28.00, 1)
        ).forEach(shoppingCart::addItem);

        assertEquals(100.00, shoppingCart.getTotalPrice(), "Summan av varorna ska bli 100.00kr");
    }

    //Test for applying discounts
    @Test
    void apply_5_percent_discount_on_total_price() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Item item = new Item("Gevalia mörkrost", 80.00, 2);
        shoppingCart.addItem(item);
        shoppingCart.addDiscount(0.05);

        assertEquals(152.00, shoppingCart.getTotalPrice(), "Summan av varorna med rabatt ska bli 152.00kr");

    }
    //EdgeCase for if the discount rate get set to 1.0 and makes the item free
    @Test
    void should_throw_exception_if_invalid_discount_rate(){
        ShoppingCart shoppingCart = new ShoppingCart();

        assertThatThrownBy(()->
                shoppingCart.addDiscount(1.10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ogiltig rabatt.");

    }

}
