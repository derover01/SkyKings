package net.skykings.combat.starterkit;

import net.skykings.core.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zentrale, technisch saubere Definition des Death-Starter-Kits (siehe Auftrag Phase 2,
 * Abschnitt 4). STRIKT getrennt von spaeteren Rang-Kits (Phase 3) - jeder Spieler bekommt
 * exakt dasselbe Kit, kein Rang-Abgleich.
 *
 * <p>Armor-Teile sind bewusst separat von den uebrigen Items modelliert, damit sie beim
 * Respawn direkt in die Ruestungs-Slots equippt werden koennen, statt nur ins Inventar gelegt
 * zu werden.
 */
public final class DeathStarterKit {

    private final ItemStack helmet;
    private final ItemStack chestplate;
    private final ItemStack leggings;
    private final ItemStack boots;
    private final List<ItemStack> otherItems;

    public DeathStarterKit(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots,
                            List<ItemStack> otherItems) {
        this.helmet = ItemUtil.safeCopy(helmet);
        this.chestplate = ItemUtil.safeCopy(chestplate);
        this.leggings = ItemUtil.safeCopy(leggings);
        this.boots = ItemUtil.safeCopy(boots);
        this.otherItems = Collections.unmodifiableList(new ArrayList<>(ItemUtil.safeCopyAll(otherItems)));
    }

    public ItemStack getHelmet() {
        return ItemUtil.safeCopy(helmet);
    }

    public ItemStack getChestplate() {
        return ItemUtil.safeCopy(chestplate);
    }

    public ItemStack getLeggings() {
        return ItemUtil.safeCopy(leggings);
    }

    public ItemStack getBoots() {
        return ItemUtil.safeCopy(boots);
    }

    /** Frische, unabhaengige Kopien der uebrigen Items (Schwert, Golden Apples, ...). */
    public List<ItemStack> getOtherItems() {
        return ItemUtil.safeCopyAll(otherItems);
    }
}
