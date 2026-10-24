package model;

import java.util.EnumMap;
import java.util.Map;

/**
 * OrderStatus - representasi state machine untuk siklus hidup pesanan.
 *
 * Alur normal: PENDING -> DIPROSES -> DIKIRIM -> SELESAI
 * PENDING juga dapat dibatalkan (CANCELLED). Status SELESAI/CANCELLED adalah terminal.
 */
public enum OrderStatus {
    PENDING("pending", "Pending"),
    DIPROSES("diproses", "Diproses"),
    DIKIRIM("dikirim", "Dikirim"),
    SELESAI("selesai", "Selesai"),
    CANCELLED("cancelled", "Dibatalkan");

    private final String dbValue;
    private final String label;

    OrderStatus(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() { return dbValue; }
    public String getLabel() { return label; }

    private static final Map<OrderStatus, OrderStatus> NEXT = new EnumMap<>(OrderStatus.class);
    static {
        NEXT.put(PENDING, DIPROSES);
        NEXT.put(DIPROSES, DIKIRIM);
        NEXT.put(DIKIRIM, SELESAI);
    }

    /** Status berikutnya pada alur normal, atau null jika sudah terminal. */
    public OrderStatus next() { return NEXT.get(this); }

    public boolean isTerminal() { return this == SELESAI || this == CANCELLED; }

    /** Validasi transisi sesuai state machine. */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) return false;
        OrderStatus next = NEXT.get(this);
        if (target == next) return true;
        return this == PENDING && target == CANCELLED;
    }

    public static OrderStatus fromDb(String value) {
        if (value != null) {
            for (OrderStatus s : values()) {
                if (s.dbValue.equalsIgnoreCase(value)) return s;
            }
        }
        return PENDING;
    }

    @Override
    public String toString() { return label; }
}