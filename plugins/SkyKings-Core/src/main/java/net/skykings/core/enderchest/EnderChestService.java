package net.skykings.core.enderchest;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SkyKings Enderchest: 54er GUI mit 45 Lager-Slots und einer Navigationszeile.
 * Bis zu 8 persistente Seiten; Seiten kommen ueber Rang oder koennen mit Coins gekauft werden.
 */
public final class EnderChestService implements Listener {

    public static final String ACCESS_PERMISSION = "skykings.perk.enderchest";
    private static final int GUI_SIZE = 54;
    private static final int STORAGE_SLOTS = 45;
    private static final int MAX_PAGES = 8;

    private static final long[] PAGE_PRICES = {
            0L,
            0L,
            2_500_000L,
            7_500_000L,
            20_000_000L,
            50_000_000L,
            100_000_000L,
            200_000_000L,
            400_000_000L
    };

    private final JavaPlugin plugin;
    private final RankService rankService;
    private final EconomyService economyService;
    private final GameplaySettlementJournal settlementJournal;
    private final File file;
    private final YamlConfiguration data;

    public EnderChestService(JavaPlugin plugin, RankService rankService, EconomyService economyService) {
        this(plugin, rankService, economyService, resolveJournal(plugin));
    }

    EnderChestService(JavaPlugin plugin, RankService rankService, EconomyService economyService,
                      GameplaySettlementJournal settlementJournal) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.economyService = economyService;
        this.settlementJournal = settlementJournal;
        this.file = new File(plugin.getDataFolder(), "enderchests.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private static GameplaySettlementJournal resolveJournal(JavaPlugin plugin) {
        try {
            GameplaySettlementJournal existing = GameplaySettlementJournal.active();
            return existing != null ? existing : new GameplaySettlementJournal(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public boolean hasAccess(Player player) {
        return player.hasPermission(ACCESS_PERMISSION)
                || rankService.hasAtLeast(player.getUniqueId(), Rank.GOLD);
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int requestedPage) {
        if (!hasAccess(player)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens Gold oder das Enderchest-Recht.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.75F);
            return;
        }

        int unlocked = getUnlockedPages(player.getUniqueId());
        int page = Math.max(1, Math.min(requestedPage, unlocked));
        PageHolder holder = new PageHolder(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE,
                ChatColor.DARK_PURPLE + "Enderchest " + ChatColor.GRAY + "- Seite " + page + "/" + unlocked);
        holder.inventory = inventory;

        ItemStack[] items = loadPage(player.getUniqueId(), page);
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            if (items[slot] != null) inventory.setItem(slot, items[slot]);
        }
        decorate(player, inventory, page, unlocked);
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.55F, 1.15F);
    }

    public int getUnlockedPages(UUID uuid) {
        return Math.min(MAX_PAGES, Math.max(getRankPages(rankService.getRank(uuid)), getPurchasedPageCap(uuid)));
    }

    private int getRankPages(Rank rank) {
        if (rank == null) return 1;
        switch (rank) {
            case KNIGHT: return 2;
            case PHOENIX: return 3;
            case ETERNAL: return 4;
            case EXILE: return 5;
            case ENDLING: return 6;
            case KING: return 8;
            default: return 1;
        }
    }

    private int getPurchasedPageCap(UUID uuid) {
        return Math.max(1, data.getInt("players." + uuid + ".purchased-page-cap", 1));
    }

    private boolean setPurchasedPageCap(UUID uuid, int value) {
        String path = "players." + uuid + ".purchased-page-cap";
        int previous = getPurchasedPageCap(uuid);
        data.set(path, Math.max(1, Math.min(MAX_PAGES, value)));
        if (saveFile()) return true;
        data.set(path, previous);
        return false;
    }

    private void decorate(Player player, Inventory inventory, int page, int unlocked) {
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.<String>emptyList());
        for (int slot = 45; slot < 54; slot++) inventory.setItem(slot, filler);

        if (page > 1) {
            inventory.setItem(45, item(Material.ARROW, (short) 0, ChatColor.YELLOW + "Vorherige Seite",
                    Arrays.asList(ChatColor.GRAY + "Zu Seite " + (page - 1))));
        }

        inventory.setItem(49, item(Material.ENDER_CHEST, (short) 0,
                ChatColor.LIGHT_PURPLE + "Deine Enderchest",
                Arrays.asList(
                        ChatColor.GRAY + "Seite: " + ChatColor.WHITE + page + "/" + unlocked,
                        ChatColor.GRAY + "Freigeschaltet: " + ChatColor.WHITE + unlocked + "/" + MAX_PAGES,
                        ChatColor.DARK_GRAY + "45 Lagerplaetze pro Seite"
                )));

        if (page < unlocked) {
            inventory.setItem(53, item(Material.ARROW, (short) 0, ChatColor.YELLOW + "Naechste Seite",
                    Arrays.asList(ChatColor.GRAY + "Zu Seite " + (page + 1))));
        } else if (unlocked < MAX_PAGES) {
            int target = unlocked + 1;
            long price = PAGE_PRICES[target];
            inventory.setItem(53, item(Material.GOLD_INGOT, (short) 0,
                    ChatColor.GOLD + "Seite " + target + " kaufen",
                    Arrays.asList(
                            ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + format(price) + " Coins",
                            ChatColor.GRAY + "Kontostand: " + ChatColor.WHITE + format(economyService.getBalance(player.getUniqueId())),
                            "",
                            ChatColor.GREEN + "Klicken zum permanenten Freischalten"
                    )));
        } else {
            inventory.setItem(53, item(Material.NETHER_STAR, (short) 0,
                    ChatColor.GOLD + "Maximale Seitenzahl erreicht",
                    Arrays.asList(ChatColor.GRAY + "Du besitzt alle " + MAX_PAGES + " Seiten.")));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PageHolder)) return;
        PageHolder holder = (PageHolder) event.getInventory().getHolder();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!player.getUniqueId().equals(holder.owner)) {
            event.setCancelled(true);
            return;
        }

        int raw = event.getRawSlot();
        if (raw < 0) return;
        if (raw < STORAGE_SLOTS) return;
        if (raw >= GUI_SIZE) return;

        event.setCancelled(true);
        saveInventory(holder.owner, holder.page, event.getInventory());

        int unlocked = getUnlockedPages(holder.owner);
        if (raw == 45 && holder.page > 1) {
            player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 0.9F);
            open(player, holder.page - 1);
            return;
        }
        if (raw != 53) return;

        if (holder.page < unlocked) {
            player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.25F);
            open(player, holder.page + 1);
            return;
        }
        if (unlocked >= MAX_PAGES) {
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.35F, 1.6F);
            return;
        }

        purchaseNextPage(player, holder.owner, unlocked + 1);
    }

    private synchronized void purchaseNextPage(Player player, UUID owner, int target) {
        if (settlementJournal != null && settlementJournal.hasPendingFor(owner)) {
            player.sendMessage(ChatColor.RED + "Eine vorherige Gameplay-Transaktion muss zuerst von Staff geprueft werden.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.65F);
            return;
        }

        long price = PAGE_PRICES[target];
        UUID transaction = settlementJournal == null ? null : settlementJournal.begin(
                owner, "ENDERCHEST_PAGE_PURCHASE", String.valueOf(target), "price=" + price);
        if (settlementJournal != null && transaction == null) {
            player.sendMessage(ChatColor.RED + "Der Seitenkauf konnte nicht sicher vorbereitet werden.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.65F);
            return;
        }

        if (!economyService.withdraw(owner, price, player.getName(), "Enderchest Seite " + target)) {
            closeJournal(transaction, "WITHDRAW_REJECTED_BEFORE_MUTATION");
            player.sendMessage(ChatColor.RED + "Dir fehlen Coins fuer Enderchest-Seite " + target + ". Preis: "
                    + ChatColor.YELLOW + format(price) + " Coins");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.7F);
            return;
        }

        if (settlementJournal != null && !economyService.persistNow(owner)) {
            settlementJournal.noteFailure(transaction, "COIN_BALANCE_DURABLE_COMMIT_FAILED");
            player.sendMessage(ChatColor.RED + "Der Seitenkauf hat einen unklaren Speicherzustand erreicht. Bitte Staff informieren.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.65F);
            return;
        }

        if (!setPurchasedPageCap(owner, target)) {
            if (settlementJournal != null) settlementJournal.noteFailure(transaction, "PAGE_CAP_DURABLE_COMMIT_FAILED_AFTER_COIN_COMMIT");
            player.sendMessage(ChatColor.RED + "Der Seitenkauf hat einen unklaren Speicherzustand erreicht. Bitte Staff informieren.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.65F);
            return;
        }

        if (!closeJournal(transaction, "PURCHASE_COMMITTED_BUT_JOURNAL_CLOSE_FAILED")) {
            player.sendMessage(ChatColor.RED + "Die Seite wurde gespeichert, aber der Abschluss muss von Staff geprueft werden.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.65F);
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Enderchest-Seite " + target + " wurde permanent freigeschaltet.");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.45F);
        open(player, target);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof PageHolder)) return;
        for (Integer raw : event.getRawSlots()) {
            if (raw >= STORAGE_SLOTS && raw < GUI_SIZE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PageHolder)) return;
        PageHolder holder = (PageHolder) event.getInventory().getHolder();
        saveInventory(holder.owner, holder.page, event.getInventory());
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            player.playSound(player.getLocation(), Sound.CHEST_CLOSE, 0.45F, 1.1F);
        }
    }

    private ItemStack[] loadPage(UUID uuid, int page) {
        ItemStack[] items = new ItemStack[STORAGE_SLOTS];
        List<?> list = data.getList(pagePath(uuid, page));
        if (list == null) return items;
        for (int i = 0; i < Math.min(STORAGE_SLOTS, list.size()); i++) {
            Object value = list.get(i);
            if (value instanceof ItemStack) items[i] = ((ItemStack) value).clone();
        }
        return items;
    }

    private void saveInventory(UUID uuid, int page, Inventory inventory) {
        List<ItemStack> items = new ArrayList<ItemStack>(STORAGE_SLOTS);
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            items.add(stack == null ? null : stack.clone());
        }
        data.set(pagePath(uuid, page), items);
        saveFile();
    }

    private String pagePath(UUID uuid, int page) {
        return "players." + uuid + ".pages." + page;
    }

    private ItemStack item(Material material, short durability, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material, 1, durability);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }

    private boolean closeJournal(UUID transaction, String reason) {
        if (settlementJournal == null || transaction == null) return true;
        if (settlementJournal.complete(transaction)) return true;
        settlementJournal.noteFailure(transaction, reason);
        return false;
    }

    private boolean saveFile() {
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Konnte Plugin-Ordner fuer Enderchest nicht erstellen.");
                return false;
            }
            data.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte enderchests.yml nicht speichern.", e);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }

    public void shutdown() {
        saveFile();
    }

    private static final class PageHolder implements InventoryHolder {
        private final UUID owner;
        private final int page;
        private Inventory inventory;

        private PageHolder(UUID owner, int page) {
            this.owner = owner;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
