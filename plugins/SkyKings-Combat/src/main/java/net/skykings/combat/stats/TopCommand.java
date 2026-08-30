package net.skykings.combat.stats;

import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /top - ästhetische PvP-Leaderboards für Kills, Beststreak und K/D. */
public final class TopCommand implements CommandExecutor, Listener {

    private static final String ROOT_TITLE = ChatColor.DARK_GRAY + "SkyKings | Top";
    private static final String KILLS_TITLE = ChatColor.DARK_GRAY + "Top | Kills";
    private static final String STREAK_TITLE = ChatColor.DARK_GRAY + "Top | Beststreak";
    private static final String KD_TITLE = ChatColor.DARK_GRAY + "Top | K/D";

    private final PvpStatsService statsService;
    private final DecimalFormat kdFormat = new DecimalFormat("0.00");

    public TopCommand(PvpStatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfügbar.");
            return true;
        }
        openRoot((Player) sender);
        return true;
    }

    private void openRoot(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ROOT_TITLE);
        inv.setItem(11, icon(Material.DIAMOND_SWORD, ChatColor.RED + "Top Kills", ChatColor.GRAY + "Die gefährlichsten Spieler"));
        inv.setItem(13, icon(Material.BLAZE_POWDER, ChatColor.GOLD + "Top Beststreak", ChatColor.GRAY + "Die höchsten Killstreaks"));
        inv.setItem(15, icon(Material.NETHER_STAR, ChatColor.YELLOW + "Top K/D", ChatColor.GRAY + "Die stärksten K/D-Werte"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(ROOT_TITLE) && !title.equals(KILLS_TITLE) && !title.equals(STREAK_TITLE) && !title.equals(KD_TITLE)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (title.equals(ROOT_TITLE)) {
            if (event.getRawSlot() == 11) openLeaderboard(player, KILLS_TITLE, Comparator.comparingLong((Map.Entry<UUID, PvpStatsSnapshot> e) -> e.getValue().getKills()).reversed(), "Kills");
            else if (event.getRawSlot() == 13) openLeaderboard(player, STREAK_TITLE, Comparator.comparingInt((Map.Entry<UUID, PvpStatsSnapshot> e) -> e.getValue().getBestStreak()).reversed(), "Beststreak");
            else if (event.getRawSlot() == 15) openLeaderboard(player, KD_TITLE, Comparator.comparingDouble((Map.Entry<UUID, PvpStatsSnapshot> e) -> kd(e.getValue())).reversed(), "K/D");
        } else if (event.getRawSlot() == 49) {
            openRoot(player);
        }
    }

    private void openLeaderboard(Player player, String title,
                                 Comparator<Map.Entry<UUID, PvpStatsSnapshot>> comparator, String metric) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        List<Map.Entry<UUID, PvpStatsSnapshot>> entries = new ArrayList<Map.Entry<UUID, PvpStatsSnapshot>>(statsService.getAllStats().entrySet());
        entries.sort(comparator);
        int limit = Math.min(45, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, PvpStatsSnapshot> entry = entries.get(i);
            inv.setItem(i, playerHead(entry.getKey(), entry.getValue(), i + 1, metric));
        }
        inv.setItem(49, icon(Material.ARROW, ChatColor.YELLOW + "Zurück", ChatColor.GRAY + "Zur Übersicht"));
        player.openInventory(inv);
    }

    private ItemStack playerHead(UUID uuid, PvpStatsSnapshot stats, int position, String metric) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline != null && offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(name);
        meta.setDisplayName(ChatColor.GOLD + "#" + position + " " + ChatColor.YELLOW + name);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Kills: " + ChatColor.WHITE + stats.getKills());
        lore.add(ChatColor.GRAY + "Tode: " + ChatColor.WHITE + stats.getDeaths());
        lore.add(ChatColor.GRAY + "K/D: " + ChatColor.WHITE + kdFormat.format(kd(stats)));
        lore.add(ChatColor.GRAY + "Beststreak: " + ChatColor.WHITE + stats.getBestStreak());
        lore.add("");
        lore.add(ChatColor.AQUA + "Sortiert nach " + metric);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private double kd(PvpStatsSnapshot stats) {
        return stats.getDeaths() <= 0L ? (double) stats.getKills() : (double) stats.getKills() / (double) stats.getDeaths();
    }

    private ItemStack icon(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<String>();
        lore.add(loreLine);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
