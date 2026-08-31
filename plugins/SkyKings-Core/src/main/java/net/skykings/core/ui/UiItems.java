package net.skykings.core.ui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Kleine zentrale Item-Factory fuer konsistente Menues. */
public final class UiItems {
    private UiItems() {}

    public static ItemStack item(Material material, String name, String... lore) {
        return item(material, (short) 0, name, lore);
    }

    public static ItemStack item(Material material, short data, String name, String... lore) {
        ItemStack stack = new ItemStack(material, 1, data);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lines = new ArrayList<String>();
            if (lore != null) lines.addAll(Arrays.asList(lore));
            meta.setLore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack head(String playerName, String name, String... lore) {
        ItemStack stack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack back() {
        return item(Material.ARROW, UiTheme.MUTED + "Zurueck", UiTheme.DISABLED + "Vorherige Ansicht");
    }

    public static ItemStack home() {
        return item(Material.NETHER_STAR, UiTheme.PRIMARY + "Home", UiTheme.MUTED + "Zur Hauptansicht");
    }

    public static ItemStack next() {
        return item(Material.ARROW, UiTheme.PRIMARY + "Weiter", UiTheme.MUTED + "Naechste Seite");
    }

    public static ItemStack empty(String title, String description) {
        return item(Material.BARRIER, UiTheme.MUTED + title, UiTheme.DISABLED + description);
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
