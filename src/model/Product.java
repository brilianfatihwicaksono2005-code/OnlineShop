package model;

import java.text.NumberFormat;
import java.util.Locale;

public class Product {
    private int id;
    private String name;
    private double price;
    private int stock;
    private String category;

    public Product(int id, String name, double price, int stock, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getCategory() { return category; }

    public void setStock(int stock) { this.stock = stock; }
    public void setPrice(double price) { this.price = price; }

    public String getFormattedPrice() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return currencyFormat.format(price);
    }

    @Override
    public String toString() {
        return String.format("%s - %s (Stock: %d)", name, getFormattedPrice(), stock);
    }
}
