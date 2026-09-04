package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.economy.BalanceSettlementGuard;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.kit.KitDefinition;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.model.Rank;
import net.skykings.core.permission.VoucherPermissionService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Rechtsklick auf Gutschein = sichere Einloesung; Rang/Rechte immer mit Confirm-GUI. */
public final class VoucherRedeemListener implements Listener {
    private static final long MAX_COIN_VOUCHER = 1_000_000_000L;

    private final SkyKingsCrates plugin;
    private final VoucherItemCodec codec;
    private final VoucherRedemptionStore store;
    private final SkyKingsCoreAPI core;
    private final RewardSettlementJournal rewardJournal;

    public VoucherRedeemListener(SkyKingsCrates plugin, VoucherItemCodec codec,
                                 VoucherRedemptionStore store, SkyKingsCoreAPI core) {
        this(plugin, codec, store, core, RewardSettlementJournal.active());
    }

    public VoucherRedeemListener(SkyKingsCrates plugin, VoucherItemCodec codec,
                                 VoucherRedemptionStore store, SkyKingsCoreAPI core,
                                 RewardSettlementJournal rewardJournal) {
        this.plugin = plugin;
        this.codec = codec;
        this.store = store;
        this.core = core;
        this.rewardJournal = rewardJournal;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        final VoucherItemCodec.DecodedVoucher voucher = codec.decode(event.getItem());
        if (voucher == null) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        if (!store.isReady()) {
            player.sendMessage(ChatColor.RED + "Gutschein-System startet noch. Bitte gleich erneut versuchen.");
            SoundFeedback.error(player);
            return;
        }
        if (rewardJournal != null && rewardJournal.hasPendingFor(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ein vorheriger Crate-/Voucher-Reward muss erst durch Staff geprueft werden.");
            SoundFeedback.error(player);
            return;
        }
        if (!canRedeem(player, voucher)) return;

        if (voucher.getType() == VoucherItemCodec.VoucherType.RANK
                || voucher.getType() == VoucherItemCodec.VoucherType.RANKUP
                || voucher.getType() == VoucherItemCodec.VoucherType.PERMISSION) {
            openDecisionGui(player, voucher);
            return;
        }
        beginRedeem(player, voucher);
    }

    private void openDecisionGui(final Player player, final VoucherItemCodec.DecodedVoucher voucher) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutschein bestaetigen", 27);
        boolean rankVoucher = voucher.getType() == VoucherItemCodec.VoucherType.RANK
                || voucher.getType() == VoucherItemCodec.VoucherType.RANKUP;
        String type = voucher.getType() == VoucherItemCodec.VoucherType.RANKUP ? "ULTRA RANKUP-GUTSCHEIN"
                : voucher.getType() == VoucherItemCodec.VoucherType.RANK ? "Rang-Gutschein" : "Rechte-Gutschein";
        Material icon = rankVoucher ? Material.DIAMOND : Material.PAPER;
        String target = voucher.getType() == VoucherItemCodec.VoucherType.RANKUP
                ? nextRankName(player) : voucher.getTarget();

        gui.setItem(13, UiItems.item(icon,
                ChatColor.GOLD.toString() + ChatColor.BOLD + type,
                ChatColor.GRAY + "Belohnung: " + ChatColor.WHITE + target,
                voucher.getType() == VoucherItemCodec.VoucherType.RANKUP
                        ? ChatColor.LIGHT_PURPLE + "Steigt genau eine Rangstufe auf." : ChatColor.GRAY + "Einmalige Einloesung"));

        gui.setItem(11, UiItems.item(Material.EMERALD_BLOCK,
                ChatColor.GREEN.toString() + ChatColor.BOLD + "ANNEHMEN",
                ChatColor.GRAY + "Gutschein jetzt einloesen.",
                ChatColor.RED + "Danach wird ein Exemplar verbraucht."), (p, e, s) -> {
            p.closeInventory();
            if (!hasMatchingSerial(p, voucher.getSerial())) {
                p.sendMessage(ChatColor.RED + "Der Gutschein ist nicht mehr in deinem Inventar.");
                SoundFeedback.error(p);
                return;
            }
            if (rewardJournal != null && rewardJournal.hasPendingFor(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "Ein vorheriger Reward muss erst durch Staff geprueft werden.");
                SoundFeedback.error(p);
                return;
            }
            if (!canRedeem(p, voucher)) return;
            beginRedeem(p, voucher);
        });

        gui.setItem(15, UiItems.item(Material.REDSTONE_BLOCK,
                ChatColor.RED.toString() + ChatColor.BOLD + "ABLEHNEN",
                ChatColor.GRAY + "Nichts wird eingeloest.",
                ChatColor.GREEN + "Der Gutschein bleibt erhalten."), (p, e, s) -> {
            p.closeInventory();
            p.sendMessage(ChatColor.YELLOW + "Gutschein nicht eingeloest. Du kannst ihn spaeter erneut benutzen.");
            SoundFeedback.back(p);
        });

        core.getGuiManager().open(gui);
        SoundFeedback.menuOpen(player);
    }

    /** Claim -> Reward -> persistenter Commit -> physisches Voucher-Item entfernen -> Journal schliessen. */
    private void beginRedeem(final Player player, final VoucherItemCodec.DecodedVoucher voucher) {
        final UUID serial = voucher.getSerial();
        final int maxClaims = maxClaims(voucher);
        if (maxClaims < 1) {
            player.sendMessage(ChatColor.RED + "Dieser Gutschein ist nicht als serverseitig ausgegeben registriert.");
            SoundFeedback.error(player);
            return;
        }
        if (!player.isOnline()) return;
        if (rewardJournal != null && rewardJournal.hasPendingFor(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ein vorheriger Reward muss erst durch Staff geprueft werden.");
            SoundFeedback.error(player);
            return;
        }

        final UUID transaction = rewardJournal == null ? null : rewardJournal.begin(
                "VOUCHER", serial.toString(), player.getUniqueId(), voucher.getType().name(), voucher.getTarget());
        if (rewardJournal != null && transaction == null) {
            player.sendMessage(ChatColor.RED + "Gutschein konnte nicht sicher vorbereitet werden. Es wurde nichts verbraucht.");
            SoundFeedback.error(player);
            return;
        }

        boolean marked = store.redeemSync(serial, maxClaims);
        if (!marked) {
            completeJournal(transaction, "CLAIM_REJECTED_BEFORE_NEW_REWARD");
            player.sendMessage(ChatColor.RED + "Dieser Gutschein wurde bereits vollstaendig eingeloest oder konnte nicht sicher gespeichert werden.");
            SoundFeedback.error(player);
            return;
        }

        if (voucher.getType() == VoucherItemCodec.VoucherType.PERMISSION
                || voucher.getType() == VoucherItemCodec.VoucherType.PREFIX) {
            grantPermissionVoucherDurably(player, voucher, maxClaims, transaction);
            return;
        }

        final List<Player> giveAllRecipients = voucher.getType() == VoucherItemCodec.VoucherType.GIVEALL_COINS
                ? new ArrayList<Player>(Bukkit.getOnlinePlayers()) : null;
        if (!grant(player, voucher, giveAllRecipients)) {
            noteJournal(transaction, "REWARD_GRANT_FAILED_AFTER_CLAIM");
            player.sendMessage(ChatColor.RED + "Gutschein konnte nicht vergeben werden. Bitte einem Admin melden: " + serial);
            SoundFeedback.error(player);
            return;
        }
        if (!persistReward(player, voucher, giveAllRecipients)) {
            noteJournal(transaction, "REWARD_DURABLE_COMMIT_FAILED_AFTER_CLAIM");
            player.sendMessage(ChatColor.RED + "Gutschein-Reward konnte nicht sicher gespeichert werden. Bitte Staff kontaktieren.");
            SoundFeedback.error(player);
            return;
        }

        consumeMatchingSerial(player, serial);
        if (!savePlayerData(player)) {
            noteJournal(transaction, "VOUCHER_ITEM_REMOVAL_PLAYER_SAVE_FAILED");
            player.sendMessage(ChatColor.RED + "Reward ist gespeichert, aber dein Voucher-Inventar muss durch Staff geprueft werden.");
            SoundFeedback.error(player);
            return;
        }
        finishSuccessfulRedeem(player, voucher, maxClaims, transaction);
    }

    private void grantPermissionVoucherDurably(final Player player,
                                                final VoucherItemCodec.DecodedVoucher voucher,
                                                final int maxClaims,
                                                final UUID transaction) {
        final UUID playerId = player.getUniqueId();
        final String playerName = player.getName();
        final String actor = "VOUCHER:" + voucher.getSerial();
        CompletableFuture<VoucherPermissionService.GrantStatus> future;
        if (voucher.getType() == VoucherItemCodec.VoucherType.PERMISSION) {
            future = core.getVoucherPermissionService().grantDurably(playerId, voucher.getTarget(), actor);
        } else {
            future = core.getVoucherPermissionService().grantPrefixDurably(playerId, voucher.getTarget(), actor);
        }
        future.whenComplete((status, error) -> Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (error != null || status != VoucherPermissionService.GrantStatus.GRANTED) {
                    noteJournal(transaction, "LUCKPERMS_DURABLE_GRANT_FAILED_AFTER_CLAIM");
                    Player online = Bukkit.getPlayer(playerId);
                    if (online != null) {
                        online.sendMessage(ChatColor.RED + "Permission-Reward konnte nicht sicher gespeichert werden. Bitte Staff kontaktieren.");
                        SoundFeedback.error(online);
                    }
                    return;
                }

                Player online = Bukkit.getPlayer(playerId);
                if (online != null) {
                    consumeMatchingSerial(online, voucher.getSerial());
                    if (!savePlayerData(online)) {
                        noteJournal(transaction, "PERMISSION_VOUCHER_ITEM_REMOVAL_SAVE_FAILED");
                        online.sendMessage(ChatColor.RED + "Permission ist gespeichert, aber dein Voucher-Inventar muss geprueft werden.");
                        return;
                    }
                }
                logRedeem(playerId, playerName, voucher, maxClaims);
                if (!closeJournal(transaction)) {
                    Player current = Bukkit.getPlayer(playerId);
                    if (current != null) current.sendMessage(ChatColor.YELLOW + "Reward ist gespeichert, bleibt aber bis zur Staff-Pruefung markiert.");
                    return;
                }
                Player current = Bukkit.getPlayer(playerId);
                if (current != null) {
                    current.sendMessage(ChatColor.GREEN + "Gutschein erfolgreich eingeloest!");
                    SoundFeedback.reward(current);
                }
            }
        }));
    }

    private boolean persistReward(Player player, VoucherItemCodec.DecodedVoucher voucher, List<Player> giveAllRecipients) {
        switch (voucher.getType()) {
            case RANK:
            case RANKUP:
            case COINS:
                return core.getEconomyService().persistNow(player.getUniqueId());
            case KIT:
                return savePlayerData(player);
            case GIVEALL_COINS:
                if (giveAllRecipients == null) return false;
                for (Player recipient : giveAllRecipients) {
                    if (!core.getEconomyService().persistNow(recipient.getUniqueId())) return false;
                }
                return true;
            default:
                return true;
        }
    }

    private boolean savePlayerData(Player player) {
        if (player == null || !player.isOnline()) return false;
        try {
            player.saveData();
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Voucher-Playerdaten konnten nicht synchron gespeichert werden: "
                    + player.getUniqueId() + " / " + ex.getMessage());
            return false;
        }
    }

    private void finishSuccessfulRedeem(Player player, VoucherItemCodec.DecodedVoucher voucher,
                                        int maxClaims, UUID transaction) {
        logRedeem(player.getUniqueId(), player.getName(), voucher, maxClaims);
        if (!closeJournal(transaction)) {
            player.sendMessage(ChatColor.YELLOW + "Reward ist gespeichert, bleibt aber bis zur Staff-Pruefung markiert.");
            return;
        }
        if (voucher.getType() != VoucherItemCodec.VoucherType.GIVEALL_COINS) {
            player.sendMessage(ChatColor.GREEN + "Gutschein erfolgreich eingeloest!");
        }
        SoundFeedback.reward(player);
    }

    private void logRedeem(UUID playerId, String playerName, VoucherItemCodec.DecodedVoucher voucher, int maxClaims) {
        core.getLoggingService().log(new AuditEvent(AuditEventType.VOUCHER_REDEEMED,
                playerId, playerName, null,
                "serial=" + voucher.getSerial() + ", claim=" + store.getRedeemedClaims(voucher.getSerial()) + "/" + maxClaims
                        + ", type=" + voucher.getType() + ", target=" + voucher.getTarget()));
    }

    private void completeJournal(UUID transaction, String fallbackReason) {
        if (rewardJournal == null || transaction == null) return;
        if (!rewardJournal.complete(transaction)) rewardJournal.noteFailure(transaction, fallbackReason);
    }

    private boolean closeJournal(UUID transaction) {
        if (rewardJournal == null || transaction == null) return true;
        if (rewardJournal.complete(transaction)) return true;
        rewardJournal.noteFailure(transaction, "REWARD_COMMITTED_BUT_JOURNAL_CLOSE_FAILED");
        return false;
    }

    private void noteJournal(UUID transaction, String reason) {
        if (rewardJournal != null && transaction != null) rewardJournal.noteFailure(transaction, reason);
    }

    private int maxClaims(VoucherItemCodec.DecodedVoucher voucher) {
        if (voucher == null) return 0;
        if (!voucher.isStackable()) return 1;
        IssuedItemStore issued = IssuedItemStore.active();
        return issued == null ? 0 : issued.getVoucherMaxClaims(voucher.getSerial(), voucher.getType(), voucher.getTarget());
    }

    private boolean canRedeem(Player player, VoucherItemCodec.DecodedVoucher voucher) {
        switch (voucher.getType()) {
            case RANK:
                Rank rank = parseRank(voucher.getTarget());
                if (rank == null) return invalid(player);
                Rank current = core.getRankService().getRank(player.getUniqueId());
                if (current.isAtLeast(rank)) {
                    player.sendMessage(ChatColor.YELLOW + "Du besitzt diesen oder einen hoeheren Rang bereits.");
                    SoundFeedback.warning(player);
                    return false;
                }
                return true;
            case RANKUP:
                if (nextRank(core.getRankService().getRank(player.getUniqueId())) == null) {
                    player.sendMessage(ChatColor.YELLOW + "Du besitzt bereits den hoechsten SkyKings-Rang.");
                    SoundFeedback.warning(player);
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
            case COINS:
                long amount = parseCoinAmount(voucher.getTarget());
                if (amount <= 0L) return invalid(player);
                if (!BalanceSettlementGuard.canAdd(core.getEconomyService().getBalance(player.getUniqueId()), amount)) {
                    player.sendMessage(ChatColor.RED + "Dein Coin-Kontostand ist zu hoch fuer diesen Gutschein.");
                    SoundFeedback.error(player);
                    return false;
                }
                return true;
            case GIVEALL_COINS:
                long giveAllAmount = parseCoinAmount(voucher.getTarget());
                if (giveAllAmount <= 0L) return invalid(player);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!BalanceSettlementGuard.canAdd(core.getEconomyService().getBalance(online.getUniqueId()), giveAllAmount)) {
                        player.sendMessage(ChatColor.RED + "GiveAll abgebrochen: Mindestens ein Online-Kontostand ist zu hoch.");
                        SoundFeedback.error(player);
                        return false;
                    }
                }
                return true;
            default:
                return invalid(player);
        }
    }

    private boolean grant(Player player, VoucherItemCodec.DecodedVoucher voucher, List<Player> giveAllRecipients) {
        switch (voucher.getType()) {
            case RANK:
                Rank rank = parseRank(voucher.getTarget());
                if (rank == null) return false;
                core.getRankService().setRank(player.getUniqueId(), rank, "VOUCHER:" + voucher.getSerial());
                return true;
            case RANKUP:
                Rank current = core.getRankService().getRank(player.getUniqueId());
                Rank next = nextRank(current);
                if (next == null) return false;
                core.getRankService().setRank(player.getUniqueId(), next, "RANKUP_VOUCHER:" + voucher.getSerial());
                Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "RANKUP! "
                        + ChatColor.WHITE + player.getName() + ChatColor.YELLOW + " ist durch einen ultra-seltenen Gutschein jetzt "
                        + ChatColor.GOLD + next.name() + ChatColor.YELLOW + "!");
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
            case COINS:
                long amount = parseCoinAmount(voucher.getTarget());
                if (amount <= 0L) return false;
                core.getEconomyService().deposit(player.getUniqueId(), amount, "VOUCHER",
                        "Coin-Gutschein " + voucher.getSerial());
                player.sendMessage(ChatColor.GOLD + "+" + UiFormat.coins(amount));
                return true;
            case GIVEALL_COINS:
                long giveAllAmount = parseCoinAmount(voucher.getTarget());
                if (giveAllAmount <= 0L || giveAllRecipients == null) return false;
                int recipients = 0;
                for (Player online : giveAllRecipients) {
                    core.getEconomyService().deposit(online.getUniqueId(), giveAllAmount, "VOUCHER_GIVEALL",
                            "GiveAll von " + player.getName() + " / " + voucher.getSerial());
                    online.sendMessage(ChatColor.GOLD + "+" + UiFormat.coins(giveAllAmount)
                            + ChatColor.GRAY + " durch einen GiveAll-Gutschein von " + ChatColor.WHITE + player.getName());
                    SoundFeedback.notify(online);
                    recipients++;
                }
                Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + "GIVEALL "
                        + ChatColor.GRAY + "| " + ChatColor.WHITE + player.getName()
                        + ChatColor.GRAY + " hat " + ChatColor.GOLD + UiFormat.coins(giveAllAmount)
                        + ChatColor.GRAY + " an alle " + ChatColor.WHITE + recipients + ChatColor.GRAY + " Online-Spieler verteilt.");
                return true;
            default:
                return false;
        }
    }

    private Rank nextRank(Rank current) {
        if (current == null) return Rank.SPIELER;
        Rank[] values = Rank.values();
        int next = current.ordinal() + 1;
        return next >= values.length ? null : values[next];
    }

    private String nextRankName(Player player) {
        Rank next = nextRank(core.getRankService().getRank(player.getUniqueId()));
        return next == null ? "Hoechster Rang erreicht" : next.name();
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
                    SoundFeedback.error(player);
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

    private long parseCoinAmount(String raw) {
        try {
            long amount = Long.parseLong(raw);
            return amount > 0L && amount <= MAX_COIN_VOUCHER ? amount : -1L;
        } catch (NumberFormatException ex) { return -1L; }
    }

    private boolean invalid(Player player) {
        player.sendMessage(ChatColor.RED + "Dieser Gutschein enthaelt ein ungueltiges Ziel.");
        SoundFeedback.error(player);
        return false;
    }

    private boolean hasMatchingSerial(Player player, UUID serial) {
        if (player == null || serial == null) return false;
        for (ItemStack current : player.getInventory().getContents()) {
            VoucherItemCodec.DecodedVoucher decoded = codec.decode(current);
            if (decoded != null && serial.equals(decoded.getSerial())) return true;
        }
        return false;
    }

    private void consumeMatchingSerial(Player player, UUID serial) {
        if (player == null || serial == null) return;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            VoucherItemCodec.DecodedVoucher decoded = codec.decode(current);
            if (decoded == null || !serial.equals(decoded.getSerial())) continue;
            if (current.getAmount() <= 1) player.getInventory().setItem(slot, null);
            else {
                ItemStack reduced = current.clone();
                reduced.setAmount(current.getAmount() - 1);
                player.getInventory().setItem(slot, reduced);
            }
            player.updateInventory();
            return;
        }
    }
}
