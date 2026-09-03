package net.skykings.core.retention;

import net.skykings.core.economy.BalanceSettlementGuard;
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

/** Einzige autoritative Jackpot-Runtime fuer Command und Shop-NPC. */
public final class JackpotService {
    private static final long ROUND_MILLIS = 10L * 60L * 1000L;
    private static final long RETRY_MILLIS = 5L * 60L * 1000L;
    private static final long MIN_ENTRY = 10_000L;
    private static final long MAX_ENTRY = 1_000_000L;
    private static final long FEE_DIVISOR = 20L; // 5%
    private static final long[] QUICK_ENTRIES = {10_000L, 50_000L, 100_000L, 250_000L, 500_000L};
    private static volatile JackpotService liveInstance;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;

    public JackpotService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "jackpot.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        migrateLegacyEntries();
        recoverIncompleteSettlement();
        if (!data.contains("round.next-draw")) {
            data.set("round.next-draw", System.currentTimeMillis() + ROUND_MILLIS);
            saveNow();
        }
        liveInstance = this;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public static JackpotService liveInstance() { return liveInstance; }

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
        Material[] materials = {Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.DIAMOND, Material.NETHER_STAR};
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
        long lastPayout = data.getLong("history.last-payout", 0L);
        gui.setItem(33, UiItems.item(Material.BOOK,
                UiTheme.MYTHIC + "Letzte Runde",
                UiTheme.MUTED + "Gewinner: " + UiTheme.TEXT + lastWinner,
                UiTheme.MUTED + "Auszahlung: " + UiTheme.TEXT + formatCoins(lastPayout),
                UiTheme.DISABLED + "5% jeder Ziehung = Economy-Sink"), null);

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

    /** Abbuchung wird bei Save-Fehler automatisch zurueckgezahlt. */
    public synchronized boolean enter(Player player, long amount) {
        if (amount < MIN_ENTRY || amount > MAX_ENTRY) {
            player.sendMessage(UiTheme.DANGER + "Jackpot-Einsatz muss zwischen " + formatCoins(MIN_ENTRY)
                    + " und " + formatCoins(MAX_ENTRY) + " liegen.");
            SoundFeedback.error(player);
            return false;
        }
        if (isSettlementOpen()) {
            if ("REVIEW_REQUIRED".equalsIgnoreCase(data.getString("recovery.status", ""))) {
                player.sendMessage(UiTheme.WARNING + "Der Jackpot ist bis zur Staff-Pruefung eines vorherigen Settlements gesperrt.");
            } else {
                player.sendMessage(UiTheme.WARNING + "Die Jackpot-Ziehung wird gerade abgeschlossen. Bitte gleich erneut versuchen.");
            }
            return false;
        }

        UUID uuid = player.getUniqueId();
        String base = "round.entries." + uuid;
        String previousName = data.getString(base + ".name", null);
        long previousAmount = data.getLong(base + ".amount", 0L);
        long previousPot = getPot();
        if (!BalanceSettlementGuard.canAdd(previousAmount, amount)
                || !BalanceSettlementGuard.canAdd(previousPot, amount)) {
            player.sendMessage(UiTheme.DANGER + "Der Jackpot kann diesen Einsatz nicht mehr sicher aufnehmen.");
            plugin.getLogger().warning("Jackpot-Einsatz vor Abbuchung wegen long-Grenze blockiert: " + uuid + " / " + amount);
            return false;
        }

        if (!economy.withdraw(uuid, amount, "JACKPOT", "Jackpot entry")) {
            player.sendMessage(UiTheme.DANGER + "Dafuer hast du nicht genug Coins.");
            SoundFeedback.error(player);
            return false;
        }

        data.set(base + ".name", player.getName());
        data.set(base + ".amount", previousAmount + amount);
        if (!saveNow()) {
            if (previousName == null) data.set(base + ".name", null);
            else data.set(base + ".name", previousName);
            if (previousAmount <= 0L) data.set(base + ".amount", null);
            else data.set(base + ".amount", previousAmount);
            try {
                economy.deposit(uuid, amount, "JACKPOT_REFUND", "Jackpot entry persistence failed");
                player.sendMessage(UiTheme.DANGER + "Jackpot konnte nicht gespeichert werden. Dein Einsatz wurde erstattet.");
            } catch (RuntimeException ex) {
                plugin.getLogger().severe("KRITISCH: Jackpot-Einsatz konnte nach Save-Fehler nicht erstattet werden: " + uuid + " / " + amount);
                player.sendMessage(UiTheme.DANGER + "Jackpot-Speicherfehler. Bitte sofort Staff kontaktieren; der Vorgang wurde geloggt.");
            }
            return false;
        }

        player.sendMessage(UiTheme.SUCCESS + "+" + formatCoins(amount) + ChatColor.GRAY + " im Jackpot.");
        SoundFeedback.success(player);
        return true;
    }

    public synchronized long getPot() {
        long total = 0L;
        for (long amount : getParticipants().values()) total = safeAdd(total, amount);
        return total;
    }

    public synchronized long getContribution(UUID uuid) {
        return data.getLong("round.entries." + uuid + ".amount", 0L);
    }

    private void tick() {
        if (isSettlementOpen()) return;
        if (System.currentTimeMillis() < data.getLong("round.next-draw", 0L)) return;
        draw();
    }

    /**
     * Vor Auszahlung wird ein PENDING-Settlement persistent reserviert. Ein Crash in diesem
     * Fenster darf niemals automatisch zu einer zweiten Auszahlung fuehren.
     */
    private synchronized void draw() {
        if (isSettlementOpen()) return;
        Map<UUID, Long> entries = getParticipants();
        if (entries.size() < 2) {
            data.set("round.next-draw", System.currentTimeMillis() + RETRY_MILLIS);
            saveNow();
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
            if (ticket < cursor) { winner = entry.getKey(); break; }
        }
        if (winner == null) winner = entries.keySet().iterator().next();

        String name = data.getString("round.entries." + winner + ".name", null);
        if (name == null || name.trim().isEmpty()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(winner);
            name = offline.getName() == null ? winner.toString().substring(0, 8) : offline.getName();
        }

        long fee = pot / FEE_DIVISOR;
        if (pot % FEE_DIVISOR != 0L) fee++;
        long payout = pot - fee;

        long oldNextDraw = data.getLong("round.next-draw", 0L);
        long settlementCreatedAt = System.currentTimeMillis();
        long nextDraw = settlementCreatedAt + ROUND_MILLIS;
        markPendingSettlement(winner, name, pot, payout, settlementCreatedAt, nextDraw);
        if (!saveNow()) {
            data.set("round.settlement", null);
            data.set("round.next-draw", oldNextDraw);
            plugin.getLogger().warning("Jackpot-Ziehung abgebrochen: Settlement konnte nicht persistent reserviert werden.");
            return;
        }

        try {
            economy.deposit(winner, payout, "JACKPOT", "Jackpot win");
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("Jackpot-Auszahlung hat einen unsicheren Zustand erreicht. Settlement bleibt PENDING: "
                    + winner + " / " + payout + " / " + ex.getMessage());
            return;
        }
        if (!economy.persistNow(winner)) {
            plugin.getLogger().severe("Jackpot-Auszahlung wurde im RAM angewendet, aber der synchrone Balance-Commit ist fehlgeschlagen. "
                    + "Settlement bleibt PENDING fuer Review: " + winner + " / " + payout);
            return;
        }

        data.set("history.last-winner", winner.toString());
        data.set("history.last-winner-name", name);
        data.set("history.last-pot", pot);
        data.set("history.last-payout", payout);
        data.set("history.last-draw", System.currentTimeMillis());
        data.set("round.entries", null);
        data.set("round.settlement", null);
        if (!saveNow()) {
            markPendingSettlement(winner, name, pot, payout, settlementCreatedAt, nextDraw);
            plugin.getLogger().severe("Jackpot wurde durable ausgezahlt, aber Abschluss konnte nicht gespeichert werden. "
                    + "Runtime bleibt fail-closed auf PENDING; beim Restart wird REVIEW_REQUIRED gesetzt.");
        }

        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "JACKPOT" + ChatColor.DARK_GRAY + "] "
                + ChatColor.WHITE + name + ChatColor.GRAY + " gewinnt " + ChatColor.GOLD + formatCoins(payout)
                + ChatColor.GRAY + " Coins!");
        Player online = Bukkit.getPlayer(winner);
        if (online != null) online.playSound(online.getLocation(), Sound.LEVEL_UP, 1F, 1.15F);
    }

    /** Ein PENDING-Settlement wird nie automatisch erneut ausgezahlt. */
    private void recoverIncompleteSettlement() {
        if (!"PENDING".equalsIgnoreCase(data.getString("round.settlement.status", ""))) return;
        String winner = data.getString("round.settlement.winner", "unknown");
        String winnerName = data.getString("round.settlement.winner-name", "unknown");
        long pot = data.getLong("round.settlement.pot", 0L);
        long payout = data.getLong("round.settlement.payout", 0L);
        long createdAt = data.getLong("round.settlement.created-at", System.currentTimeMillis());
        plugin.getLogger().severe("Unvollstaendiges Jackpot-Settlement erkannt: " + winner + " / " + payout
                + ". Keine automatische Neuauszahlung; REVIEW_REQUIRED wurde gespeichert.");
        data.set("recovery.status", "REVIEW_REQUIRED");
        data.set("recovery.winner", winner);
        data.set("recovery.winner-name", winnerName);
        data.set("recovery.pot", pot);
        data.set("recovery.payout", payout);
        data.set("recovery.detected-at", System.currentTimeMillis());
        data.set("round.entries", null);
        data.set("round.settlement", null);
        long nextDraw = System.currentTimeMillis() + ROUND_MILLIS;
        data.set("round.next-draw", nextDraw);
        if (!saveNow()) {
            try {
                markPendingSettlement(UUID.fromString(winner), winnerName, pot, payout, createdAt, nextDraw);
            } catch (IllegalArgumentException ex) {
                data.set("round.settlement.status", "PENDING");
                data.set("round.settlement.winner", winner);
                data.set("round.settlement.winner-name", winnerName);
                data.set("round.settlement.pot", pot);
                data.set("round.settlement.payout", payout);
                data.set("round.settlement.created-at", createdAt);
            }
            plugin.getLogger().severe("Jackpot-Recovery konnte nicht gespeichert werden; Runtime bleibt PENDING und blockiert neue Einsaetze.");
        }
    }

    private void markPendingSettlement(UUID winner, String name, long pot, long payout, long createdAt, long nextDraw) {
        data.set("round.settlement.status", "PENDING");
        data.set("round.settlement.winner", winner.toString());
        data.set("round.settlement.winner-name", name);
        data.set("round.settlement.pot", pot);
        data.set("round.settlement.payout", payout);
        data.set("round.settlement.created-at", createdAt);
        data.set("round.next-draw", nextDraw);
    }

    private boolean isSettlementOpen() {
        return JackpotSettlementGate.blocks(
                data.getString("round.settlement.status", ""),
                data.getString("recovery.status", ""));
    }

    private void migrateLegacyEntries() {
        ConfigurationSection legacy = data.getConfigurationSection("entries");
        if (legacy == null || data.getConfigurationSection("round.entries") != null) return;
        int migrated = 0;
        for (String raw : legacy.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                long amount = legacy.getLong(raw, 0L);
                if (amount <= 0L) continue;
                data.set("round.entries." + uuid + ".amount", amount);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player.getName() != null) data.set("round.entries." + uuid + ".name", player.getName());
                migrated++;
            } catch (IllegalArgumentException ignored) { }
        }
        data.set("entries", null);
        if (migrated > 0) plugin.getLogger().info("Legacy-Jackpot migriert: " + migrated + " Teilnehmer.");
        saveNow();
    }

    private void clearRound(long delayMillis) {
        data.set("round.entries", null);
        data.set("round.next-draw", System.currentTimeMillis() + delayMillis);
        saveNow();
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
        return (seconds / 60L) + "m " + (seconds % 60L) + "s";
    }

    private long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private String formatCoins(long amount) { return String.format(java.util.Locale.GERMANY, "%,d", amount); }

    public synchronized void save() { saveNow(); }

    private boolean saveNow() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Jackpot-Datenordner konnte nicht erstellt werden.");
                return false;
            }
            data.save(file);
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().warning("jackpot.yml konnte nicht gespeichert werden: " + ex.getMessage());
            return false;
        }
    }
}
