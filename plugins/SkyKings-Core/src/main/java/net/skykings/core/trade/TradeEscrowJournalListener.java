package net.skykings.core.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Verdrahtet das persistente TradeEscrowJournal um die bestehende Trade-GUI herum.
 *
 * Die LOWEST-Handler schreiben zuerst einen transienten Write-Ahead-State. Die bestehenden
 * TradeGuiService-Handler mutieren danach das Inventar. MONITOR persistiert anschliessend die
 * Spielerdatei und committed erst dann ACTIVE. Ein Hard-Crash zwischen diesen Schritten endet
 * daher in REVIEW_REQUIRED statt in einer automatischen Doppel-Auszahlung.
 */
public final class TradeEscrowJournalListener implements Listener {
    private static final String TRADE_TITLE = ChatColor.DARK_GRAY + "SkyKings | Trade";

    private final JavaPlugin plugin;
    private final TradeService tradeService;
    private final TradeGuiService tradeGuiService;
    private final TradeEscrowJournal journal;
    private final Map<UUID, ClickTxn> clickTxns = new HashMap<UUID, ClickTxn>();
    private final Map<UUID, ReturnTxn> closeTxns = new HashMap<UUID, ReturnTxn>();
    private final Map<UUID, ReturnTxn> quitTxns = new HashMap<UUID, ReturnTxn>();
    private List<TradeSession> shutdownSessions = new ArrayList<TradeSession>();

    public TradeEscrowJournalListener(JavaPlugin plugin, TradeService tradeService,
                                      TradeGuiService tradeGuiService, TradeEscrowJournal journal) {
        this.plugin = plugin;
        this.tradeService = tradeService;
        this.tradeGuiService = tradeGuiService;
        this.journal = journal;

        // Bei /reload (nicht empfohlen) koennen Spieler bereits online sein. Ein normaler Start
        // recovered ueber PlayerJoinEvent, dieser Task deckt nur den Legacy-Sonderfall ab.
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                for (Player online : Bukkit.getOnlinePlayers()) recover(online);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClickBefore(InventoryClickEvent event) {
        if (!isTrade(event)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        TradeSession session = tradeService.get(player.getUniqueId());
        if (session == null || session.isFinished()) return;
        TradeOffer self = session.offerOf(player.getUniqueId());
        if (self == null) return;

        int raw = event.getRawSlot();
        if (raw >= 54) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == org.bukkit.Material.AIR || self.getItems().size() >= 18) return;
            boolean prepared = journal.prepareInbound(session, player.getUniqueId(), event.getSlot(), clicked.clone());
            clickTxns.put(player.getUniqueId(), new ClickTxn(session, Kind.INBOUND, prepared));
            return;
        }
        if (raw >= 0 && raw < 18 && raw < self.getItems().size()) {
            boolean prepared = journal.markReturning(session, "OFFER_ITEM_WITHDRAW");
            clickTxns.put(player.getUniqueId(), new ClickTxn(session, Kind.OUTBOUND, prepared));
            return;
        }
        if (raw == 20 || raw == 21 || raw == 23 || raw == 24) {
            clickTxns.put(player.getUniqueId(), new ClickTxn(session, Kind.COINS, true));
            return;
        }
        if (raw == 48) {
            clickTxns.put(player.getUniqueId(), new ClickTxn(session, Kind.ACCEPT, true));
            return;
        }
        if (raw == 53) {
            boolean prepared = journal.markReturning(session, "TRADE_CANCEL_BUTTON");
            clickTxns.put(player.getUniqueId(), new ClickTxn(session, Kind.CANCEL, prepared));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClickAfter(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ClickTxn txn = clickTxns.remove(player.getUniqueId());
        if (txn == null) return;

        switch (txn.kind) {
            case INBOUND:
                afterInbound(player, txn);
                break;
            case OUTBOUND:
                afterOutbound(player, txn);
                break;
            case COINS:
                TradeSession coinSession = tradeService.get(player.getUniqueId());
                if (coinSession == txn.session && !coinSession.isFinished() && !journal.saveActive(coinSession)) {
                    abortForJournalFailure(coinSession, "Coin-Angebot konnte nicht sicher gejournalt werden.");
                }
                break;
            case ACCEPT:
                scheduleSettlementGuard(txn.session);
                break;
            case CANCEL:
                savePlayers(txn.session);
                if (!journal.clear(txn.session.getId())) {
                    journal.markReviewRequired(txn.session.getId(), "CANCEL_COMPLETED_BUT_JOURNAL_CLEAR_FAILED");
                }
                break;
            default:
                break;
        }
    }

    private void afterInbound(Player player, ClickTxn txn) {
        TradeSession current = tradeService.get(player.getUniqueId());
        if (!txn.prepared) {
            if (current == txn.session && !current.isFinished()) {
                abortForJournalFailure(current, "Escrow-Write-Ahead konnte nicht gespeichert werden.");
            }
            return;
        }
        if (current != txn.session || current.isFinished()) {
            journal.markReviewRequired(txn.session.getId(), "INBOUND_SESSION_CHANGED_BEFORE_COMMIT");
            return;
        }
        try {
            player.saveData();
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Trade-Escrow: Player#saveData nach Item-Einlage fehlgeschlagen fuer " + player.getName(), ex);
            journal.markReviewRequired(txn.session.getId(), "PLAYER_SAVE_FAILED_AFTER_INBOUND");
            abortForJournalFailure(txn.session, "Spieler-Inventar konnte nicht sicher gespeichert werden.");
            return;
        }
        if (!journal.saveActive(txn.session)) {
            abortForJournalFailure(txn.session, "Escrow-Commit konnte nicht sicher gespeichert werden.");
        }
    }

    private void afterOutbound(Player player, ClickTxn txn) {
        TradeSession current = tradeService.get(player.getUniqueId());
        if (current != txn.session || current.isFinished()) return;
        try {
            player.saveData();
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Trade-Escrow: Player#saveData nach Item-Rueckgabe fehlgeschlagen fuer " + player.getName(), ex);
            journal.markReviewRequired(txn.session.getId(), "PLAYER_SAVE_FAILED_AFTER_OUTBOUND");
            abortForJournalFailure(txn.session, "Item-Rueckgabe konnte nicht sicher gespeichert werden.");
            return;
        }
        if (!journal.saveActive(txn.session)) {
            abortForJournalFailure(txn.session, "Escrow nach Item-Rueckgabe konnte nicht sicher gespeichert werden.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCloseBefore(InventoryCloseEvent event) {
        if (event.getView() == null || !TRADE_TITLE.equals(event.getView().getTitle())) return;
        UUID player = event.getPlayer().getUniqueId();
        TradeSession session = tradeService.get(player);
        if (session == null || session.isFinished()) return;
        boolean prepared = journal.markReturning(session, "INVENTORY_CLOSE_CANCEL");
        closeTxns.put(player, new ReturnTxn(session, prepared));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCloseAfter(InventoryCloseEvent event) {
        ReturnTxn txn = closeTxns.remove(event.getPlayer().getUniqueId());
        if (txn == null) return;
        finishReturnTxn(txn, "INVENTORY_CLOSE");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuitBefore(PlayerQuitEvent event) {
        TradeSession session = tradeService.get(event.getPlayer().getUniqueId());
        if (session == null || session.isFinished()) return;
        boolean prepared = journal.markReturning(session, "PLAYER_QUIT_CANCEL");
        quitTxns.put(event.getPlayer().getUniqueId(), new ReturnTxn(session, prepared));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuitAfter(PlayerQuitEvent event) {
        ReturnTxn txn = quitTxns.remove(event.getPlayer().getUniqueId());
        if (txn == null) return;
        finishReturnTxn(txn, "PLAYER_QUIT");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDisableBefore(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        Collection<TradeSession> active = tradeService.activeSessionsSnapshot();
        shutdownSessions = new ArrayList<TradeSession>(active);
        for (TradeSession session : shutdownSessions) {
            if (session != null && !session.isFinished()) journal.markReturning(session, "PLUGIN_DISABLE_CANCEL");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisableAfter(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        for (TradeSession session : shutdownSessions) {
            if (session == null) continue;
            savePlayers(session);
            if (!journal.clear(session.getId())) {
                journal.markReviewRequired(session.getId(), "SHUTDOWN_RETURN_COMPLETED_BUT_CLEAR_FAILED");
            }
        }
        shutdownSessions.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() { if (player.isOnline()) recover(player); }
        });
    }

    private void recover(Player player) {
        List<TradeEscrowJournal.RecoveryEntry> entries = journal.recoveriesFor(player.getUniqueId());
        if (entries.isEmpty()) return;
        int recoveredItems = 0;
        for (TradeEscrowJournal.RecoveryEntry entry : entries) {
            if (!canFit(player, entry.items)) {
                player.sendMessage(ChatColor.YELLOW + "Trade-Recovery wartet: Bitte schaffe Inventarplatz und joine erneut.");
                continue;
            }
            if (!journal.beginRecovery(entry.sessionId, player.getUniqueId())) continue;

            boolean delivered = true;
            for (ItemStack item : entry.items) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
                if (!leftovers.isEmpty()) {
                    delivered = false;
                    break;
                }
                recoveredItems += item.getAmount();
            }
            player.updateInventory();
            if (!delivered) {
                journal.markReviewRequired(entry.sessionId, "AUTO_RECOVERY_UNEXPECTED_INVENTORY_LEFTOVER");
                player.sendMessage(ChatColor.RED + "Trade-Recovery wurde gestoppt. Bitte Staff informieren.");
                continue;
            }
            try {
                player.saveData();
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE, "Trade-Escrow Auto-Recovery konnte Playerdaten nicht speichern: " + player.getName(), ex);
                journal.markReviewRequired(entry.sessionId, "AUTO_RECOVERY_PLAYER_SAVE_FAILED");
                continue;
            }
            if (!journal.completeRecovery(entry.sessionId, player.getUniqueId())) {
                journal.markReviewRequired(entry.sessionId, "AUTO_RECOVERY_COMMIT_FAILED");
            }
        }
        if (recoveredItems > 0) {
            player.sendMessage(ChatColor.YELLOW + "Trade-Recovery: " + ChatColor.WHITE + recoveredItems
                    + ChatColor.GRAY + " Item(s) aus einem vorigen Hard-Crash wurden sicher zurueckgegeben.");
        }
    }

    private void scheduleSettlementGuard(final TradeSession session) {
        if (session == null || session.isFinished() || !session.bothAccepted()) return;
        final long revision = session.getAcceptanceRevision();

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                if (session.isFinished() || !session.bothAccepted() || !session.isAcceptanceRevision(revision)) return;
                if (!journal.markSettling(session)) {
                    abortForJournalFailure(session, "Settlement-Journal konnte nicht vorbereitet werden.");
                }
            }
        }, 59L);

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                TradeEscrowJournal.State state = journal.stateOf(session.getId());
                if (state != TradeEscrowJournal.State.SETTLING) return;
                if (session.isFinished()) {
                    savePlayers(session);
                    if (!journal.clear(session.getId())) {
                        journal.markReviewRequired(session.getId(), "SETTLEMENT_COMPLETED_BUT_CLEAR_FAILED");
                    }
                    return;
                }

                // Alle absichtlich abgebrochenen Settlement-Pfade resetten beide Accepts. Bleibt
                // die Session dagegen accepted, ist der Settlement-Task unerwartet abgebrochen;
                // dann niemals automatisch ACTIVE markieren.
                if (!session.bothAccepted()) {
                    if (!journal.saveActive(session)) {
                        abortForJournalFailure(session, "Abgebrochenes Settlement konnte nicht auf ACTIVE zurueckgesetzt werden.");
                    }
                } else {
                    journal.markReviewRequired(session.getId(), "SETTLEMENT_DID_NOT_FINISH_NORMALLY");
                    tradeGuiService.cancel(session, ChatColor.RED + "Trade gestoppt: Settlement muss durch Staff geprueft werden.");
                    savePlayers(session);
                }
            }
        }, 61L);
    }

    private void finishReturnTxn(ReturnTxn txn, String reason) {
        savePlayers(txn.session);
        if (!txn.prepared) {
            journal.markReviewRequired(txn.session.getId(), reason + "_RETURN_WAS_NOT_PREPARED");
        }
        if (!journal.clear(txn.session.getId())) {
            journal.markReviewRequired(txn.session.getId(), reason + "_RETURN_COMPLETED_BUT_CLEAR_FAILED");
        }
    }

    private void abortForJournalFailure(TradeSession session, String reason) {
        if (session == null || session.isFinished()) return;
        journal.markReviewRequired(session.getId(), reason.replace(' ', '_').toUpperCase(java.util.Locale.ROOT));
        tradeGuiService.cancel(session, ChatColor.RED + "Trade aus Sicherheitsgruenden abgebrochen: " + ChatColor.GRAY + reason);
        savePlayers(session);
    }

    private void savePlayers(TradeSession session) {
        if (session == null) return;
        savePlayer(session.getLeft().getPlayer());
        savePlayer(session.getRight().getPlayer());
    }

    private void savePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        try {
            player.saveData();
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Trade-Escrow konnte Playerdaten nicht synchron speichern: " + uuid, ex);
        }
    }

    private boolean canFit(Player player, List<ItemStack> items) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        for (ItemStack item : items) {
            if (item != null && !temp.addItem(item.clone()).isEmpty()) return false;
        }
        return true;
    }

    private boolean isTrade(InventoryClickEvent event) {
        return event.getView() != null && TRADE_TITLE.equals(event.getView().getTitle());
    }

    private enum Kind { INBOUND, OUTBOUND, COINS, ACCEPT, CANCEL }

    private static final class ClickTxn {
        final TradeSession session;
        final Kind kind;
        final boolean prepared;
        ClickTxn(TradeSession session, Kind kind, boolean prepared) {
            this.session = session;
            this.kind = kind;
            this.prepared = prepared;
        }
    }

    private static final class ReturnTxn {
        final TradeSession session;
        final boolean prepared;
        ReturnTxn(TradeSession session, boolean prepared) {
            this.session = session;
            this.prepared = prepared;
        }
    }
}
