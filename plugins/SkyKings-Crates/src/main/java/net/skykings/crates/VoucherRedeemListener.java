package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.kit.KitDefinition;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.model.Rank;
import net.skykings.core.permission.VoucherPermissionService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Rechtsklick auf Gutschein = einmalige, persistente Einloesung. */
public final class VoucherRedeemListener implements Listener {
    private final SkyKingsCrates plugin;
    private final VoucherItemCodec codec;
    private final VoucherRedemptionStore store;
    private final SkyKingsCoreAPI core;

    public VoucherRedeemListener(SkyKingsCrates plugin, VoucherItemCodec codec,
                                 VoucherRedemptionStore store, SkyKingsCoreAPI core) {
        this.plugin = plugin;
        this.codec = codec;
        this.store = store;
        this.core = core;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = event.getItem();
        VoucherItemCodec.DecodedVoucher voucher = codec.decode(hand);
        if (voucher == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!store.isReady()) {
            player.sendMessage(ChatColor.RED + "Gutschein-System startet noch. Bitte gleich erneut versuchen.");
            return;
        }
        if (!canRedeem(player, voucher)) return;

        store.redeem(voucher.getSerial()).thenAccept(marked -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!marked) {
                player.sendMessage(ChatColor.RED + "Dieser Gutschein wurde bereits eingeloest oder konnte nicht sicher gespeichert werden.");
                return;
            }
            if (!grant(player, voucher)) {
                player.sendMessage(ChatColor.RED + "Gutschein konnte nicht vergeben werden. Bitte einem Admin melden: " + voucher.getSerial());
                return;
            }
            consume(player, hand);
            core.getLoggingService().log(new AuditEvent(AuditEventType.VOUCHER_REDEEMED,
                    player.getUniqueId(), player.getName(), null,
                    "serial=" + voucher.getSerial() + ", type=" + voucher.getType() + ", target=" + voucher.getTarget()));
            player.sendMessage(ChatColor.GREEN + "Gutschein erfolgreich eingeloest!");
        }));
    }

    private boolean canRedeem(Player player, VoucherItemCodec.DecodedVoucher voucher) {
        switch (voucher.getType()) {
            case RANK:
                Rank rank = parseRank(voucher.getTarget());
                if (rank == null) return invalid(player);
                Rank current = core.getRankService().getRank(player.getUniqueId());
                if (current.isAtLeast(rank)) {
                    player.sendMessage(ChatColor.YELLOW + "Du besitzt diesen oder einen hoeheren Rang bereits.");
                    return false;
                }
                return true;
            case KIT:
                Optional<KitDefinition> kit = core.getKitRegistry().get(voucher.getTarget());
                if (!kit.isPresent()) return invalid(player);
                return hasInventorySpace(player, kit.get().createItems());
            case PERMISSION:
                if (core.getVoucherPermissionService().find(voucher.getTarget()) == null) return invalid(player);
                return true;
            case PREFIX:
                return voucher.getTarget().matches("[a-zA-Z0-9_-]{1,32}") || invalid(player);
            default:
                return invalid(player);
        }
    }

    private boolean grant(Player player, VoucherItemCodec.DecodedVoucher voucher) {
        switch (voucher.getType()) {
            case RANK:
                Rank rank = parseRank(voucher.getTarget());
                if (rank == null) return false;
                core.getRankService().setRank(player.getUniqueId(), rank, "VOUCHER:" + voucher.getSerial());
                return true;
            case KIT:
                Optional<KitDefinition> optional = core.getKitRegistry().get(voucher.getTarget());
                if (!optional.isPresent()) return false;
                KitDefinition kit = optional.get();
                List<ItemStack> items = kit.createItems();
                if (!hasInventorySpace(player, items)) return false;
                for (ItemStack item : items) player.getInventory().addItem(item.clone());
                for (PotionEffect effect : kit.getPotionEffects()) player.addPotionEffect(effect, true);
                player.updateInventory();
                return true;
            case PERMISSION:
                return core.getVoucherPermissionService().grant(player.getUniqueId(), voucher.getTarget(),
                        "VOUCHER:" + voucher.getSerial()) == VoucherPermissionService.GrantStatus.GRANTED;
            case PREFIX:
                return core.getVoucherPermissionService().grantPrefix(player.getUniqueId(), voucher.getTarget(),
                        "VOUCHER:" + voucher.getSerial()) == VoucherPermissionService.GrantStatus.GRANTED;
            default:
                return false;
        }
    }

    private boolean hasInventorySpace(Player player, List<ItemStack> items) {
        ItemStack[] simulation = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack existing = player.getInventory().getItem(i);
            simulation[i] = existing == null ? null : existing.clone();
        }
        for (ItemStack raw : items) {
            if (raw == null || raw.getAmount() <= 0) continue;
            ItemStack remaining = raw.clone();
            for (int i = 0; i < simulation.length && remaining.getAmount() > 0; i++) {
                ItemStack existing = simulation[i];
                if (existing == null || !existing.isSimilar(remaining)) continue;
                int room = existing.getMaxStackSize() - existing.getAmount();
                if (room <= 0) continue;
                int move = Math.min(room, remaining.getAmount());
                existing.setAmount(existing.getAmount() + move);
                remaining.setAmount(remaining.getAmount() - move);
            }
            while (remaining.getAmount() > 0) {
                int free = -1;
                for (int i = 0; i < simulation.length; i++) if (simulation[i] == null) { free = i; break; }
                if (free < 0) {
                    player.sendMessage(ChatColor.RED + "Du brauchst mehr freien Inventarplatz fuer dieses Kit.");
                    return false;
                }
                int amount = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
                ItemStack part = remaining.clone();
                part.setAmount(amount);
                simulation[free] = part;
                remaining.setAmount(remaining.getAmount() - amount);
            }
        }
        return true;
    }

    private Rank parseRank(String raw) {
        try { return Rank.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private boolean invalid(Player player) {
        player.sendMessage(ChatColor.RED + "Dieser Gutschein enthaelt ein ungueltiges Ziel.");
        return false;
    }

    private void consume(Player player, ItemStack hand) {
        if (hand.getAmount() <= 1) player.setItemInHand(null);
        else hand.setAmount(hand.getAmount() - 1);
        player.updateInventory();
    }
}
