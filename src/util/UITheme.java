package util;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UITheme - sumber tunggal untuk tema "Modern SaaS" (Light Mode).
 *
 * Berisi palet warna, font, dan pabrik komponen (tombol, text field, tabel, card)
 * sehingga seluruh halaman memiliki tampilan yang seragam dan kode tidak berulang.
 */
public final class UITheme {

    // =====================================================
    // PALET WARNA
    // =====================================================
    public static final Color BACKGROUND   = new Color(0xF5F7FB); // background utama aplikasi
    public static final Color SURFACE      = Color.WHITE;         // card / panel konten
    public static final Color BORDER       = new Color(0xE4E8F0); // garis halus

    public static final Color PRIMARY       = new Color(0x4F46E5); // indigo - aksi utama
    public static final Color PRIMARY_DARK  = new Color(0x4338CA);

    public static final Color SUCCESS       = new Color(0x16A34A); // hijau - simpan/checkout
    public static final Color SUCCESS_DARK  = new Color(0x15803D);

    public static final Color DANGER        = new Color(0xDC2626); // merah - hapus
    public static final Color DANGER_DARK   = new Color(0xB91C1C);

    public static final Color WARNING       = new Color(0xF59E0B); // kuning - upload
    public static final Color WARNING_DARK  = new Color(0xD97706);

    public static final Color NEUTRAL       = new Color(0x64748B); // slate - sekunder
    public static final Color NEUTRAL_DARK  = new Color(0x475569);

    public static final Color TEXT          = new Color(0x1E293B);
    public static final Color TEXT_MUTED    = new Color(0x64748B);

    public static final Color TABLE_HEADER  = new Color(0x1E293B);
    public static final Color TABLE_ALT     = new Color(0xF1F5F9);
    public static final Color SELECTION     = new Color(0xE0E7FF);

    // =====================================================
    // FONT
    // =====================================================
    private static final String FAMILY = "Segoe UI";
    public static final Font FONT_BASE  = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BOLD  = new Font(FAMILY, Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font FONT_H1    = new Font(FAMILY, Font.BOLD, 26);
    public static final Font FONT_H2    = new Font(FAMILY, Font.BOLD, 20);
    public static final Font FONT_H3    = new Font(FAMILY, Font.BOLD, 16);

    private UITheme() { }

    // =====================================================
    // INSTALASI TEMA GLOBAL
    // =====================================================
    public static void install() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
            // jika FlatLaf tidak tersedia, tetap gunakan L&F aktif
        }

        UIManager.put("defaultFont", FONT_BASE);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.alternateRowColor", TABLE_ALT);
    }

    // =====================================================
    // TOMBOL
    // =====================================================
    public static JButton button(String text, Color bg, Color hover) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        addHover(b, bg, hover);
        return b;
    }

    public static JButton primaryButton(String text) { return button(text, PRIMARY, PRIMARY_DARK); }
    public static JButton successButton(String text) { return button(text, SUCCESS, SUCCESS_DARK); }
    public static JButton dangerButton(String text)  { return button(text, DANGER, DANGER_DARK); }
    public static JButton warningButton(String text) { return button(text, WARNING, WARNING_DARK); }
    public static JButton neutralButton(String text) { return button(text, NEUTRAL, NEUTRAL_DARK); }

    /** Menambahkan efek hover: warna berubah saat kursor masuk, kembali normal saat keluar. */
    public static void addHover(final JButton b, final Color normal, final Color hover) {
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(normal); }
        });
    }

    // =====================================================
    // INPUT TEXT
    // =====================================================
    public static JTextField textField(int columns) {
        JTextField f = new JTextField(columns);
        styleTextField(f);
        return f;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField f = new JPasswordField(columns);
        styleTextField(f);
        return f;
    }

    /** Padding dalam + border membulat halus agar teks tidak menempel ke tepi. */
    public static void styleTextField(JTextField f) {
        f.setFont(FONT_BASE);
        f.setForeground(TEXT);
        f.setBackground(SURFACE);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
    }

    public static JTextArea textArea(int rows, int columns) {
        JTextArea a = new JTextArea(rows, columns);
        a.setFont(FONT_BASE);
        a.setForeground(TEXT);
        a.setBackground(SURFACE);
        a.setCaretColor(TEXT);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(8, 12, 8, 12));
        return a;
    }

    // =====================================================
    // SCROLLPANE
    // =====================================================
    public static JScrollPane scrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.setBackground(SURFACE);
        sp.getViewport().setBackground(SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // =====================================================
    // TABEL
    // =====================================================
    public static void styleTable(JTable table) {
        table.setFont(FONT_BASE);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(TABLE_HEADER);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        TableCellRenderer body = new BodyRenderer();
        Class<?>[] classes = {
                Object.class, String.class, Integer.class, Long.class, Double.class,
                Float.class, Number.class, Boolean.class,
                java.util.Date.class, java.sql.Date.class, java.sql.Timestamp.class
        };
        for (Class<?> c : classes) {
            table.setDefaultRenderer(c, body);
        }
    }

    private static class BodyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                setBackground(SELECTION);
            } else {
                setBackground(row % 2 == 0 ? SURFACE : TABLE_ALT);
            }
            setForeground(TEXT);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(TABLE_HEADER);
            setForeground(Color.WHITE);
            setFont(FONT_BOLD);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    // =====================================================
    // CARD & LABEL
    // =====================================================
    /** Panel "card" putih dengan border halus dan padding luas. */
    public static JPanel card(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)));
        return p;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H1);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H2);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    public static JLabel centeredTitle(String text) {
        JLabel l = title(text);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** Memusatkan sebuah komponen di tengah panel pembungkus. */
    public static JPanel center(Component c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }
}
