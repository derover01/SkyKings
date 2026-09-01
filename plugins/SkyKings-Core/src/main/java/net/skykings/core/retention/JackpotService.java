package net.skykings.core.retention;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Persistenter serverweiter Coin-Jackpot. Gewinnchance entspricht exakt dem Anteil am Pot.
 * Ausschliesslich SkyKings-Coins; keine Echtgeld-/Store-Waehrung.
 */
public final class JackpotService {
    private static final long ROUND_MILLIS = 10L * 60L * 1000L;
    private static final long RETRY_MILLIS = 5L * 60L * 1000L;
    private static final long MIN_ENTRY = 10_000L;
    private static final long MAX_ENTRY = 1_000_000L;
    private static final long[] QUICK_ENTRIES = {10_000L, 50_000L, 100_000L, 250_000L, 500_000L};

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;

    public JackpotService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "jackpot.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        if (!data.contains("round.next-draw")) {
            data.set("round.next-draw", System.currentTimeMillis() + ROUND_MILLIS);
            save();
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Jackpot"), 54);
        long pot = getPot();
        long own = getContribution(player.getUniqueId());
        int participants = getParticipants().size();

        gui.setItem(4, UiItems.item(Material.NETHER_STAR,
                UiTheme.LEGENDARY + "COIN JACKPOT",
                UiTheme.MUTED + "Pot: " + UiTheme.TEXT + formatCoins(pot),
                UiTheme.MUTED + "Spieler: " + UiTheme.TEXT + participants,
                UiTheme.MUTED + "Naechste Ziehung: " + UiTheme.TEXT + formatRemaining()), null);

        int[] slots = {19, 20, 21, 23, 24};
        Material[] materials = {Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD,
                Material.DIAMOND, Material.NETHER_STAR};
        for (int i = 0; i < QUICK_ENTRIES.length; i++) {
            final long amount = QUICK_ENTRIES[i];
            gui.setItem(slots[i], UiItems.item(materials[i],
                    UiTheme.PRIMARY + "+" + formatCoins(amount),
                    UiTheme.MUTED + "Zahlt Coins in den aktuellen Pot.",
                    UiTheme.MUTED + "Deine Chance steigt proportional.",
                    UiItems.action("Klicken zum Einzahlen")), (p,e,s) -> {
                enter(p, amount);
                open(p);
            });
        }

        double chance = pot <= 0L ? 0D : (own * 100D / pot);
        gui.setItem(31, UiItems.item(Material.PAPER,
                UiTheme.TEXT + "Dein Ticket",
                UiTheme.MUTED + "Eingezahlt: " + UiTheme.TEXT + formatCoins(own),
                UiTheme.MUTED + "Gewinnchance: " + UiTheme.PRIMARY + String.format(java.util.Locale.US, "%.2f%%", chance),
                UiTheme.MUTED + "Mindestens 2 Spieler fuer Ziehung."), null);

        String lastWinner = data.getString("history.last-winner-name", "-");
        long lastPot = data.getLong("history.last-pot", 0L);
        gui.setItem(33, UiItems.item(Material.BOOK,
                UiTheme.MYTHIC + "Letzte Runde",
                UiTheme.MUTED + "Gewinner: " + UiTheme.TEXT + lastWinner,
                UiTheme.MUTED + "Pot: " + UiTheme.TEXT + formatCoins(lastPot)), null);

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS,
                UiTheme.PRIMARY + "Home", UiTheme.MUTED + "Zur SkyKings Uebersicht.", UiItems.action("Klicken")),
                (p,e,s) -> {
                    SoundFeedback.back(p);
                    Bukkit.dispatchCommand(p, "commands");
                });

        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    public synchronized boolean enter(Player player, long amount) {
        if (amount < MIN_ENTRY || amount > MAX_ENTRY) {
            player.sendMessage(UiTheme.DANGER + "Jackpot-Einsatz muss zwischen " + formatCoins(MIN_ENTRY)
                    + " und " + formatCoins(MAX_ENTRY) + " liegen.");
            SoundFeedback.error(player);
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (!economy.withdraw(uuid, amount, "JACKPOT", "Jackpot entry")) {
            player.sendMessage(UiTheme.DANGER + "Dafuer hast du nicht genug Coins.");
            SoundFeedback.error(player);
            return false;
        }

        String base = "round.entries." + uuid;
        data.set(base + ".name", player.getName());
        data.set(base + ".amount", safeAdd(data.getLong(base + ".amount", 0L), amount));
        save();
        player.sendMessage(UiTheme.SUCCESS + "+" + formatCoins(amount) + ChatColor.GRAY + " im Jackpot.");
        SoundFeedback.success(player);
        return true;
    }

    public synchronized long getPot() {
        long total = 0L;
        for (long amount : getParticipants().values()) total = safeAdd(total, amount);
        return total;
    }

    public long getContribution(UUID uuid) {
        return data.getLong("round.entries." + uuid + ".amount", 0L);
    }

    private void tick() {
        if (System.currentTimeMillis() < data.getLong("round.next-draw", 0L)) return;
        draw();
    }

    private synchronized void draw() {
        Map<UUID, Long> entries = getParticipants();
        if (entries.size() < 2) {
            data.set("round.next-draw", System.currentTimeMillis() + RETRY_MILLIS);
            save();
            return;
        }

        long pot = 0L;
        for (long amount : entries.values()) pot = safeAdd(pot, amount);
        if (pot <= 0L) {
            clearRound(ROUND_MILLIS);
            return;
        }

        long ticket = ThreadLocalRandom.current().nextLong(pot);
        UUID winner = null;
        long cursor = 0L;
        for (Map.Entry<UUID, Long> entry : entries.entrySet()) {
            cursor = safeAdd(cursor, entry.getValue());
            if (ticket < cursor) {
                winner = entry.getKey();
                break;
            }
        }
        if (winner == null) winner = entries.keySet().iterator().next();

        String name = data.getString("round.entries." + winner + ".name", null);
        if (name == null || name.trim().isEmpty()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(winner);
            name = offline.getName() == null ? winner.toString().substring(0, 8) : offline.getName();
        }

        economy.deposit(winner, pot, "JACKPOT", "Jackpot win");
        data.set("history.last-winner", winner.toString());
        data.set("history.last-winner-name", name);
        data.set("history.last-pot", pot);
        data.set("history.last-draw", System.currentTimeMillis());
        clearRound(ROUND_MILLIS);

        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "JACKPOT" + ChatColor.DARK_GRAY + "] "
                + ChatColor.WHITE + name + ChatColor.GRAY + " gewinnt " + ChatColor.GOLD + formatCoins(pot)
                + ChatColor.GRAY + " Coins!");
        Player online = Bukkit.getPlayer(winner);
        if (online != null) online.playSound(online.getLocation(), Sound.LEVEL_UP, 1F, 1.15F);
    }

    private void clearRound(long delayMillis) {
        data.set("round.entries", null);
        data.set("round.next-draw", System.currentTimeMillis() + delayMillis);
        save();
    }

    private Map<UUID, Long> getParticipants() {
        Map<UUID, Long> out = new LinkedHashMap<UUID, Long>();
        ConfigurationSection section = data.getConfigurationSection("round.entries");
        if (section == null) return out;
        for (String raw : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                long amount = section.getLong(raw + ".amount", 0L);
                if (amount > 0L) out.put(uuid, amount);
            } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }

    private String formatRemaining() {
        long millis = Math.max(0L, data.getLong("round.next-draw", 0L) - System.currentTimeMillis());
        long seconds = (millis + 999L) / 1000L;
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        return minutes + "m " + rest + "s";
    }

    private long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private String formatCoins(long amount) {
        return String.format(java.util.Locale.GERMANY, "%,d", amount);
    }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("jackpot.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
