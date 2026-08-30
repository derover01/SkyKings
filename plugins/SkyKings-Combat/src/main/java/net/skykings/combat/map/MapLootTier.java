package net.skykings.combat.map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public enum MapLootTier {
    COMMON("Common", ChatColor.GREEN, 3 * 60L),
    RARE("Rare", ChatColor.AQUA, 10 * 60L),
    EPIC("Epic", ChatColor.LIGHT_PURPLE, 30 * 60L),
    SUPPLY("Supply Drop", ChatColor.GOLD, 45 * 60L);

    private final String display;
    private final ChatColor color;
    private final long cooldownSeconds;

    MapLootTier(String display, ChatColor color, long cooldownSeconds) {
        this.display = display;
        this.color = color;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getDisplay() { return display; }
    public ChatColor getColor() { return color; }
    public long getCooldownMillis() { return cooldownSeconds * 1000L; }

    public static MapLootTier parse(String raw) {
        if (raw == null) return null;
        for (MapLootTier tier : values()) {
            if (tier.name().equalsIgnoreCase(raw) || tier.display.equalsIgnoreCase(raw)) return tier;
        }
        return null;
    }

    public List<ItemStack> rollLoot(Random random) {
        int rolls;
        switch (this) {
            case COMMON: rolls = 3 + random.nextInt(2); break;
            case RARE: rolls = 4 + random.nextInt(2); break;
            case EPIC: rolls = 5 + random.nextInt(2); break;
            default: rolls = 7 + random.nextInt(2); break;
        }
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (int i = 0; i < rolls; i++) out.add(rollOne(random));
        return out;
    }

    private ItemStack rollOne(Random random) {
        int r = random.nextInt(100);
        if (this == COMMON) {
            if (r < 30) return named(Material.ENDER_PEARL, 4 + random.nextInt(5), ChatColor.AQUA + "Map-Loot");
            if (r < 55) return named(Material.EXP_BOTTLE, 16 + random.nextInt(17), ChatColor.GREEN + "Map-Loot");
            if (r < 75) return named(Material.ARROW, 16 + random.nextInt(17), ChatColor.GRAY + "Map-Loot");
            if (r < 90) return named(Material.GOLDEN_APPLE, 2 + random.nextInt(3), ChatColor.GOLD + "Map-Loot");
            return named(Material.NETHER_STAR, 1, ChatColor.DARK_AQUA + "PvP-Währung");
        }
        if (this == RARE) {
            if (r < 25) return named(Material.ENDER_PEARL, 8 + random.nextInt(9), ChatColor.AQUA + "Rare Loot");
            if (r < 50) return named(Material.GOLDEN_APPLE, 4 + random.nextInt(5), ChatColor.GOLD + "Rare Loot");
            if (r < 70) return named(Material.EXP_BOTTLE, 32 + random.nextInt(33), ChatColor.GREEN + "Rare Loot");
            if (r < 88) return named(Material.NETHER_STAR, 2 + random.nextInt(3), ChatColor.DARK_AQUA + "PvP-Währung");
            return named(Material.DIAMOND, 2 + random.nextInt(3), ChatColor.AQUA + "Rare Loot");
        }
        if (this == EPIC) {
            if (r < 22) return named(Material.ENDER_PEARL, 16, ChatColor.AQUA + "Epic Loot");
            if (r < 45) return named(Material.GOLDEN_APPLE, 8 + random.nextInt(9), ChatColor.GOLD + "Epic Loot");
            if (r < 67) return named(Material.NETHER_STAR, 4 + random.nextInt(5), ChatColor.DARK_AQUA + "PvP-Währung");
            if (r < 85) return named(Material.EXP_BOTTLE, 64, ChatColor.GREEN + "Epic Loot");
            return named(Material.DIAMOND, 4 + random.nextInt(5), ChatColor.LIGHT_PURPLE + "Epic Loot");
        }
        if (r < 20) return named(Material.ENDER_PEARL, 16 + random.nextInt(17), ChatColor.AQUA + "Supply Drop");
        if (r < 40) return named(Material.GOLDEN_APPLE, 16 + random.nextInt(17), ChatColor.GOLD + "Supply Drop");
        if (r < 60) return named(Material.NETHER_STAR, 8 + random.nextInt(9), ChatColor.DARK_AQUA + "Supply Drop");
        if (r < 80) return named(Material.EXP_BOTTLE, 64, ChatColor.GREEN + "Supply Drop");
        return named(Material.DIAMOND, 8 + random.nextInt(9), ChatColor.LIGHT_PURPLE + "Supply Drop");
    }

    private ItemStack named(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
