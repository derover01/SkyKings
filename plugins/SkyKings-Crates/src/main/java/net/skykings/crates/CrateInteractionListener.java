package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.gui.GuiSession;
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
import java.util.Map;
import java.util.UUID;

/** Crate UX: Preview, Auswahlmenü, Animation/Sofort und Open-All. */
public final class CrateInteractionListener implements Listener {

    public static final String OPEN_ALL_PERMISSION = "skykings.perk.crate.openall";
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
            event.getPlayer().sendMessage(ChatColor.RED + "Diese Crate ist nicht mehr gültig.");
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
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Crate öffnen", 27);
        gui.setItem(11, named(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Mit Animation",
                ChatColor.GRAY + "Öffnet eine Crate mit Roulette-Animation.", ChatColor.YELLOW + "Klicken"),
                (p,e,s) -> startAnimation(p, crate, decoded));
        gui.setItem(15, named(Material.ENDER_CHEST, ChatColor.GREEN + "Sofort öffnen",
                ChatColor.GRAY + "Zeigt dir den Gewinn direkt.", ChatColor.YELLOW + "Klicken"),
                (p,e,s) -> {
                    p.closeInventory();
                    openSerial(p, crate, decoded.getSerial(), decoded.getMaxClaims(), null);
                });
        if (canOpenAll(player)) {
            gui.setItem(22, named(Material.CHEST, ChatColor.GOLD + "Alle sofort öffnen",
                    ChatColor.GRAY + "Öffnet alle Crates dieses Typs nacheinander.", ChatColor.YELLOW + "Klicken"),
                    (p,e,s) -> {
                        p.closeInventory();
                        openAll(p, crate);
                    });
        }
        core.getGuiManager().open(gui);
    }

    private boolean canOpenAll(Player player) {
        return player.hasPermission(OPEN_ALL_PERMISSION)
                || core.getRankService().hasAtLeast(player.getUniqueId(), Rank.EXILE);
    }

    private void startAnimation(Player player, CrateRegistry.CrateDefinition crate, CrateItemCodec.DecodedCrate decoded) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Öffnung", 27);
        for (int i = 0; i < 27; i++) gui.setItem(i, named(Material.STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "•"));
        core.getGuiManager().open(gui);
        final int[] step = {0};
        final int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) return;
            CrateRegistry.RewardDefinition preview = registry.draw(crate);
            if (preview != null) gui.getInventory().setItem(13, rewardIcon(preview));
            player.updateInventory();
            step[0]++;
        }, 0L, 3L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId);
            if (!player.isOnline()) return;
            player.closeInventory();
            openSerial(player, crate, decoded.getSerial(), decoded.getMaxClaims(), null);
        }, 30L);
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
        List<Claim> claims = new ArrayList<Claim>();
        for (ItemStack item : player.getInventory().getContents()) {
            CrateItemCodec.DecodedCrate decoded = codec.decode(item);
            if (decoded == null || !crate.getId().equalsIgnoreCase(decoded.getCrateId())) continue;
            for (int i = 0; i < item.getAmount(); i++) {
                claims.add(new Claim(decoded.getSerial(), decoded.getMaxClaims()));
            }
        }
        if (claims.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du besitzt keine Crates dieses Typs.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Open-All: " + ChatColor.YELLOW + claims.size()
                + ChatColor.GRAY + " Crates werden nacheinander geöffnet...");
        openAllNext(player, crate, claims, 0);
    }

    private void openAllNext(Player player, CrateRegistry.CrateDefinition crate, List<Claim> claims, int index) {
        if (!player.isOnline()) return;
        if (index >= claims.size()) {
            player.sendMessage(ChatColor.GREEN + "Open-All abgeschlossen.");
            return;
        }
        Claim claim = claims.get(index);
        openSerial(player, crate, claim.serial, claim.maxClaims,
                () -> openAllNext(player, crate, claims, index + 1));
    }

    private void openSerial(Player player, CrateRegistry.CrateDefinition crate, UUID serial, int maxClaims, Runnable onFinished) {
        CrateRegistry.RewardDefinition reward = registry.draw(crate);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "Diese Crate hat keine gültigen Rewards.");
            finish(onFinished);
            return;
        }
        if ((reward.getType() == CrateRegistry.RewardType.ITEM || reward.getType() == CrateRegistry.RewardType.VOUCHER)
                && !hasSpace(player, reward)) {
            player.sendMessage(ChatColor.RED + "Nicht genug Inventarplatz für den nächsten Reward.");
            return;
        }

        redemptionStore.redeem(serial, maxClaims).thenAccept(firstRedemption ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (!firstRedemption) {
                        removeOne(player, serial);
                        player.sendMessage(ChatColor.RED + "Eine bereits vollständig eingelöste Crate-Batch wurde bereinigt.");
                        finish(onFinished);
                        return;
                    }
                    if (!grant(player, reward)) {
                        removeOne(player, serial);
                        player.sendMessage(ChatColor.RED + "Reward-Vergabe fehlgeschlagen. Der Claim wurde sicher gesperrt.");
                        return;
                    }
                    removeOne(player, serial);
                    player.sendMessage(ChatColor.GOLD + "Gewinn: " + ChatColor.YELLOW + rewardText(reward));
                    finish(onFinished);
                }));
    }

    private void finish(Runnable onFinished) { if (onFinished != null) onFinished.run(); }

    private void removeOne(Player player, UUID serial) {
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
                    core.getEconomyService().deposit(player.getUniqueId(), reward.getAmount(), "CRATE", "Crate-Reward " + reward.getId());
                    return true;
                case NETHERSTARS:
                    givePhysicalNetherstars(player, reward.getAmount());
                    return true;
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
            plugin.getLogger().warning("Crate-Reward fehlgeschlagen für " + player.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private void givePhysicalNetherstars(Player player, long amount) {
        long remaining = amount;
        while (remaining > 0L) {
            int stackSize = (int) Math.min(64L, remaining);
            ItemStack stack = new ItemStack(Material.NETHER_STAR, stackSize);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            remaining -= stackSize;
        }
        player.updateInventory();
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
            case NETHERSTARS: return reward.getAmount() + " physische Nethersterne";
            case ITEM: return reward.getAmount() + "x " + reward.getMaterial().name();
            case VOUCHER: return ChatColor.translateAlternateColorCodes('&', reward.getVoucherDisplay());
            default: return reward.getId();
        }
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && lore.length > 0) meta.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static final class Claim {
        final UUID serial;
        final int maxClaims;
        Claim(UUID serial, int maxClaims) { this.serial = serial; this.maxClaims = maxClaims; }
    }
}
