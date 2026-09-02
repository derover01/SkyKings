package net.skykings.core.resourcepack;

import net.skykings.core.ui.ResourcePackIcon;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Rein visueller Legacy-1.8-Decorator fuer SkyKings-GUIs.
 *
 * Die Klicklogik der Menues bleibt slot-basiert und unangetastet. Wir ersetzen nur
 * dekorative Item-Materialien durch die im optionalen Resource-Pack reservierten Slots
 * und kopieren sichtbaren Namen/Lore. Ohne Pack bleiben die Fallback-Items weiterhin
 * eindeutig ueber Name und Lore bedienbar.
 */
public final class ResourcePackGuiDecorator {
    private ResourcePackGuiDecorator() {}

    public static void decorate(Inventory inventory, String rawTitle) {
        if (inventory == null || rawTitle == null) return;
        String title = ChatColor.stripColor(rawTitle);
        if (title == null) return;

        if ("SkyKings | Battle Pass".equals(title)) {
            icon(inventory, 10, ResourcePackIcon.BATTLE_PASS);
            icon(inventory, 14, ResourcePackIcon.QUESTS);
            icon(inventory, 16, ResourcePackIcon.QUESTS);
            icon(inventory, 23, ResourcePackIcon.COMPLETED);
            icon(inventory, 25, ResourcePackIcon.COMPLETED);
            icon(inventory, 28, ResourcePackIcon.STAR);
            icon(inventory, 32, ResourcePackIcon.PREMIUM);
            icon(inventory, 34, ResourcePackIcon.PREMIUM);
            return;
        }
        if (title.startsWith("SkyKings | Pass Rewards ")) {
            icon(inventory, 4, ResourcePackIcon.BATTLE_PASS);
            icon(inventory, 27, ResourcePackIcon.PREMIUM);
            icon(inventory, 38, ResourcePackIcon.STAR);
            return;
        }
        if ("SkyKings | Quest Center".equals(title)) {
            icon(inventory, 4, ResourcePackIcon.QUESTS);
            icon(inventory, 16, ResourcePackIcon.PREMIUM);
            icon(inventory, 46, ResourcePackIcon.STAR);
            icon(inventory, 48, ResourcePackIcon.BATTLE_PASS);
            icon(inventory, 53, ResourcePackIcon.BATTLE_PASS);
            return;
        }
        if (title.startsWith("SkyKings | Kit Arsenal")) {
            icon(inventory, 4, ResourcePackIcon.KITS);
            icon(inventory, 47, ResourcePackIcon.READY);
            icon(inventory, 51, ResourcePackIcon.LOCKED);
            return;
        }
        if ("SkyKings | Crate Center".equals(title)) {
            icon(inventory, 4, ResourcePackIcon.CRATES);
            icon(inventory, 42, ResourcePackIcon.SHOP);
            icon(inventory, 44, ResourcePackIcon.CRATES);
            icon(inventory, 49, ResourcePackIcon.COINS);
            return;
        }
        if ("SkyKings | Crate Market".equals(title)) {
            icon(inventory, 4, ResourcePackIcon.SHOP);
            icon(inventory, 45, ResourcePackIcon.BACK);
            icon(inventory, 49, ResourcePackIcon.COINS);
            return;
        }
        if ("SkyKings | Shop | Kategorien".equals(title)) {
            icon(inventory, 4, ResourcePackIcon.SHOP);
            icon(inventory, 12, ResourcePackIcon.CRATES);
            icon(inventory, 49, ResourcePackIcon.COINS);
            return;
        }
        if ("SkyKings | Hilfe".equals(title)) {
            icon(inventory, 12, ResourcePackIcon.SHOP);
            icon(inventory, 16, ResourcePackIcon.BATTLE_PASS);
            icon(inventory, 28, ResourcePackIcon.EVENT);
            icon(inventory, 30, ResourcePackIcon.CLAN);
            icon(inventory, 40, ResourcePackIcon.CRATES);
            return;
        }
        if ("SkyKings | Economy & Handel".equals(title)) {
            icon(inventory, 10, ResourcePackIcon.SHOP);
            icon(inventory, 16, ResourcePackIcon.TRADE);
            icon(inventory, 28, ResourcePackIcon.JACKPOT);
            icon(inventory, 30, ResourcePackIcon.SHOP);
            icon(inventory, 34, ResourcePackIcon.SHOP);
            return;
        }
        if ("SkyKings | Progression".equals(title)) {
            icon(inventory, 10, ResourcePackIcon.KITS);
            icon(inventory, 16, ResourcePackIcon.BATTLE_PASS);
            icon(inventory, 28, ResourcePackIcon.QUESTS);
            icon(inventory, 34, ResourcePackIcon.COINS);
            return;
        }
        if ("SkyKings | Events".equals(title)) {
            icon(inventory, 10, ResourcePackIcon.DUEL);
            icon(inventory, 12, ResourcePackIcon.EVENT);
            icon(inventory, 14, ResourcePackIcon.CLAN);
            icon(inventory, 16, ResourcePackIcon.EVENT);
            icon(inventory, 30, ResourcePackIcon.EVENT);
            return;
        }
        if ("SkyKings | Social".equals(title)) {
            icon(inventory, 11, ResourcePackIcon.CLAN);
            return;
        }
        if ("SkyKings | Crates & Rewards".equals(title)) {
            icon(inventory, 11, ResourcePackIcon.CRATES);
            icon(inventory, 13, ResourcePackIcon.CRATES);
            icon(inventory, 15, ResourcePackIcon.COINS);
            icon(inventory, 29, ResourcePackIcon.JACKPOT);
            icon(inventory, 31, ResourcePackIcon.BATTLE_PASS);
            return;
        }
        if ("SkyKings | Jackpot".equals(title)) {
            icon(inventory, 4, ResourcePackIcon.JACKPOT);
            icon(inventory, 19, ResourcePackIcon.COINS);
            icon(inventory, 20, ResourcePackIcon.COINS);
            icon(inventory, 21, ResourcePackIcon.COINS);
            icon(inventory, 23, ResourcePackIcon.COINS);
            icon(inventory, 24, ResourcePackIcon.COINS);
            icon(inventory, 31, ResourcePackIcon.COINS);
            icon(inventory, 49, ResourcePackIcon.HOME);
            return;
        }
        if ("SkyKings | Trade".equals(title)) {
            icon(inventory, 18, ResourcePackIcon.TRADE);
            icon(inventory, 22, ResourcePackIcon.COINS);
            icon(inventory, 26, ResourcePackIcon.TRADE);
            icon(inventory, 53, ResourcePackIcon.LOCKED);
            return;
        }
        if ("SkyKings | Mein Shop".equals(title)) {
            icon(inventory, 11, ResourcePackIcon.SHOP);
            icon(inventory, 13, ResourcePackIcon.COINS);
            icon(inventory, 15, ResourcePackIcon.LOCKED);
            return;
        }
        if ("SkyKings | Angebote".equals(title)) {
            for (int slot = 18; slot <= 26; slot++) icon(inventory, slot, ResourcePackIcon.COINS);
        }
    }

    static ItemStack decorated(ItemStack source, ResourcePackIcon icon) {
        if (source == null || icon == null) return source;
        ItemStack replacement = new ItemStack(icon.material(), Math.max(1, source.getAmount()));
        ItemMeta oldMeta = source.getItemMeta();
        ItemMeta newMeta = replacement.getItemMeta();
        if (oldMeta != null && newMeta != null) {
            if (oldMeta.hasDisplayName()) newMeta.setDisplayName(oldMeta.getDisplayName());
            if (oldMeta.hasLore()) {
                List<String> lore = oldMeta.getLore();
                if (lore != null) newMeta.setLore(lore);
            }
            replacement.setItemMeta(newMeta);
        }
        return replacement;
    }

    private static void icon(Inventory inventory, int slot, ResourcePackIcon icon) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        ItemStack source = inventory.getItem(slot);
        if (source == null) return;
        inventory.setItem(slot, decorated(source, icon));
    }
}
