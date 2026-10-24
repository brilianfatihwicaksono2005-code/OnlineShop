package model;

import java.text.NumberFormat;
import java.util.Locale;

public class CartItem {
    // ANSI Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String BOLD = "\u001B[1m";

    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }

    public String getFormattedSubtotal() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return GREEN + BOLD + currencyFormat.format(getSubtotal()) + RESET;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s %sx%d%s = %s",
                CYAN + BOLD, product.getName(), RESET,
                YELLOW, quantity, RESET,
                getFormattedSubtotal());
    }
}