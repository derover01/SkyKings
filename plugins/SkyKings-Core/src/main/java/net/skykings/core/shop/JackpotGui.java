package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Persistenter Coin-Jackpot. Reine Ingame-Economy, keine Echtgeld-Funktion. */
public final class JackpotGui {

    private static final long ROUND_TICKS = 20L * 60L * 10L;
    private static final double FEE_RATE = 0.05D;

    private final JavaPlugin plugin;
    private final GuiManager guiManager;
    private final EconomyService economyService;
    private final File file;
    private final Map<UUID, Long> entries = new LinkedHashMap<UUID, Long>();
    private final Random random = new Random();

    public JackpotGui(JavaPlugin plugin, GuiManager guiManager, EconomyService economyService) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.economyService = economyService;
        this.file = new File(plugin.getDataFolder(), "jackpot.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::draw, ROUND_TICKS, ROUND_TICKS);
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Jackpot", 27);
        long pot = pot();
        long own = entries.containsKey(player.getUniqueId()) ? entries.get(player.getUniqueId()) : 0L;
        double chance = pot <= 0L ? 0D : (100D * own / pot);

        gui.setItem(4, named(Material.NETHER_STAR, ChatColor.GOLD + "Aktueller Jackpot",
                ChatColor.GRAY + "Pot: " + ChatColor.YELLOW + format(pot) + " Coins",
                ChatColor.GRAY + "Dein Einsatz: " + ChatColor.WHITE + format(own),
                ChatColor.GRAY + "Deine Chance: " + ChatColor.AQUA + String.format(java.util.Locale.US, "%.2f%%", chance),
                ChatColor.DARK_GRAY + "Ziehung automatisch alle 10 Minuten"));

        addEntry(gui, 10, Material.IRON_INGOT, 50_000L);
        addEntry(gui, 13, Material.GOLD_INGOT, 250_000L);
        addEntry(gui, 16, Material.DIAMOND, 1_000_000L);

        gui.setItem(22, named(Material.PAPER, ChatColor.YELLOW + "Regeln",
                ChatColor.GRAY + "Dein Anteil am Pot bestimmt deine Gewinnchance.",
                ChatColor.GRAY + "Mindestens 2 Teilnehmer für eine Ziehung.",
                ChatColor.GRAY + "5% des Pots werden als Economy-Sink entfernt."));

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void addEntry(GuiSession gui, int slot, Material icon, long amount) {
        gui.setItem(slot, named(icon, ChatColor.GREEN + "+ " + format(amount) + " Coins",
                ChatColor.GRAY + "In den aktuellen Jackpot einzahlen",
                ChatColor.YELLOW + "Klicken zum Teilnehmen"),
                (p,e,s) -> enter(p, amount));
    }

    private void enter(Player player, long amount) {
        if (!economyService.withdraw(player.getUniqueId(), amount, "JACKPOT", "Jackpot entry")) {
            player.sendMessage(ChatColor.RED + "Du hast nicht genug Coins für diesen Einsatz.");
            SoundFeedback.error(player);
            return;
        }
        long current = entries.containsKey(player.getUniqueId()) ? entries.get(player.getUniqueId()) : 0L;
        entries.put(player.getUniqueId(), current + amount);
        save();
        player.sendMessage(ChatColor.GREEN + "Jackpot-Einsatz: " + ChatColor.GOLD + format(amount) + " Coins");
        SoundFeedback.confirm(player);
        open(player);
    }

    private void draw() {
        if (entries.size() < 2) return;
        long pot = pot();
        if (pot <= 0L) return;

        long roll = nextLong(pot);
        long cursor = 0L;
        UUID winner = null;
        for (Map.Entry<UUID, Long> entry : entries.entrySet()) {
            cursor += entry.getValue();
            if (roll < cursor) {
                winner = entry.getKey();
                break;
            }
        }
        if (winner == null) return;

        long payout = Math.max(1L, Math.round(pot * (1D - FEE_RATE)));
        economyService.deposit(winner, payout, "JACKPOT", "Jackpot win");
        OfflinePlayer offline = Bukkit.getOfflinePlayer(winner);
        String name = offline.getName() == null ? winner.toString().substring(0, 8) : offline.getName();
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "JACKPOT! " + ChatColor.YELLOW
                + name + ChatColor.GRAY + " gewinnt " + ChatColor.GOLD + format(payout) + " Coins!");
        Player online = Bukkit.getPlayer(winner);
        if (online != null) SoundFeedback.reward(online);

        entries.clear();
        save();
    }

    private long nextLong(long bound) {
        if (bound <= 1L) return 0L;
        long bits;
        long value;
        do {
            bits = random.nextLong() & Long.MAX_VALUE;
            value = bits % bound;
        } while (bits - value + (bound - 1L) < 0L);
        return value;
    }

    private long pot() {
        long total = 0L;
        for (Long value : entries.values()) {
            if (value == null || value <= 0L) continue;
            if (Long.MAX_VALUE - total < value) return Long.MAX_VALUE;
            total += value;
        }
        return total;
    }

    private void load() {
        entries.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("entries") == null) return;
        for (String key : yaml.getConfigurationSection("entries").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long amount = yaml.getLong("entries." + key, 0L);
                if (amount > 0L) entries.put(uuid, amount);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ungültiger Jackpot-Spieler in jackpot.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : entries.entrySet()) yaml.set("entries." + entry.getKey(), entry.getValue());
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("jackpot.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }
}
