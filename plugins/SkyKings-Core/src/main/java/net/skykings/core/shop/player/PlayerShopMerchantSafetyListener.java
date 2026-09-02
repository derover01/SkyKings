package net.skykings.core.shop.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Zusaetzlicher Schutz fuer das echte 1.8-Villager-Handelsfenster der PlayerShops.
 *
 * Der eigentliche Kauf wird im PlayerShopTradeController verarbeitet. Dieser Listener
 * verhindert insbesondere, dass Shift-Klicks oder Drag-Aktionen aus dem unteren
 * Spielerinventar Vanilla-seitig Items in die virtuellen Merchant-Input-Slots schieben.
 */
final class PlayerShopMerchantSafetyListener implements Listener {
    private static final String TOKEN_NAME = ChatColor.GOLD + "SkyKings Coins";

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView() == null || !isSkyKingsMerchant(event.getView().getTopInventory())) return;

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        // Die drei Merchant-Slots gehoeren vollstaendig SkyKings. Der Hauptcontroller
        // wertet Slot 2 als Kauf aus; Vanilla darf keinen dieser Slots mutieren.
        if (rawSlot >= 0 && rawSlot < topSize) {
            event.setCancelled(true);
            return;
        }

        // Normales Sortieren/Klicken im eigenen Inventar bleibt erlaubt. Nur Shift-Moves
        // koennten automatisch in das Merchant-Inventar wandern und werden blockiert.
        if (event.isShiftClick()) event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView() == null || !isSkyKingsMerchant(event.getView().getTopInventory())) return;
        int topSize = event.getView().getTopInventory().getSize();
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot != null && rawSlot >= 0 && rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isSkyKingsMerchant(Inventory top) {
        if (top == null || top.getType() != InventoryType.MERCHANT || top.getSize() < 3) return false;
        ItemStack token = top.getItem(0);
        if (token == null || token.getType() != Material.NETHER_STAR || !token.hasItemMeta()) return false;
        ItemMeta meta = token.getItemMeta();
        return meta != null && meta.hasDisplayName() && TOKEN_NAME.equals(meta.getDisplayName());
    }
}
