package net.skykings.core.item;

import net.skykings.core.ui.UiItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Gemeinsamer ItemStack-Builder fuer Spigot 1.8.8 (siehe Auftrag Phase 1B, "Item Utility").
 *
 * <p>Bewusst nur 1.8.8-kompatible API: {@code ItemStack#setDurability(short)} statt einer
 * moderneren Material-/Damage-API, {@link Enchantment#getByName(String)}-kompatible Enchantment-
 * Konstanten und {@link ItemFlag} (seit Bukkit 1.8 verfuegbar). Farbcodes werden mit {@code &}
 * geschrieben und ueber {@link ChatColor#translateAlternateColorCodes} uebersetzt.
 *
 * <p>{@link #build()} liefert immer eine neue, unabhaengige Kopie - der Builder selbst bleibt
 * wiederverwendbar und darf nach {@code build()} weiter veraendert werden, ohne bereits gebaute
 * ItemStacks zu beeinflussen.
 */
public final class ItemBuilder {

    private final ItemStack stack;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.stack = new ItemStack(Objects.requireNonNull(material, "material"), amount);
    }

    /** Startet von einer sicheren Kopie eines bestehenden ItemStacks. */
    public ItemBuilder(ItemStack base) {
        this.stack = Objects.requireNonNull(base, "base").clone();
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    /** Legacy 1.8-Data-/Durability-Wert (z. B. Wollfarbe, Werkzeug-Abnutzung). */
    public ItemBuilder durability(short durability) {
        stack.setDurability(durability);
        return this;
    }

    public ItemBuilder name(String displayName) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateColors(displayName));
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> translated = new ArrayList<String>(lines.size());
            for (String line : lines) translated.add(translateColors(line));
            meta.setLore(UiItems.wrapLore(translated.toArray(new String[translated.size()])));
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        stack.addUnsafeEnchantment(Objects.requireNonNull(enchantment, "enchantment"), level);
        return this;
    }

    public ItemBuilder flag(ItemFlag... flags) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            stack.setItemMeta(meta);
        }
        return this;
    }

    /** Liefert eine neue, unabhaengige Kopie des aktuellen Zustands. */
    public ItemStack build() {
        return stack.clone();
    }

    private static String translateColors(String raw) {
        return raw == null ? null : ChatColor.translateAlternateColorCodes('&', raw);
    }
}
