package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.event.CrateOpenedEvent;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.model.Rank;
import net.skykings.core.shop.player.PlayerShopEgg;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import java.util.Map;
import java.util.UUID;

/** Crate UX: Preview, Roulette/Sofort und Open-All; normale Crates bleiben voll stackbar. */
public final class CrateInteractionListener implements Listener {

    public static final String OPEN_ALL_PERMISSION = "skykings.perk.crate.openall";
    private static final String PREVIEW_TITLE = ChatColor.DARK_GRAY + "Crate Preview";

    private final JavaPlugin plugin;
    private final CrateRegistry registry;
    private final CrateItemCodec codec;
    private final VoucherItemCodec voucherCodec = new VoucherItemCodec();
    private final PlayerShopEgg playerShopEgg = new PlayerShopEgg();
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
            // Nur alte Serial-Crates brauchen den historischen Redemption-Store.
            if (decoded.isLegacySerial() && !redemptionStore.isReady()) {
                event.getPlayer().sendMessage(ChatColor.RED + "Das Crate-System startet noch. Bitte versuche es gleich erneut.");
                return;
            }
            openChoice(event.getPlayer(), crate, decoded);
        }
    }

    @EventHandler public void onPreviewClick(InventoryClickEvent event) {
        if (event.getView() != null && PREVIEW_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }
    @EventHandler public void onPreviewDrag(InventoryDragEvent event) {
        if (event.getView() != null && PREVIEW_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    private void openChoice(Player player, CrateRegistry.CrateDefinition crate, CrateItemCodec.DecodedCrate decoded) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Crate oeffnen", 27);
        gui.setItem(11, named(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "MIT ANIMATION",
                ChatColor.GRAY + "Ca. 6 Sekunden Roulette mit Sounds.", ChatColor.YELLOW + "Klicken"),
                (p,e,s) -> startAnimation(p, crate, decoded));
        gui.setItem(15, named(Material.ENDER_CHEST, ChatColor.GREEN.toString() + ChatColor.BOLD + "SOFORT OEFFNEN",
                ChatColor.GRAY + "Zeigt dir den Gewinn direkt.", ChatColor.YELLOW + "Klicken"),
                (p,e,s) -> {
                    p.closeInventory();
                    openClaim(p, crate, decoded.getSerial(), decoded.getMaxClaims(), null, null);
                });
        if (canOpenAll(player)) {
            gui.setItem(22, named(Material.CHEST, ChatColor.GOLD.toString() + ChatColor.BOLD + "ALLE SOFORT OEFFNEN",
                    ChatColor.GRAY + "Oeffnet alle Crates dieses Typs nacheinander.", ChatColor.YELLOW + "Klicken"),
                    (p,e,s) -> {
                        p.closeInventory();
                        openAll(p, crate);
                    });
        }
        core.getGuiManager().open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.3F);
    }

    private boolean canOpenAll(Player player) {
        return player.hasPermission(OPEN_ALL_PERMISSION)
                || core.getRankService().hasAtLeast(player.getUniqueId(), Rank.EXILE);
    }

    private void startAnimation(Player player, CrateRegistry.CrateDefinition crate, CrateItemCodec.DecodedCrate decoded) {
        final CrateRegistry.RewardDefinition finalReward = registry.draw(crate);
        if (finalReward == null) {
            player.sendMessage(ChatColor.RED + "Diese Crate hat keine gueltigen Rewards.");
            return;
        }
        if (requiresSpace(finalReward) && !hasSpace(player, finalReward)) {
            player.sendMessage(ChatColor.RED + "Nicht genug Inventarplatz fuer den Gewinn.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }

        final GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | " + ChatColor.LIGHT_PURPLE + "Crate Roulette", 27);
        for (int i = 0; i < 27; i++) gui.setItem(i, named(Material.STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " "));
        gui.setItem(4, named(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "SKYKINGS CRATE",
                ChatColor.GRAY + "Dein Gewinn wird gezogen..."));
        gui.setItem(12, named(Material.STAINED_GLASS_PANE, ChatColor.GOLD + ">>>"));
        gui.setItem(14, named(Material.STAINED_GLASS_PANE, ChatColor.GOLD + "<<<"));
        core.getGuiManager().open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.6F, 1.0F);

        final int[] step = {0};
        final int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) return;
            int current = step[0]++;
            if (current >= 24) return;
            CrateRegistry.RewardDefinition preview = registry.draw(crate);
            if (preview != null) gui.getInventory().setItem(13, rewardIcon(preview));
            float pitch = Math.min(1.9F, 0.75F + current * 0.045F);
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.55F, pitch);
            player.updateInventory();
        }, 0L, 4L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId);
            if (!player.isOnline()) return;
            gui.getInventory().setItem(13, rewardIcon(finalReward));
            gui.getInventory().setItem(4, named(Material.DIAMOND, ChatColor.GOLD.toString() + ChatColor.BOLD + "DEIN GEWINN",
                    ChatColor.YELLOW + rewardText(finalReward)));
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.9F, 1.45F);
            player.updateInventory();
        }, 96L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.closeInventory();
            openClaim(player, crate, decoded.getSerial(), decoded.getMaxClaims(), null, finalReward);
        }, 120L);
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
            lore.add(ChatColor.GRAY + "Chance: " + ChatColor.WHITE + String.format(java.util.Locale.US, "%.2f%%", chance));
            lore.add(ChatColor.GRAY + "Economy-Wert: " + ChatColor.GOLD + UiFormat.coins(reward.getEvValue()));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot++, icon);
        }
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.2F);
        player.sendMessage(ChatColor.GRAY + "Expected Value: " + ChatColor.GOLD
                + UiFormat.coins(Math.round(crate.getExpectedValue())));
    }

    private void openAll(Player player, CrateRegistry.CrateDefinition crate) {
        List<Claim> claims = new ArrayList<Claim>();
        for (ItemStack item : player.getInventory().getContents()) {
            CrateItemCodec.DecodedCrate decoded = codec.decode(item);
            if (decoded == null || !crate.getId().equalsIgnoreCase(decoded.getCrateId())) continue;
            for (int i = 0; i < item.getAmount(); i++) claims.add(new Claim(decoded.getSerial(), decoded.getMaxClaims()));
        }
        if (claims.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du besitzt keine Crates dieses Typs.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Open-All: " + ChatColor.YELLOW + claims.size()
                + ChatColor.GRAY + " Crates werden nacheinander geoeffnet...");
        openAllNext(player, crate, claims, 0);
    }

    private void openAllNext(Player player, CrateRegistry.CrateDefinition crate, List<Claim> claims, int index) {
        if (!player.isOnline()) return;
        if (index >= claims.size()) {
            player.sendMessage(ChatColor.GREEN + "Open-All abgeschlossen.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.65F, 1.35F);
            return;
        }
        Claim claim = claims.get(index);
        openClaim(player, crate, claim.serial, claim.maxClaims,
                () -> openAllNext(player, crate, claims, index + 1), null);
    }

    private void openClaim(Player player, CrateRegistry.CrateDefinition crate, UUID serial, int maxClaims,
                           Runnable onFinished, CrateRegistry.RewardDefinition selectedReward) {
        final CrateRegistry.RewardDefinition reward = selectedReward != null ? selectedReward : registry.draw(crate);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "Diese Crate hat keine gueltigen Rewards.");
            finish(onFinished);
            return;
        }
        if (requiresSpace(reward) && !hasSpace(player, reward)) {
            player.sendMessage(ChatColor.RED + "Nicht genug Inventarplatz fuer den naechsten Reward.");
            return;
        }

        if (serial == null) {
            if (!removeOne(player, null, crate.getId())) {
                player.sendMessage(ChatColor.RED + "Du besitzt diese Crate nicht mehr.");
                finish(onFinished);
                return;
            }
            if (!grant(player, reward)) {
                refundCrate(player, crate);
                player.sendMessage(ChatColor.RED + "Reward-Vergabe fehlgeschlagen. Deine Crate wurde zurueckgegeben.");
                finish(onFinished);
                return;
            }
            completeOpen(player, crate, reward, onFinished);
            return;
        }

        redemptionStore.redeem(serial, maxClaims).thenAccept(firstRedemption ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (!firstRedemption) {
                        removeOne(player, serial, crate.getId());
                        player.sendMessage(ChatColor.RED + "Eine bereits vollstaendig eingeloeste alte Crate-Batch wurde bereinigt.");
                        finish(onFinished);
                        return;
                    }
                    if (!grant(player, reward)) {
                        removeOne(player, serial, crate.getId());
                        player.sendMessage(ChatColor.RED + "Reward-Vergabe fehlgeschlagen. Der alte Serial-Claim wurde sicher gesperrt.");
                        finish(onFinished);
                        return;
                    }
                    removeOne(player, serial, crate.getId());
                    completeOpen(player, crate, reward, onFinished);
                }));
    }

    private void completeOpen(Player player, CrateRegistry.CrateDefinition crate,
                              CrateRegistry.RewardDefinition reward, Runnable onFinished) {
        Bukkit.getPluginManager().callEvent(new CrateOpenedEvent(player, crate.getId()));
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "CRATE GEWINN: " + ChatColor.YELLOW + rewardText(reward));
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.5F);
        finish(onFinished);
    }

    private boolean requiresSpace(CrateRegistry.RewardDefinition reward) {
        return reward.getType() == CrateRegistry.RewardType.ITEM
                || reward.getType() == CrateRegistry.RewardType.VOUCHER
                || reward.getType() == CrateRegistry.RewardType.PLAYER_SHOP_EGG;
    }

    private void finish(Runnable onFinished) { if (onFinished != null) onFinished.run(); }

    private boolean removeOne(Player player, UUID serial, String crateId) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            CrateItemCodec.DecodedCrate decoded = codec.decode(item);
            if (decoded == null || !crateId.equalsIgnoreCase(decoded.getCrateId())) continue;
            boolean match = serial == null ? decoded.getSerial() == null : serial.equals(decoded.getSerial());
            if (!match) continue;
            if (item.getAmount() <= 1) player.getInventory().setItem(slot, null);
            else {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(slot, item);
            }
            player.updateInventory();
            return true;
        }
        return false;
    }

    private void refundCrate(Player player, CrateRegistry.CrateDefinition crate) {
        ItemStack refund = codec.create(crate, 1);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(refund);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.updateInventory();
    }

    private boolean hasSpace(Player player, CrateRegistry.RewardDefinition reward) {
        ItemStack rewardItem;
        if (reward.getType() == CrateRegistry.RewardType.VOUCHER) {
            rewardItem = voucherCodec.preview(reward.getVoucherType(), reward.getVoucherDisplay());
        } else if (reward.getType() == CrateRegistry.RewardType.NETHERSTARS) {
            rewardItem = SkyKingsCurrencyItems.star(1);
        } else if (reward.getType() == CrateRegistry.RewardType.PLAYER_SHOP_EGG) {
            rewardItem = playerShopEgg.create();
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
                    core.getEconomyService().deposit(player.getUniqueId(), reward.getAmount(), "CRATE", "Crate-Reward " + reward.getId());
                    return true;
                case NETHERSTARS:
                    SkyKingsCurrencyItems.give(player, reward.getAmount());
                    return true;
                case PLAYER_SHOP_EGG:
                    if (!hasSpace(player, reward)) return false;
                    return player.getInventory().addItem(playerShopEgg.create()).isEmpty();
                case ITEM:
                    if (!hasSpace(player, reward)) return false;
                    return player.getInventory().addItem(new ItemStack(reward.getMaterial(), (int) reward.getAmount(), reward.getData())).isEmpty();
                case VOUCHER:
                    if (!hasSpace(player, reward)) return false;
                    ItemStack voucher = voucherCodec.create(reward.getVoucherType(), reward.getVoucherTarget(), reward.getVoucherDisplay());
                    VoucherItemCodec.DecodedVoucher decoded = voucherCodec.decode(voucher);
                    if (decoded == null || !player.getInventory().addItem(voucher).isEmpty()) return false;
                    core.getLoggingService().log(new AuditEvent(AuditEventType.VOUCHER_GENERATED,
                            player.getUniqueId(), "CRATE", null,
                            "serial=" + decoded.getSerial() + ", type=" + decoded.getType()
                                    + ", target=" + decoded.getTarget() + ", reward=" + reward.getId()));
                    return true;
                default: return false;
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Crate-Reward fehlgeschlagen fuer " + player.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private ItemStack rewardIcon(CrateRegistry.RewardDefinition reward) {
        if (reward.getType() == CrateRegistry.RewardType.VOUCHER) {
            return voucherCodec.preview(reward.getVoucherType(), reward.getVoucherDisplay());
        }
        ItemStack icon;
        switch (reward.getType()) {
            case COINS: icon = new ItemStack(Material.DOUBLE_PLANT); break;
            case NETHERSTARS: icon = SkyKingsCurrencyItems.star(1); break;
            case PLAYER_SHOP_EGG: icon = playerShopEgg.create(); break;
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
            case COINS: return UiFormat.coins(reward.getAmount());
            case NETHERSTARS: return UiFormat.number(reward.getAmount()) + " SkyKings Sterne";
            case PLAYER_SHOP_EGG: return "1x SkyKings Haendler-Ei";
            case ITEM: return reward.getAmount() + "x " + reward.getMaterial().name();
            case VOUCHER: return "Gutschein: " + ChatColor.translateAlternateColorCodes('&', reward.getVoucherDisplay());
            default: return reward.getId();
        }
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && lore.length > 0) meta.setLore(UiItems.wrapLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static final class Claim {
        final UUID serial;
        final int maxClaims;
        Claim(UUID serial, int maxClaims) { this.serial = serial; this.maxClaims = maxClaims; }
    }
}
