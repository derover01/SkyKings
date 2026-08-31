package net.skykings.core.ui;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Einheitliche Zahlen-, Prozent- und Progress-Bar-Formatierung. */
public final class UiFormat {
    private static final DecimalFormat INTEGER;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        INTEGER = new DecimalFormat("#,##0", symbols);
    }

    private UiFormat() {}

    public static synchronized String number(long value) {
        return INTEGER.format(value);
    }

    public static String coins(long value) {
        return number(value) + " Coins";
    }

    public static String percent(double value) {
        int pct = (int) Math.round(Math.max(0D, Math.min(1D, value)) * 100D);
        return pct + "%";
    }

    public static String progress(long current, long max, int segments) {
        if (segments < 1) segments = 10;
        double ratio = max <= 0 ? 0D : Math.max(0D, Math.min(1D, (double) current / (double) max));
        int filled = (int) Math.round(ratio * segments);
        StringBuilder out = new StringBuilder();
        out.append(ChatColor.AQUA);
        for (int i = 0; i < filled; i++) out.append('█');
        out.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < segments; i++) out.append('░');
        out.append(ChatColor.GRAY).append(' ').append(percent(ratio));
        return out.toString();
    }

    public static String durationSeconds(long seconds) {
        long safe = Math.max(0L, seconds);
        long minutes = safe / 60L;
        long rest = safe % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, rest);
    }
}
