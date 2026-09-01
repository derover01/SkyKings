package net.skykings.combat.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Verlustfreier Snapshot fuer temporaere Duel-Loadouts.
 * Inventory, Armor, aktiver Hotbar-Slot, XP, Potion-Effekte und normale Survival-Werte
 * werden exakt wiederhergestellt. Vitalwerte werden separat restaurierbar gehalten,
 * weil ein toter Spieler im PlayerDeathEvent noch nicht sicher geheilt werden darf.
 */
final class DuelInventorySnapshot {
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final int heldSlot;
    private final int level;
    private final float exp;
    private final int totalExperience;
    private final List<PotionEffect> effects;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int fireTicks;

    private DuelInventorySnapshot(ItemStack[] contents, ItemStack[] armor, int heldSlot,
                                  int level, float exp, int totalExperience, List<PotionEffect> effects,
                                  double health, int foodLevel, float saturation, int fireTicks) {
        this.contents = cloneItems(contents);
        this.armor = cloneItems(armor);
        this.heldSlot = heldSlot;
        this.level = level;
        this.exp = exp;
        this.totalExperience = totalExperience;
        this.effects = new ArrayList<PotionEffect>(effects);
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.fireTicks = fireTicks;
    }

    static DuelInventorySnapshot capture(Player player) {
        return new DuelInventorySnapshot(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getHeldItemSlot(),
                player.getLevel(),
                player.getExp(),
                player.getTotalExperience(),
                new ArrayList<PotionEffect>(player.getActivePotionEffects()),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getFireTicks());
    }

    /** Vollstaendige Wiederherstellung fuer lebende Spieler. */
    void restore(Player player) {
        restoreLoadout(player);
        restoreVitals(player);
    }

    /**
     * Wiederherstellung aller Werte, die bereits waehrend des DeathEvents sicher gesetzt
     * werden koennen. So wird das Originalinventar durch keepInventory in den Respawn getragen.
     */
    void restoreLoadout(Player player) {
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

    /** Survival-Werte erst auf lebenden Spielern bzw. nach dem Respawn anwenden. */
    void restoreVitals(Player player) {
        if (player == null || player.isDead()) return;
        double maxHealth = player.getMaxHealth();
        double restoredHealth = Math.max(0.5D, Math.min(maxHealth, health));
        player.setHealth(restoredHealth);
        player.setFoodLevel(Math.max(0, Math.min(20, foodLevel)));
        player.setSaturation(Math.max(0F, Math.min(20F, saturation)));
        player.setFireTicks(Math.max(0, fireTicks));
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
