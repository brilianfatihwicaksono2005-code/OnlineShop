package view;

import dao.CartDAO;
import dao.OrderDAO;
import dao.ProductDAO;
import model.OrderStatus;
import util.ReportExporter;
import util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerInterface extends JFrame {

    private JTextField searchField;
    private JPanel productsPanel;
    private int currentUserId;
    private String currentCategoryName = null;
    private List<JButton> categoryButtons;
    private JComboBox<String> categoryCombo;
    private boolean syncingCategory = false;

    // Indikator jumlah keranjang
    private JButton viewCartButton;

    public CustomerInterface(int userId) {
        this.currentUserId = userId;
        this.categoryButtons = new ArrayList<>();

        setTitle("Product Catalog");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        root.add(createTopPanel(), BorderLayout.NORTH);
        root.add(createCategoryPanel(), BorderLayout.WEST);

        productsPanel = new JPanel(new GridLayout(0, 4, 18, 18));
        productsPanel.setBackground(UITheme.BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(productsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        root.add(createBottomPanel(), BorderLayout.SOUTH);

        setContentPane(root);

        populateCategoryCombo();
        loadProducts("");
        updateCartIndicator();

        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel titleLabel = UITheme.title("Explore Products");
        JLabel subtitle = UITheme.muted("Temukan produk favorit Anda");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(titleLabel);
        titleBox.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBox.add(subtitle);
        topPanel.add(titleBox, BorderLayout.WEST);

        searchField = UITheme.textField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search for products...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { loadProducts(searchField.getText()); }
            @Override public void removeUpdate(DocumentEvent e)  { loadProducts(searchField.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { loadProducts(searchField.getText()); }
        });

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UITheme.FONT_BASE);
        categoryCombo.setBackground(UITheme.SURFACE);
        categoryCombo.setPreferredSize(new Dimension(170, 46));
        categoryCombo.addActionListener(e -> {
            if (syncingCategory) return;
            Object sel = categoryCombo.getSelectedItem();
            currentCategoryName = (sel == null || "Semua Kategori".equals(sel)) ? null : sel.toString();
            for (JButton btn : categoryButtons) {
                String cat = (String) btn.getClientProperty("categoryName");
                boolean match = (cat == null && currentCategoryName == null)
                        || (cat != null && cat.equals(currentCategoryName));
                if (match) { updateCategoryButtonStyles(btn); break; }
            }
            loadProducts(searchField.getText());
        });

        JPanel searchWrapper = new JPanel(new BorderLayout(10, 0));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchField, BorderLayout.CENTER);
        searchWrapper.add(categoryCombo, BorderLayout.EAST);
        searchWrapper.setPreferredSize(new Dimension(520, 46));
        topPanel.add(searchWrapper, BorderLayout.EAST);

        return topPanel;
    }

    /** Mengisi dropdown kategori dari daftar kategori pada sidebar. */
    private void populateCategoryCombo() {
        syncingCategory = true;
        categoryCombo.removeAllItems();
        categoryCombo.addItem("Semua Kategori");
        for (JButton btn : categoryButtons) {
            String cat = (String) btn.getClientProperty("categoryName");
            if (cat != null) categoryCombo.addItem(cat);
        }
        categoryCombo.setSelectedIndex(0);
        syncingCategory = false;
    }

    /** Menyamakan pilihan dropdown dengan kategori aktif. */
    private void syncCategoryCombo() {
        syncingCategory = true;
        if (currentCategoryName == null) {
            categoryCombo.setSelectedIndex(0);
        } else {
            for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                if (currentCategoryName.equals(categoryCombo.getItemAt(i))) {
                    categoryCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        syncingCategory = false;
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.SURFACE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(18, 16, 18, 16)));
        panel.setPreferredSize(new Dimension(210, 0));

        JLabel titleLabel = UITheme.heading("Categories");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0, 4, 12, 0));
        panel.add(titleLabel);

        JButton allButton = createCategoryButton("All Products", null);
        panel.add(allButton);
        categoryButtons.add(allButton);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        try {
            for (String categoryName : ProductDAO.getCategories()) {
                if (categoryName != null && !categoryName.trim().isEmpty()) {
                    JButton categoryButton = createCategoryButton(categoryName, categoryName);
                    panel.add(categoryButton);
                    categoryButtons.add(categoryButton);
                    panel.add(Box.createRigidArea(new Dimension(0, 4)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateCategoryButtonStyles(allButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JButton createCategoryButton(String displayName, String categoryNameValue) {
        JButton button = new JButton(displayName);
        button.setFont(UITheme.FONT_BOLD);
        button.setForeground(UITheme.TEXT_MUTED);
        button.setBackground(UITheme.SURFACE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");

        button.addActionListener(e -> {
            currentCategoryName = categoryNameValue;
            updateCategoryButtonStyles(button);
            if (categoryCombo != null) syncCategoryCombo();
            loadProducts(searchField.getText());
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boolean isActive = (currentCategoryName == null && categoryNameValue == null) ||
                        (currentCategoryName != null && currentCategoryName.equals(categoryNameValue));
                if (!isActive) {
                    button.setBackground(UITheme.SELECTION);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                boolean isActive = (currentCategoryName == null && categoryNameValue == null) ||
                        (currentCategoryName != null && currentCategoryName.equals(categoryNameValue));
                if (!isActive) {
                    button.setBackground(UITheme.SURFACE);
                }
            }
        });

        button.putClientProperty("categoryName", categoryNameValue);
        return button;
    }

    private void updateCategoryButtonStyles(JButton selectedButton) {
        for (JButton btn : categoryButtons) {
            btn.setBackground(UITheme.SURFACE);
            btn.setForeground(UITheme.TEXT_MUTED);
            btn.setFont(UITheme.FONT_BOLD);
        }
        selectedButton.setBackground(UITheme.SELECTION);
        selectedButton.setForeground(UITheme.PRIMARY);
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomPanel.setOpaque(false);

        JButton historyButton = UITheme.neutralButton("Riwayat Pesanan");
        historyButton.addActionListener(e ->
                SwingUtilities.invokeLater(() -> new OrderHistoryInterface(this, currentUserId)));

        viewCartButton = UITheme.primaryButton("Lihat Keranjang");
        viewCartButton.addActionListener(e -> {
            KeranjangInterface keranjang = new KeranjangInterface(this, currentUserId);
            keranjang.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    updateCartIndicator();
                }
            });
            keranjang.setVisible(true);
        });

        bottomPanel.add(historyButton);
        bottomPanel.add(viewCartButton);
        return bottomPanel;
    }

    private void loadProducts(String keyword) {
        productsPanel.removeAll();
        if ("Search for products...".equals(keyword)) {
            keyword = "";
        }

        try {
            for (Object[] row : ProductDAO.search(keyword, currentCategoryName)) {
                int productId = (Integer) row[0];
                String name = (String) row[1];
                double price = (Double) row[2];
                int stock = (Integer) row[3];
                String imageName = (String) row[4];
                productsPanel.add(createProductCard(productId, name, price, stock, imageName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        productsPanel.revalidate();
        productsPanel.repaint();
    }

    private JPanel createProductCard(int productId, String name, double price, int stock, String imageName) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(UITheme.SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(14, 14, 14, 14)));

        // Gambar
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(180, 150));
        if (imageName != null && !imageName.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon("images/" + imageName);
                Image scaled = icon.getImage().getScaledInstance(180, 150, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                imageLabel.setText("Image not found");
                imageLabel.setForeground(UITheme.TEXT_MUTED);
            }
        } else {
            imageLabel.setText("No Image Available");
            imageLabel.setForeground(UITheme.TEXT_MUTED);
        }
        card.add(imageLabel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(0, 10));
        southPanel.setOpaque(false);

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel("<html><p style='width:150px; text-align:center;'>" + name + "</p></html>");
        nameLabel.setFont(UITheme.FONT_BOLD);
        nameLabel.setForeground(UITheme.TEXT);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("Rp %,.0f", price));
        priceLabel.setFont(UITheme.FONT_BASE);
        priceLabel.setForeground(UITheme.PRIMARY);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(priceLabel);

        JButton quickAddToCartButton = UITheme.primaryButton("Add to Cart");
        quickAddToCartButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (stock <= 0) {
            quickAddToCartButton.setText("Out of Stock");
            quickAddToCartButton.setEnabled(false);
            quickAddToCartButton.setBackground(Color.GRAY);
        } else {
            quickAddToCartButton.addActionListener(e -> addToCart(productId, 1));
        }

        JPanel buttonWrapper = new JPanel();
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(quickAddToCartButton);

        southPanel.add(infoPanel, BorderLayout.CENTER);
        southPanel.add(buttonWrapper, BorderLayout.SOUTH);

        card.add(southPanel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() instanceof JButton) return;
                showProductDetailAndAddToCart(productId);
            }
        });

        return card;
    }

    private void showProductDetailAndAddToCart(int productId) {
        try {
            Object[] product = ProductDAO.getById(productId);
            if (product != null) {
                JDialog dialog = new JDialog(this, "Product Detail", true);
                dialog.setSize(520, 420);
                dialog.setLocationRelativeTo(this);
                dialog.getContentPane().setBackground(UITheme.BACKGROUND);
                dialog.setLayout(new BorderLayout(15, 15));
                dialog.getRootPane().setBorder(new EmptyBorder(20, 20, 20, 20));

                String name = (String) product[0];
                double price = (Double) product[1];
                int stock = (Integer) product[2];
                String imageName = (String) product[3];

                JLabel imageLabel = new JLabel();
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imageLabel.setPreferredSize(new Dimension(180, 150));
                if (imageName != null && !imageName.isEmpty()) {
                    try {
                        ImageIcon icon = new ImageIcon("images/" + imageName);
                        Image scaled = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                        imageLabel.setIcon(new ImageIcon(scaled));
                    } catch (Exception e) {
                        imageLabel.setText("No Image");
                    }
                } else {
                    imageLabel.setText("No Image");
                }

                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setOpaque(false);

                JLabel nameLabel = new JLabel("<html><p style='width:200px; text-align:left;'>" + name + "</p></html>");
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                nameLabel.setForeground(UITheme.TEXT);
                nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel priceLabel = new JLabel(String.format("Rp %,.0f", price));
                priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                priceLabel.setForeground(UITheme.PRIMARY);
                priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel stockLabel = new JLabel("Stock: " + stock);
                stockLabel.setFont(UITheme.FONT_BASE);
                stockLabel.setForeground(UITheme.TEXT_MUTED);
                stockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                infoPanel.add(nameLabel);
                infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                infoPanel.add(priceLabel);
                infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                infoPanel.add(stockLabel);

                JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, stock > 0 ? stock : 1, 1));
                qtySpinner.setFont(UITheme.FONT_BASE);
                qtySpinner.setPreferredSize(new Dimension(80, 32));
                qtySpinner.setMaximumSize(new Dimension(80, 32));

                JButton addToCartBtn = UITheme.primaryButton("Add to Cart");
                if (stock <= 0) {
                    qtySpinner.setEnabled(false);
                    addToCartBtn.setEnabled(false);
                    addToCartBtn.setText("Out of Stock");
                    addToCartBtn.setBackground(Color.GRAY);
                }

                addToCartBtn.addActionListener(e -> {
                    int quantity = (int) qtySpinner.getValue();
                    addToCart(productId, quantity);
                    dialog.dispose();
                });

                JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
                quantityPanel.setOpaque(false);
                JLabel qtyLabel = new JLabel("Quantity:");
                qtyLabel.setFont(UITheme.FONT_BOLD);
                qtyLabel.setForeground(UITheme.TEXT);
                quantityPanel.add(qtyLabel);
                quantityPanel.add(qtySpinner);

                JPanel actionPanel = new JPanel(new BorderLayout(0, 10));
                actionPanel.setOpaque(false);
                actionPanel.add(quantityPanel, BorderLayout.WEST);
                actionPanel.add(addToCartBtn, BorderLayout.EAST);

                JPanel detailsPanel = new JPanel(new BorderLayout(14, 10));
                detailsPanel.setOpaque(false);
                detailsPanel.add(imageLabel, BorderLayout.WEST);
                detailsPanel.add(infoPanel, BorderLayout.CENTER);

                dialog.add(detailsPanel, BorderLayout.CENTER);
                dialog.add(actionPanel, BorderLayout.SOUTH);

                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Product not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load product detail: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void addToCart(int productId, int quantity) {
        try {
            CartDAO.addToCart(currentUserId, productId, quantity);
            JOptionPane.showMessageDialog(this, "Product successfully added to cart.");
            updateCartIndicator();
        } catch (SQLException e) {
            e.printStackTrace();
            if ("45000".equals(e.getSQLState())) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Stock Error", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add to cart: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void updateCartIndicator() {
        try {
            int itemCount = CartDAO.countDistinctItems(currentUserId);
            if (itemCount > 0) {
                viewCartButton.setText(String.format("Lihat Keranjang (%d)", itemCount));
            } else {
                viewCartButton.setText("Lihat Keranjang");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            viewCartButton.setText("Lihat Keranjang");
        }
    }

    static class KeranjangInterface extends JFrame {
        private int userId;
        private JPanel cartItemsPanel;
        private JLabel totalLabel;
        private double totalAmount = 0.0;
        private CustomerInterface parentFrame;

        private JButton checkoutButton;
        private JButton clearCartButton;

        public KeranjangInterface(CustomerInterface parent, int userId) {
            this.parentFrame = parent;
            this.userId = userId;

            setTitle("Shopping Cart - User ID: " + userId);
            setSize(860, 640);
            setMinimumSize(new Dimension(720, 520));
            setLocationRelativeTo(parentFrame);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
            mainPanel.setBackground(UITheme.BACKGROUND);
            mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel titleLabel = UITheme.title("Your Shopping Cart");
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            cartItemsPanel = new JPanel();
            cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));
            cartItemsPanel.setBackground(UITheme.SURFACE);
            JScrollPane scrollPane = new JScrollPane(cartItemsPanel);
            scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1, true));
            scrollPane.getViewport().setBackground(UITheme.SURFACE);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout(0, 12));
            bottomPanel.setOpaque(false);

            totalLabel = new JLabel("Total: Rp 0", SwingConstants.RIGHT);
            totalLabel.setFont(UITheme.FONT_H2);
            totalLabel.setForeground(UITheme.TEXT);
            bottomPanel.add(totalLabel, BorderLayout.NORTH);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            buttonPanel.setOpaque(false);

            clearCartButton = UITheme.dangerButton("Clear Cart");
            clearCartButton.addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear your cart?", "Clear Cart", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    clearCart();
                    parentFrame.updateCartIndicator();
                }
            });
            buttonPanel.add(clearCartButton);

            checkoutButton = UITheme.primaryButton("Checkout");
            checkoutButton.addActionListener(e -> {
                if (totalAmount > 0) {
                    processCheckout();
                    parentFrame.updateCartIndicator();
                } else {
                    JOptionPane.showMessageDialog(this, "Your cart is empty!", "Cart Empty", JOptionPane.WARNING_MESSAGE);
                }
            });
            buttonPanel.add(checkoutButton);

            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
            mainPanel.add(bottomPanel, BorderLayout.SOUTH);

            setContentPane(mainPanel);

            loadCartItems();
            updateTotal();
        }

        private void loadCartItems() {
            cartItemsPanel.removeAll();
            totalAmount = 0.0;
            try {
                List<Object[]> rows = CartDAO.getCartItems(userId);
                if (rows.isEmpty()) {
                    JLabel emptyCartLabel = new JLabel("Your cart is empty.", SwingConstants.CENTER);
                    emptyCartLabel.setFont(UITheme.FONT_H3);
                    emptyCartLabel.setForeground(UITheme.TEXT_MUTED);
                    cartItemsPanel.add(Box.createVerticalGlue());
                    cartItemsPanel.add(emptyCartLabel);
                    cartItemsPanel.add(Box.createVerticalGlue());
                    checkoutButton.setEnabled(false);
                    clearCartButton.setEnabled(false);
                } else {
                    checkoutButton.setEnabled(true);
                    clearCartButton.setEnabled(true);
                    for (Object[] row : rows) {
                        int cartId = (Integer) row[0];
                        int productId = (Integer) row[1];
                        String name = (String) row[2];
                        int quantity = (Integer) row[3];
                        double price = (Double) row[4];
                        int stock = (Integer) row[5];
                        String imageName = (String) row[6];

                        totalAmount += (quantity * price);
                        cartItemsPanel.add(createCartItemCard(cartId, productId, name, quantity, price, stock, imageName));
                        cartItemsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load cart items: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            cartItemsPanel.revalidate();
            cartItemsPanel.repaint();
            updateTotal();
        }

        private JPanel createCartItemCard(int cartId, int productId, String name, int quantity, double price, int stock, String imageName) {
            JPanel card = new JPanel(new BorderLayout(12, 0));
            card.setBackground(UITheme.SURFACE);
            card.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.BORDER, 1, true),
                    new EmptyBorder(12, 12, 12, 12)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));

            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(80, 80));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            if (imageName != null && !imageName.isEmpty()) {
                try {
                    ImageIcon icon = new ImageIcon("images/" + imageName);
                    Image scaled = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaled));
                } catch (Exception e) {
                    imageLabel.setText("No Image");
                    imageLabel.setForeground(UITheme.TEXT_MUTED);
                }
            } else {
                imageLabel.setText("No Image");
                imageLabel.setForeground(UITheme.TEXT_MUTED);
            }
            card.add(imageLabel, BorderLayout.WEST);

            JPanel infoPanel = new JPanel(new GridLayout(3, 1));
            infoPanel.setOpaque(false);
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(UITheme.FONT_BOLD);
            nameLabel.setForeground(UITheme.TEXT);
            JLabel priceLabel = new JLabel(String.format("Rp %,.0f / item", price));
            priceLabel.setFont(UITheme.FONT_BASE);
            priceLabel.setForeground(UITheme.TEXT_MUTED);
            JLabel stockLabel = new JLabel("Available Stock: " + stock);
            stockLabel.setFont(UITheme.FONT_SMALL);
            stockLabel.setForeground(UITheme.TEXT_MUTED);
            infoPanel.add(nameLabel);
            infoPanel.add(priceLabel);
            infoPanel.add(stockLabel);
            card.add(infoPanel, BorderLayout.CENTER);

            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            controlPanel.setOpaque(false);

            JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(quantity, 1, stock > 0 ? stock : 1, 1));
            qtySpinner.setFont(UITheme.FONT_BASE);
            qtySpinner.setPreferredSize(new Dimension(64, 32));
            if (stock <= 0) {
                qtySpinner.setEnabled(false);
            }
            qtySpinner.addChangeListener(e -> {
                int newQuantity = (int) qtySpinner.getValue();
                updateCartItemQuantity(cartId, newQuantity);
            });
            controlPanel.add(qtySpinner);

            JButton removeButton = UITheme.dangerButton("Remove");
            removeButton.setBorder(new EmptyBorder(8, 14, 8, 14));
            removeButton.addActionListener(e -> removeCartItem(cartId));
            controlPanel.add(removeButton);

            card.add(controlPanel, BorderLayout.EAST);

            return card;
        }

        private void updateCartItemQuantity(int cartId, int newQuantity) {
            try {
                CartDAO.updateQuantity(cartId, newQuantity);
                loadCartItems();
            } catch (SQLException e) {
                e.printStackTrace();
                if ("45000".equals(e.getSQLState())) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Stock Error", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update quantity: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                loadCartItems();
            }
        }

        private void removeCartItem(int cartId) {
            try {
                CartDAO.removeItem(cartId);
                JOptionPane.showMessageDialog(this, "Item removed from cart.");
                loadCartItems();
                parentFrame.updateCartIndicator();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to remove item: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void clearCart() {
            try {
                CartDAO.clearCart(userId);
                JOptionPane.showMessageDialog(this, "Cart cleared successfully!");
                loadCartItems();
                parentFrame.updateCartIndicator();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to clear cart: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateTotal() {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            totalLabel.setText("Total: " + currencyFormat.format(totalAmount));
        }

        private void processCheckout() {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Total amount: " + NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(totalAmount) + "\nProceed to checkout?",
                    "Confirm Checkout", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    int orderId = OrderDAO.checkout(userId);

                    JOptionPane.showMessageDialog(this, "Checkout successful! Your order ID is: " + orderId, "Success", JOptionPane.INFORMATION_MESSAGE);

                    loadCartItems();
                    parentFrame.updateCartIndicator();

                    int saveReceipt = JOptionPane.showConfirmDialog(this,
                            "Unduh struk/invoice pesanan ini sebagai PDF?",
                            "Cetak Struk", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (saveReceipt == JOptionPane.YES_OPTION) {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setSelectedFile(new File("invoice-" + orderId + ".pdf"));
                        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                            File dest = OrderManagementPanel.ensurePdfExtension(chooser.getSelectedFile());
                            try {
                                ReportExporter.exportInvoice(orderId, dest);
                                JOptionPane.showMessageDialog(this, "Struk tersimpan: " + dest.getAbsolutePath());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(this, "Gagal menyimpan struk: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Checkout failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    static class OrderHistoryInterface extends JFrame {
        private int userId;
        private JTable ordersTable;
        private DefaultTableModel tableModel;
        private JButton clearHistoryButton;

        public OrderHistoryInterface(Frame parent, int userId) {
            this.userId = userId;
            setTitle("Riwayat Pesanan - User ID: " + userId);
            setSize(880, 580);
            setMinimumSize(new Dimension(720, 480));
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
            mainPanel.setBackground(UITheme.BACKGROUND);
            mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel titleLabel = UITheme.title("Riwayat Pesanan Anda");
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            String[] columnNames = {"ID Pesanan", "Tanggal", "Total", "Status"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            ordersTable = new JTable(tableModel);
            UITheme.styleTable(ordersTable);
            ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            ordersTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int selectedRow = ordersTable.getSelectedRow();
                        if (selectedRow >= 0) {
                            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
                            new OrderDetailDialog(OrderHistoryInterface.this, orderId);
                        }
                    }
                }
            });

            JPanel tableCard = UITheme.card(new BorderLayout());
            tableCard.add(UITheme.scrollPane(ordersTable), BorderLayout.CENTER);
            mainPanel.add(tableCard, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
            bottomPanel.setOpaque(false);

            JLabel hintLabel = new JLabel("Klik dua kali pada pesanan untuk melihat detail.");
            hintLabel.setFont(UITheme.FONT_SMALL);
            hintLabel.setForeground(UITheme.TEXT_MUTED);
            bottomPanel.add(hintLabel, BorderLayout.WEST);

            clearHistoryButton = UITheme.dangerButton("Clear Order History");
            clearHistoryButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete ALL your order history? This action cannot be undone.",
                        "Confirm Clear History", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    clearOrderHistory();
                }
            });
            JPanel clearWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            clearWrapper.setOpaque(false);

            JButton invoiceButton = UITheme.neutralButton("Cetak Struk");
            invoiceButton.addActionListener(e -> {
                int row = ordersTable.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Pilih pesanan terlebih dahulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                int orderId = (int) ordersTable.getValueAt(row, 0);
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File("invoice-" + orderId + ".pdf"));
                if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
                File dest = OrderManagementPanel.ensurePdfExtension(chooser.getSelectedFile());
                try {
                    ReportExporter.exportInvoice(orderId, dest);
                    JOptionPane.showMessageDialog(this, "Struk tersimpan: " + dest.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Gagal menyimpan struk: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            clearWrapper.add(invoiceButton);
            clearWrapper.add(clearHistoryButton);
            bottomPanel.add(clearWrapper, BorderLayout.EAST);

            mainPanel.add(bottomPanel, BorderLayout.SOUTH);

            setContentPane(mainPanel);
            loadOrders();
            setVisible(true);
        }

        private void loadOrders() {
            try {
                List<Object[]> rows = OrderDAO.getOrdersByUser(userId);
                tableModel.setRowCount(0);

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm");
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

                clearHistoryButton.setEnabled(!rows.isEmpty());
                for (Object[] r : rows) {
                    int orderId = (Integer) r[0];
                    Timestamp orderDate = (Timestamp) r[1];
                    double totalAmount = (Double) r[2];
                    OrderStatus status = OrderStatus.fromDb((String) r[3]);

                    tableModel.addRow(new Object[]{
                            orderId,
                            dateFormat.format(orderDate),
                            currencyFormat.format(totalAmount),
                            status.getLabel()
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Gagal memuat riwayat pesanan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void clearOrderHistory() {
            try {
                OrderDAO.deleteOrderHistory(userId);
                JOptionPane.showMessageDialog(this, "Order history cleared successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadOrders();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to clear order history: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class OrderDetailDialog extends JDialog {
        private int orderId;
        private DefaultTableModel tableModel;

        public OrderDetailDialog(JFrame parent, int orderId) {
            super(parent, "Detail Pesanan #" + orderId, true);
            this.orderId = orderId;

            setSize(640, 460);
            setLocationRelativeTo(parent);
            getContentPane().setBackground(UITheme.BACKGROUND);
            setLayout(new BorderLayout());
            getRootPane().setBorder(new EmptyBorder(20, 20, 20, 20));

            String[] columnNames = {"Nama Produk", "Jumlah", "Harga Satuan", "Subtotal"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            JTable itemsTable = new JTable(tableModel);
            UITheme.styleTable(itemsTable);

            add(UITheme.scrollPane(itemsTable), BorderLayout.CENTER);
            loadOrderDetails();
            setVisible(true);
        }

        private void loadOrderDetails() {
            try {
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                for (Object[] r : OrderDAO.getOrderItems(orderId)) {
                    tableModel.addRow(new Object[]{
                            r[0],
                            r[1],
                            currencyFormat.format((Double) r[2]),
                            currencyFormat.format((Double) r[3])
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Gagal memuat detail pesanan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        UITheme.install();
        SwingUtilities.invokeLater(() -> new CustomerInterface(2));
    }
}
