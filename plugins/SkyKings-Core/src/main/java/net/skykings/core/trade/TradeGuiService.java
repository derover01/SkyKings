package net.skykings.core.trade;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 6x9 Zwei-Spieler-Trade-GUI mit Item-Escrow, Coins, Doppelbestaetigung und 3s Sicherheitscountdown. */
public final class TradeGuiService implements Listener {

    private static final int[] SELF_SLOTS = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};
    private static final int[] OTHER_SLOTS = {27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44};

    private final JavaPlugin plugin;
    private final TradeService tradeService;
    private final EconomyService economyService;
    private final LoggingService loggingService;
    private final Map<UUID, Inventory> views = new HashMap<UUID, Inventory>();

    public TradeGuiService(JavaPlugin plugin, TradeService tradeService, EconomyService economyService, LoggingService loggingService) {
        this.plugin = plugin;
        this.tradeService = tradeService;
        this.economyService = economyService;
        this.loggingService = loggingService;
    }

    public void openForBoth(TradeSession session) {
        Player left = Bukkit.getPlayer(session.getLeft().getPlayer());
        Player right = Bukkit.getPlayer(session.getRight().getPlayer());
        if (left == null || right == null) {
            cancel(session, ChatColor.RED + "Trade abgebrochen: Spieler offline.");
            return;
        }
        open(left, session);
        open(right, session);
        refresh(session);
    }

    private void open(Player player, TradeSession session) {
        TradeHolder holder = new TradeHolder(session.getId(), player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ChatColor.DARK_GRAY + "SkyKings | Trade");
        holder.inventory = inventory;
        views.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

    private void refresh(TradeSession session) {
        renderFor(session.getLeft().getPlayer(), session);
        renderFor(session.getRight().getPlayer(), session);
    }

    private void renderFor(UUID viewerId, TradeSession session) {
        Inventory inv = views.get(viewerId);
        Player viewer = Bukkit.getPlayer(viewerId);
        if (inv == null || viewer == null) return;
        TradeOffer self = session.offerOf(viewerId);
        TradeOffer other = session.otherOf(viewerId);
        if (self == null || other == null) return;

        inv.clear();
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 18; i <= 26; i++) inv.setItem(i, filler);
        for (int i = 45; i <= 53; i++) inv.setItem(i, filler);
        for (int i = 0; i < self.getItems().size() && i < SELF_SLOTS.length; i++) inv.setItem(SELF_SLOTS[i], self.getItems().get(i));
        for (int i = 0; i < other.getItems().size() && i < OTHER_SLOTS.length; i++) inv.setItem(OTHER_SLOTS[i], other.getItems().get(i));

        Player otherPlayer = Bukkit.getPlayer(other.getPlayer());
        String otherName = otherPlayer != null ? otherPlayer.getName() : "Spieler";
        inv.setItem(18, named(Material.CHEST, (short) 0, ChatColor.AQUA + "Dein Angebot", ChatColor.GRAY + "Oben links: deine Items"));
        inv.setItem(26, named(Material.CHEST, (short) 0, ChatColor.AQUA + otherName, ChatColor.GRAY + "Unten: Angebot des anderen"));
        inv.setItem(20, named(Material.REDSTONE, (short) 0, ChatColor.RED + "-100.000 Coins"));
        inv.setItem(21, named(Material.REDSTONE_BLOCK, (short) 0, ChatColor.RED + "-1.000.000 Coins"));
        inv.setItem(22, named(Material.GOLD_INGOT, (short) 0, ChatColor.GOLD + "Dein Coin-Angebot: " + format(self.getCoins()), ChatColor.GRAY + "Kontostand: " + format(economyService.getBalance(viewerId))));
        inv.setItem(23, named(Material.EMERALD, (short) 0, ChatColor.GREEN + "+100.000 Coins"));
        inv.setItem(24, named(Material.EMERALD_BLOCK, (short) 0, ChatColor.GREEN + "+1.000.000 Coins"));
        inv.setItem(48, confirm(self.isAccepted(), "Du"));
        inv.setItem(50, confirm(other.isAccepted(), otherName));
        inv.setItem(49, named(Material.WATCH, (short) 0, ChatColor.YELLOW + "Trade-Status",
                session.bothAccepted() ? ChatColor.GREEN + "Beide bestaetigt - 3 Sekunden..." : ChatColor.GRAY + "Beide muessen bestaetigen."));
        inv.setItem(53, named(Material.BARRIER, (short) 0, ChatColor.RED + "Trade abbrechen"));
        viewer.updateInventory();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        TradeSession session = tradeService.get(player.getUniqueId());
        if (session == null || session.isFinished()) return;
        TradeOffer self = session.offerOf(player.getUniqueId());
        if (self == null) return;

        int raw = event.getRawSlot();
        if (raw >= 54) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || self.getItems().size() >= SELF_SLOTS.length) return;
            List<ItemStack> updated = new ArrayList<ItemStack>(self.getItems());
            updated.add(clicked.clone());
            self.setItems(updated);
            event.getClickedInventory().setItem(event.getSlot(), null);
            resetAcceptance(session);
            refresh(session);
            return;
        }

        int offerIndex = indexOf(SELF_SLOTS, raw);
        if (offerIndex >= 0 && offerIndex < self.getItems().size()) {
            List<ItemStack> updated = new ArrayList<ItemStack>(self.getItems());
            ItemStack returned = updated.remove(offerIndex);
            self.setItems(updated);
            resetAcceptance(session);
            giveOrDrop(player, returned);
            refresh(session);
            return;
        }

        if (raw == 20) changeCoins(player, session, self, -100000L);
        else if (raw == 21) changeCoins(player, session, self, -1000000L);
        else if (raw == 23) changeCoins(player, session, self, 100000L);
        else if (raw == 24) changeCoins(player, session, self, 1000000L);
        else if (raw == 48) {
            session.bumpAcceptanceRevision();
            self.setAccepted(!self.isAccepted());
            refresh(session);
            if (session.bothAccepted()) startCountdown(session);
        } else if (raw == 53) {
            cancel(session, ChatColor.RED + "Trade wurde abgebrochen.");
        }
    }

    private void changeCoins(Player player, TradeSession session, TradeOffer self, long delta) {
        long next = TradeSettlementGuard.adjustOffer(self.getCoins(), delta,
                economyService.getBalance(player.getUniqueId()));
        if (next == self.getCoins()) return;
        self.setCoins(next);
        resetAcceptance(session);
        refresh(session);
    }

    /** Jede Angebotsaenderung invalidiert beide alten Zusagen und alle alten Countdown-Tasks. */
    private void resetAcceptance(TradeSession session) {
        if (session == null) return;
        session.bumpAcceptanceRevision();
        session.getLeft().setAccepted(false);
        session.getRight().setAccepted(false);
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TradeHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeHolder)) return;
        UUID uuid = event.getPlayer().getUniqueId();
        TradeSession session = tradeService.get(uuid);
        views.remove(uuid);
        if (session != null && !session.isFinished()) cancel(session, ChatColor.RED + "Trade wurde abgebrochen.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TradeSession session = tradeService.get(event.getPlayer().getUniqueId());
        if (session != null && !session.isFinished()) cancel(session, ChatColor.RED + "Trade abgebrochen: Spieler offline.");
    }

    private void startCountdown(final TradeSession session) {
        final long revision = session.getAcceptanceRevision();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!session.isFinished() && session.bothAccepted()
                    && session.getAcceptanceRevision() == revision) complete(session);
        }, 60L);
    }

    private void complete(TradeSession session) {
        if (session == null || session.isFinished() || !session.bothAccepted()) return;
        Player left = Bukkit.getPlayer(session.getLeft().getPlayer());
        Player right = Bukkit.getPlayer(session.getRight().getPlayer());
        if (left == null || right == null) { cancel(session, ChatColor.RED + "Trade abgebrochen: Spieler offline."); return; }
        TradeOffer a = session.getLeft();
        TradeOffer b = session.getRight();

        long leftBalance = economyService.getBalance(a.getPlayer());
        long rightBalance = economyService.getBalance(b.getPlayer());
        if (leftBalance < a.getCoins() || rightBalance < b.getCoins()) {
            resetAcceptance(session);
            left.sendMessage(ChatColor.RED + "Trade gestoppt: Ein Kontostand hat sich veraendert.");
            right.sendMessage(ChatColor.RED + "Trade gestoppt: Ein Kontostand hat sich veraendert.");
            refresh(session);
            return;
        }
        if (!TradeSettlementGuard.canSettle(leftBalance, a.getCoins(), rightBalance, b.getCoins())) {
            resetAcceptance(session);
            left.sendMessage(ChatColor.RED + "Trade gestoppt: Die Coin-Auszahlung wuerde einen Kontostand ueberlaufen lassen.");
            right.sendMessage(ChatColor.RED + "Trade gestoppt: Die Coin-Auszahlung wuerde einen Kontostand ueberlaufen lassen.");
            refresh(session);
            return;
        }
        if (!canFit(left, b.getItems()) || !canFit(right, a.getItems())) {
            resetAcceptance(session);
            left.sendMessage(ChatColor.RED + "Trade gestoppt: Nicht genug Inventarplatz.");
            right.sendMessage(ChatColor.RED + "Trade gestoppt: Nicht genug Inventarplatz.");
            refresh(session);
            return;
        }

        if (a.getCoins() > 0 && !economyService.withdraw(a.getPlayer(), a.getCoins(), "TRADE", "Trade " + session.getId())) {
            resetAcceptance(session);
            left.sendMessage(ChatColor.RED + "Trade gestoppt: Coins konnten nicht reserviert werden.");
            right.sendMessage(ChatColor.RED + "Trade gestoppt: Coins konnten nicht reserviert werden.");
            refresh(session);
            return;
        }
        if (b.getCoins() > 0 && !economyService.withdraw(b.getPlayer(), b.getCoins(), "TRADE", "Trade " + session.getId())) {
            if (a.getCoins() > 0) economyService.deposit(a.getPlayer(), a.getCoins(), "TRADE_ROLLBACK", "Trade rollback " + session.getId());
            resetAcceptance(session);
            left.sendMessage(ChatColor.RED + "Trade gestoppt: Coins konnten nicht reserviert werden.");
            right.sendMessage(ChatColor.RED + "Trade gestoppt: Coins konnten nicht reserviert werden.");
            refresh(session);
            return;
        }
        if (b.getCoins() > 0) economyService.deposit(a.getPlayer(), b.getCoins(), "TRADE", "Trade " + session.getId());
        if (a.getCoins() > 0) economyService.deposit(b.getPlayer(), a.getCoins(), "TRADE", "Trade " + session.getId());
        for (ItemStack item : b.getItems()) giveOrDrop(left, item.clone());
        for (ItemStack item : a.getItems()) giveOrDrop(right, item.clone());

        session.setFinished(true);
        tradeService.finish(session);
        views.remove(left.getUniqueId()); views.remove(right.getUniqueId());
        left.closeInventory(); right.closeInventory();
        left.sendMessage(ChatColor.GREEN + "Trade mit " + right.getName() + " erfolgreich abgeschlossen.");
        right.sendMessage(ChatColor.GREEN + "Trade mit " + left.getName() + " erfolgreich abgeschlossen.");
        loggingService.log(new AuditEvent(AuditEventType.TRADE_COMPLETE, a.getPlayer(), right.getName(), a.getCoins(), "trade=" + session.getId() + ", items=" + a.getItems().size()));
        loggingService.log(new AuditEvent(AuditEventType.TRADE_COMPLETE, b.getPlayer(), left.getName(), b.getCoins(), "trade=" + session.getId() + ", items=" + b.getItems().size()));
    }

    public void cancel(TradeSession session, String message) {
        if (session == null || session.isFinished()) return;
        session.setFinished(true);
        tradeService.finish(session);
        returnOffer(session.getLeft(), message);
        returnOffer(session.getRight(), message);
    }

    private void returnOffer(TradeOffer offer, String message) {
        Player player = Bukkit.getPlayer(offer.getPlayer());
        views.remove(offer.getPlayer());
        if (player == null) return;
        for (ItemStack item : offer.getItems()) giveOrDrop(player, item.clone());
        player.closeInventory();
        if (message != null) player.sendMessage(message);
    }

    private boolean canFit(Player player, List<ItemStack> items) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        for (ItemStack item : items) if (!temp.addItem(item.clone()).isEmpty()) return false;
        return true;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> rest = player.getInventory().addItem(item);
        for (ItemStack leftover : rest.values()) player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        player.updateInventory();
    }

    private int indexOf(int[] slots, int raw) {
        for (int i = 0; i < slots.length; i++) if (slots[i] == raw) return i;
        return -1;
    }

    private ItemStack confirm(boolean accepted, String who) {
        return named(accepted ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, (short) 0,
                accepted ? ChatColor.GREEN + who + ": bestaetigt" : ChatColor.RED + who + ": nicht bestaetigt",
                ChatColor.GRAY + "Klicken zum Umschalten");
    }

    private ItemStack named(Material material, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String format(long value) { return String.format("%,d", value).replace(',', '.'); }

    private static final class TradeHolder implements InventoryHolder {
        private final UUID sessionId;
        private final UUID viewer;
        private Inventory inventory;
        private TradeHolder(UUID sessionId, UUID viewer) { this.sessionId = sessionId; this.viewer = viewer; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
