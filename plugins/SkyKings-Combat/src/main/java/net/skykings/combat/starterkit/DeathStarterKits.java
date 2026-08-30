package net.skykings.combat.starterkit;

import net.skykings.core.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Faktory fuer das aktuelle (Phase 2) Death-Starter-Kit: volle Eisenruestung, Eisenschwert und
 * eine konfigurierbare Menge normaler Golden Apples - explizit KEINE Enchanted Golden Apples.
 */
public final class DeathStarterKits {

    private DeathStarterKits() {
    }

    public static DeathStarterKit createDefault(int goldenAppleAmount) {
        if (goldenAppleAmount < 0) {
            throw new IllegalArgumentException("goldenAppleAmount darf nicht negativ sein: " + goldenAppleAmount);
        }
        ItemStack helmet = new ItemBuilder(Material.IRON_HELMET).build();
        ItemStack chestplate = new ItemBuilder(Material.IRON_CHESTPLATE).build();
        ItemStack leggings = new ItemBuilder(Material.IRON_LEGGINGS).build();
        ItemStack boots = new ItemBuilder(Material.IRON_BOOTS).build();
        ItemStack sword = new ItemBuilder(Material.IRON_SWORD).build();
        // durability(0) explizit gesetzt: in 1.8 unterscheidet nur der Data-Wert normalen (0) von
        // Enchanted/Notch-Apple (1) auf demselben Material GOLDEN_APPLE - hier bewusst und
        // dokumentiert erzwungen, damit ein spaeterer Copy-Paste-Fehler nie versehentlich
        // OP-Gapples ausgibt.
        ItemStack goldenApples = new ItemBuilder(Material.GOLDEN_APPLE, goldenAppleAmount)
                .durability((short) 0)
                .build();

        List<ItemStack> otherItems = Arrays.asList(sword, goldenApples);
        return new DeathStarterKit(helmet, chestplate, leggings, boots, otherItems);
    }
}
