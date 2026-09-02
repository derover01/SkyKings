package net.skykings.core.ui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** Kleine zentrale Item-Factory fuer konsistente Menues. */
public final class UiItems {
    private static final int LORE_WIDTH = 32;

    private UiItems() {}

    public static ItemStack item(Material material, String name, String... lore) {
        return item(material, (short) 0, name, lore);
    }

    public static ItemStack item(Material material, short data, String name, String... lore) {
        ItemStack stack = new ItemStack(material, 1, data);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(wrapLore(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack icon(ResourcePackIcon icon, String name, String... lore) {
        if (icon == null) throw new IllegalArgumentException("icon");
        return item(icon.material(), name, lore);
    }

    public static ItemStack head(String playerName, String name, String... lore) {
        ItemStack stack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(name);
        meta.setLore(wrapLore(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Tooltips werden in 1.8 nicht automatisch sinnvoll umgebrochen. Sehr lange Lore-Zeilen
     * koennen deshalb bei kleinen Aufloesungen aus dem Bildschirm laufen. Diese Methode haelt
     * sichtbare Lore-Zeilen kompakt und uebernimmt den letzten Farbcode in die Folgezeile.
     */
    public static List<String> wrapLore(String... lore) {
        List<String> result = new ArrayList<String>();
        if (lore == null) return result;
        for (String raw : lore) {
            if (raw == null || raw.isEmpty()) {
                result.add("");
                continue;
            }
            if (visibleLength(raw) <= LORE_WIDTH) {
                result.add(raw);
                continue;
            }
            String[] words = raw.split(" ");
            String current = "";
            for (String word : words) {
                if (word.isEmpty()) continue;
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (!current.isEmpty() && visibleLength(candidate) > LORE_WIDTH) {
                    result.add(current);
                    String carry = ChatColor.getLastColors(current);
                    current = carry + word;
                } else {
                    current = candidate;
                }
            }
            if (!current.isEmpty()) result.add(current);
        }
        return result;
    }

    private static int visibleLength(String text) {
        String stripped = ChatColor.stripColor(text);
        return stripped == null ? 0 : stripped.length();
    }

    public static ItemStack back() {
        return icon(ResourcePackIcon.BACK, UiTheme.MUTED + "Zurueck", UiTheme.DISABLED + "Vorherige Ansicht");
    }

    public static ItemStack home() {
        return icon(ResourcePackIcon.HOME, UiTheme.PRIMARY + "Home", UiTheme.MUTED + "Zur Hauptansicht");
    }

    public static ItemStack next() {
        return icon(ResourcePackIcon.NEXT, UiTheme.PRIMARY + "Weiter", UiTheme.MUTED + "Naechste Seite");
    }

    public static ItemStack empty(String title, String description) {
        return icon(ResourcePackIcon.LOCKED, UiTheme.MUTED + title, UiTheme.DISABLED + description);
    }

    public static ItemStack status(Material material, String title, boolean ready, String detail) {
        return item(material, (ready ? UiTheme.SUCCESS : UiTheme.DANGER) + title,
                UiTheme.MUTED + detail,
                "",
                ready ? UiTheme.STATUS_READY : UiTheme.STATUS_LOCKED);
    }

    public static String action(String text) {
        return ChatColor.YELLOW + text;
    }
}
