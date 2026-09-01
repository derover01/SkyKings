package net.skykings.combat.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Verlustfreier Snapshot fuer temporaere Duel-Loadouts.
 * Inventory, Armor, aktiver Hotbar-Slot, XP und Potion-Effekte werden exakt wiederhergestellt.
 */
final class DuelInventorySnapshot {
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final int heldSlot;
    private final int level;
    private final float exp;
    private final int totalExperience;
    private final List<PotionEffect> effects;

    private DuelInventorySnapshot(ItemStack[] contents, ItemStack[] armor, int heldSlot,
                                  int level, float exp, int totalExperience, List<PotionEffect> effects) {
        this.contents = cloneItems(contents);
        this.armor = cloneItems(armor);
        this.heldSlot = heldSlot;
        this.level = level;
        this.exp = exp;
        this.totalExperience = totalExperience;
        this.effects = new ArrayList<PotionEffect>(effects);
    }

    static DuelInventorySnapshot capture(Player player) {
        return new DuelInventorySnapshot(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getHeldItemSlot(),
                player.getLevel(),
                player.getExp(),
                player.getTotalExperience(),
                new ArrayList<PotionEffect>(player.getActivePotionEffects()));
    }

    void restore(Player player) {
        clearEffects(player);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setContents(cloneItems(contents));
        player.getInventory().setArmorContents(cloneItems(armor));
        player.getInventory().setHeldItemSlot(Math.max(0, Math.min(8, heldSlot)));
        player.setLevel(level);
        player.setExp(exp);
        player.setTotalExperience(totalExperience);
        for (PotionEffect effect : effects) player.addPotionEffect(effect, true);
        player.updateInventory();
    }

    static void clearEffects(Player player) {
        Collection<PotionEffect> active = new ArrayList<PotionEffect>(player.getActivePotionEffects());
        for (PotionEffect effect : active) player.removePotionEffect(effect.getType());
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
        return copy;
    }
}
