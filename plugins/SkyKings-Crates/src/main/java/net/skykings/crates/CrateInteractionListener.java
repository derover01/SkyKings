package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Linksklick = Preview, Rechtsklick = eine Crate oeffnen. */
public final class CrateInteractionListener implements Listener {

    private static final String PREVIEW_TITLE = ChatColor.DARK_GRAY + "Crate Preview";

    private final CrateRegistry registry;
    private final CrateItemCodec codec;
    private final SkyKingsCoreAPI core;

    public CrateInteractionListener(CrateRegistry registry, CrateItemCodec codec, SkyKingsCoreAPI core) {
        this.registry = registry;
        this.codec = codec;
        this.core = core;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack hand = event.getItem();
        CrateItemCodec.DecodedCrate decoded = codec.decode(hand);
        if (decoded == null) return;

        CrateRegistry.CrateDefinition crate = registry.get(decoded.getCrateId());
        if (crate == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Diese Crate ist nicht mehr gueltig.");
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            openPreview(event.getPlayer(), crate);
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            openOne(event.getPlayer(), hand, crate);
        }
    }

    @EventHandler
    public void onPreviewClick(InventoryClickEvent event) {
        if (event.getView() != null && PREVIEW_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPreviewDrag(InventoryDragEvent event) {
        if (event.getView() != null && PREVIEW_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private void openPreview(Player player, CrateRegistry.CrateDefinition crate) {
        int rows = Math.max(1, Math.min(6, (crate.getRewards().size() + 8) / 9));
        Inventory inventory = Bukkit.createInventory(null, rows * 9, PREVIEW_TITLE);
        int totalWeight = 0;
        for (CrateRegistry.RewardDefinition reward : crate.getRewards()) totalWeight += reward.getWeight();

        int slot = 0;
        for (CrateRegistry.RewardDefinition reward : crate.getRewards()) {
            ItemStack icon = rewardIcon(reward);
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = new ArrayList<String>();
            double chance = totalWeight <= 0 ? 0D : (100D * reward.getWeight() / totalWeight);
            lore.add(ChatColor.GRAY + "Chance: " + ChatColor.WHITE + String.format(java.util.Locale.US, "%.1f%%", chance));
            lore.add(ChatColor.GRAY + "Wert: " + ChatColor.GOLD + reward.getEvValue());
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot++, icon);
        }
        player.openInventory(inventory);
        player.sendMessage(ChatColor.GRAY + "Expected Value: " + ChatColor.GOLD
                + Math.round(crate.getExpectedValue()) + ChatColor.GRAY + " Coins-Wert");
    }

    private void openOne(Player player, ItemStack hand, CrateRegistry.CrateDefinition crate) {
        CrateRegistry.RewardDefinition reward = registry.draw(crate);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "Diese Crate hat keine gueltigen Rewards.");
            return;
        }

        if (!grant(player, reward)) {
            player.sendMessage(ChatColor.RED + "Reward konnte nicht vergeben werden. Die Crate wurde nicht verbraucht.");
            return;
        }

        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) player.setItemInHand(null);
        else player.setItemInHand(hand);
        player.updateInventory();
        player.sendMessage(ChatColor.GOLD + "Crate geoeffnet! " + ChatColor.YELLOW + rewardText(reward));
    }

    private boolean grant(Player player, CrateRegistry.RewardDefinition reward) {
        switch (reward.getType()) {
            case COINS:
                try {
                    core.getEconomyService().deposit(player.getUniqueId(), reward.getAmount(), "CRATE",
                            "Crate-Reward " + reward.getId());
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            case NETHERSTARS:
                try {
                    core.getNetherstarService().deposit(player.getUniqueId(), reward.getAmount(), "CRATE",
                            "Crate-Reward " + reward.getId());
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            case ITEM:
                if (reward.getAmount() > 64L) return false;
                ItemStack item = new ItemStack(reward.getMaterial(), (int) reward.getAmount(), reward.getData());
                if (!canFit(player, item)) return false;
                return player.getInventory().addItem(item).isEmpty();
            default:
                return false;
        }
    }

    private boolean canFit(Player player, ItemStack item) {
        Inventory simulation = Bukkit.createInventory(null, 36);
        for (int slot = 0; slot < 36; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current != null) simulation.setItem(slot, current.clone());
        }
        return simulation.addItem(item.clone()).isEmpty();
    }

    private ItemStack rewardIcon(CrateRegistry.RewardDefinition reward) {
        ItemStack icon;
        switch (reward.getType()) {
            case COINS:
                icon = new ItemStack(Material.GOLD_INGOT);
                break;
            case NETHERSTARS:
                icon = new ItemStack(Material.NETHER_STAR);
                break;
            case ITEM:
            default:
                icon = new ItemStack(reward.getMaterial(), 1, reward.getData());
                break;
        }
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + rewardText(reward));
        icon.setItemMeta(meta);
        return icon;
    }

    private String rewardText(CrateRegistry.RewardDefinition reward) {
        switch (reward.getType()) {
            case COINS: return reward.getAmount() + " Coins";
            case NETHERSTARS: return reward.getAmount() + " Nethersterne";
            case ITEM: return reward.getAmount() + "x " + reward.getMaterial().name();
            default: return reward.getId();
        }
    }
}
