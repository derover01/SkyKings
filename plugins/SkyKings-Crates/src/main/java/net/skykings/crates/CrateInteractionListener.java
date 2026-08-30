package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.model.Rank;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Linksklick = Preview, Rechtsklick = eine Crate, Shift+Rechtsklick = Open-All ab Exile. */
public final class CrateInteractionListener implements Listener {

    private static final String PREVIEW_TITLE = ChatColor.DARK_GRAY + "Crate Preview";

    private final JavaPlugin plugin;
    private final CrateRegistry registry;
    private final CrateItemCodec codec;
    private final VoucherItemCodec voucherCodec = new VoucherItemCodec();
    private final CrateRedemptionStore redemptionStore;
    private final SkyKingsCoreAPI core;

    public CrateInteractionListener(JavaPlugin plugin, CrateRegistry registry, CrateItemCodec codec,
                                    CrateRedemptionStore redemptionStore, SkyKingsCoreAPI core) {
        this.plugin = plugin;
        this.registry = registry;
        this.codec = codec;
        this.redemptionStore = redemptionStore;
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
            if (!redemptionStore.isReady()) {
                event.getPlayer().sendMessage(ChatColor.RED + "Das Crate-System startet noch. Bitte versuche es gleich erneut.");
                return;
            }
            if (event.getPlayer().isSneaking()) {
                if (!event.getPlayer().isOp()
                        && !core.getRankService().hasAtLeast(event.getPlayer().getUniqueId(), Rank.EXILE)) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Open-All ist ab Exile verfuegbar.");
                    return;
                }
                openAll(event.getPlayer(), crate);
            } else {
                openSerial(event.getPlayer(), crate, decoded.getSerial(), null);
            }
        }
    }

    @EventHandler
    public void onPreviewClick(InventoryClickEvent event) {
        if (event.getView() != null && PREVIEW_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    @EventHandler
    public void onPreviewDrag(InventoryDragEvent event) {
        if (event.getView() != null && PREVIEW_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
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
            lore.add(ChatColor.GRAY + "Chance: " + ChatColor.WHITE
                    + String.format(java.util.Locale.US, "%.1f%%", chance));
            lore.add(ChatColor.GRAY + "Wert: " + ChatColor.GOLD + reward.getEvValue());
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot++, icon);
        }
        player.openInventory(inventory);
        player.sendMessage(ChatColor.GRAY + "Expected Value: " + ChatColor.GOLD
                + Math.round(crate.getExpectedValue()) + ChatColor.GRAY + " Coins-Wert");
    }

    private void openAll(Player player, CrateRegistry.CrateDefinition crate) {
        List<UUID> serials = new ArrayList<UUID>();
        for (ItemStack item : player.getInventory().getContents()) {
            CrateItemCodec.DecodedCrate decoded = codec.decode(item);
            if (decoded != null && crate.getId().equalsIgnoreCase(decoded.getCrateId())) serials.add(decoded.getSerial());
        }
        if (serials.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du besitzt keine Crates dieses Typs.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Open-All: " + ChatColor.YELLOW + serials.size()
                + ChatColor.GRAY + " Crates werden nacheinander geoeffnet...");
        openAllNext(player, crate, serials, 0);
    }

    private void openAllNext(Player player, CrateRegistry.CrateDefinition crate, List<UUID> serials, int index) {
        if (!player.isOnline()) return;
        if (index >= serials.size()) {
            player.sendMessage(ChatColor.GREEN + "Open-All abgeschlossen.");
            return;
        }
        openSerial(player, crate, serials.get(index), () -> openAllNext(player, crate, serials, index + 1));
    }

    private void openSerial(Player player, CrateRegistry.CrateDefinition crate, UUID serial, Runnable onFinished) {
        CrateRegistry.RewardDefinition reward = registry.draw(crate);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "Diese Crate hat keine gueltigen Rewards.");
            finish(onFinished);
            return;
        }
        if ((reward.getType() == CrateRegistry.RewardType.ITEM || reward.getType() == CrateRegistry.RewardType.VOUCHER)
                && !hasSpace(player, reward)) {
            player.sendMessage(ChatColor.RED + "Open-All gestoppt: Nicht genug Inventarplatz fuer den naechsten Reward.");
            return;
        }

        redemptionStore.redeem(serial).thenAccept(firstRedemption ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (!firstRedemption) {
                        removeSerial(player, serial);
                        player.sendMessage(ChatColor.RED + "Eine bereits eingeloeste Crate wurde entfernt.");
                        finish(onFinished);
                        return;
                    }
                    if (!grant(player, reward)) {
                        removeSerial(player, serial);
                        player.sendMessage(ChatColor.RED + "Reward-Vergabe fehlgeschlagen. Die Seriennummer wurde sicher gesperrt.");
                        return;
                    }
                    removeSerial(player, serial);
                    player.sendMessage(ChatColor.GOLD + "Crate geoeffnet! " + ChatColor.YELLOW + rewardText(reward));
                    finish(onFinished);
                }));
    }

    private void finish(Runnable onFinished) {
        if (onFinished != null) onFinished.run();
    }

    private void removeSerial(Player player, UUID serial) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            CrateItemCodec.DecodedCrate decoded = codec.decode(item);
            if (decoded != null && serial.equals(decoded.getSerial())) {
                if (item.getAmount() <= 1) player.getInventory().setItem(slot, null);
                else {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItem(slot, item);
                }
                player.updateInventory();
                return;
            }
        }
    }

    private boolean hasSpace(Player player, CrateRegistry.RewardDefinition reward) {
        ItemStack rewardItem;
        if (reward.getType() == CrateRegistry.RewardType.VOUCHER) {
            rewardItem = voucherCodec.create(reward.getVoucherType(), reward.getVoucherTarget(), reward.getVoucherDisplay());
        } else {
            if (reward.getAmount() > 64L) return false;
            rewardItem = new ItemStack(reward.getMaterial(), (int) reward.getAmount(), reward.getData());
        }
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(rewardItem).isEmpty();
    }

    private boolean grant(Player player, CrateRegistry.RewardDefinition reward) {
        try {
            switch (reward.getType()) {
                case COINS:
                    core.getEconomyService().deposit(player.getUniqueId(), reward.getAmount(), "CRATE",
                            "Crate-Reward " + reward.getId());
                    return true;
                case NETHERSTARS:
                    core.getNetherstarService().deposit(player.getUniqueId(), reward.getAmount(), "CRATE",
                            "Crate-Reward " + reward.getId());
                    return true;
                case ITEM:
                    if (!hasSpace(player, reward)) return false;
                    return player.getInventory().addItem(new ItemStack(reward.getMaterial(),
                            (int) reward.getAmount(), reward.getData())).isEmpty();
                case VOUCHER:
                    if (!hasSpace(player, reward)) return false;
                    ItemStack voucher = voucherCodec.create(reward.getVoucherType(), reward.getVoucherTarget(), reward.getVoucherDisplay());
                    VoucherItemCodec.DecodedVoucher decoded = voucherCodec.decode(voucher);
                    if (decoded == null) return false;
                    if (!player.getInventory().addItem(voucher).isEmpty()) return false;
                    core.getLoggingService().log(new AuditEvent(AuditEventType.VOUCHER_GENERATED,
                            player.getUniqueId(), "CRATE", null,
                            "serial=" + decoded.getSerial() + ", type=" + decoded.getType()
                                    + ", target=" + decoded.getTarget() + ", reward=" + reward.getId()));
                    return true;
                default:
                    return false;
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Crate-Reward fehlgeschlagen fuer " + player.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private ItemStack rewardIcon(CrateRegistry.RewardDefinition reward) {
        ItemStack icon;
        switch (reward.getType()) {
            case COINS: icon = new ItemStack(Material.GOLD_INGOT); break;
            case NETHERSTARS: icon = new ItemStack(Material.NETHER_STAR); break;
            case VOUCHER: icon = new ItemStack(Material.PAPER); break;
            case ITEM:
            default: icon = new ItemStack(reward.getMaterial(), 1, reward.getData()); break;
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
            case VOUCHER: return "Gutschein: " + reward.getVoucherDisplay();
            default: return reward.getId();
        }
    }
}
