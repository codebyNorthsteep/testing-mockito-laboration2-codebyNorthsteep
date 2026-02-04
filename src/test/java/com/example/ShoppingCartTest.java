package com.example;

import com.example.shop.Item;
import com.example.shop.ShoppingCart;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ShoppingCartTest {
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

    //Handle quiantity - updates
    @Test
    void update_quantity_of_items() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Item item1 = new Item("Yoghurt", 26.00, 1);
        Item item2 = new Item("Yoghurt", 26.00, 2);

        shoppingCart.addItem(item1);
        shoppingCart.addItem(item2);

        //Listan ska bara ha 1 rad med 3 exemplar av youghurt trots två olika inserts
        assertThat(shoppingCart.getShoppingList()).hasSize(1);
        //Efter pit-mutations, kontrollera att kvantiteten faktiskt blir 3
        assertThat(shoppingCart.getShoppingList().getFirst().getQuantity()).isEqualTo(3);
    }


    //--- Edge-Cases ---
    //EdgeCase for if the discount rate gets set to 1.0 and makes the item free
    @Test
    void should_throw_exception_if_invalid_discount_rate() {
        ShoppingCart shoppingCart = new ShoppingCart();

        assertThatThrownBy(() ->
                shoppingCart.addDiscount(1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ogiltig rabatt.");

    }

    //Edge case test for removing an item that is not in the cart
    @Test
    void removing_non_added_item_should_not_effect_cart_and_throw_exception() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Item existingItem = new Item("Pepsi", 18.90, 1);
        Item nonExistentItem = new Item("Coca Cola", 19.90, 1);

        shoppingCart.addItem(existingItem);

        assertThatThrownBy(() ->
                shoppingCart.removeItem(nonExistentItem))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kan ej ta bort ikke-existerande vara, listan lämnas oförändrad.");

        assertThat(shoppingCart.getShoppingList()).containsExactly(existingItem);
    }

    //Edge-case test for negative prices
    @Test
    void should_throw_exception_if_item_has_negative_price() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Item item = new Item("Milk", -10.0, 1);

        assertThatThrownBy(() ->
                shoppingCart.addItem(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pris kan ej vara negativt");
    }

    //Edge-case test for zero quantity
    @Test
    void should_throw_exception_if_item_has_zero_quantity() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Item item = new Item("Oboy", 36.90, 0);

        assertThatThrownBy(() ->
                shoppingCart.addItem(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kvantitet måste vara större än 0");
    }


}
