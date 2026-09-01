package net.skykings.combat.retention;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/** Season Battle Pass mit 100 Free- und 100 Premium-Level-Rewards. */
public final class BattlePassService implements Listener {
    private static final int MAX_LEVEL = 100;
    private static final int[] TRACK_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] RAIL_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    private static final int[] PREMIUM_SLOTS = {28, 29, 30, 31, 32, 33, 34};
    private static final int PER_PAGE = 7;
    private static volatile BattlePassService active;

    private final JavaPlugin plugin;
    private final SeasonProgressService progress;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.GERMANY);

    public BattlePassService(JavaPlugin plugin, SeasonProgressService progress, EconomyService economy) {
        this.plugin = plugin;
        this.progress = progress;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "battlepass.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        active = this;
    }

    public static BattlePassService active() { return active; }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        int level = progress.getLevel(uuid);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Battle Pass"), 45);

        gui.setItem(10, UiItems.item(Material.EXP_BOTTLE, UiTheme.PRIMARY.toString() + ChatColor.BOLD + "DEIN PASS",
                UiTheme.MUTED + "Season " + UiTheme.TEXT + progress.getSeason(),
                UiTheme.MUTED + "Level " + UiTheme.TEXT + level + "/" + MAX_LEVEL,
                UiTheme.MUTED + "XP " + UiTheme.TEXT + numbers.format(progress.getXp(uuid)),
                "", seasonBar(level),
                level >= MAX_LEVEL ? UiTheme.STATUS_COMPLETED : UiTheme.STATUS_ACTIVE));
        gui.setItem(19, panel((short) 9, UiTheme.PRIMARY + "PASS"));
        gui.setItem(28, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Naechstes Level",
                level >= MAX_LEVEL ? UiTheme.SUCCESS + "Season-Pfad abgeschlossen"
                        : UiTheme.MUTED + "Noch " + UiTheme.TEXT + numbers.format(progress.xpToNext(uuid)) + " XP",
                UiTheme.MUTED + "Quests und legitime PvP-Aktivitaet",
                UiTheme.MUTED + "fuellen deinen Season-Pfad."));

        gui.setItem(14, UiItems.item(Material.ENCHANTED_BOOK, UiTheme.MYTHIC.toString() + ChatColor.BOLD + "QUESTS",
                UiTheme.MUTED + "Daily, Weekly und Premium.",
                UiTheme.MUTED + "Aufgaben treiben deinen Pass voran.",
                "", UiItems.action("Questboard oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "quests"));
        gui.setItem(23, UiItems.item(Material.CHEST, UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "REWARDS",
                UiTheme.MUTED + "100 Level-Rewards.",
                UiTheme.MUTED + "Free- und Premium-Track getrennt.",
                "", UiItems.action("Reward Track oeffnen")), (p,e,s) -> openRewards(p, 0));
        gui.setItem(32, UiItems.item(Material.GOLD_INGOT,
                isPremium(uuid) ? UiTheme.SUCCESS.toString() + ChatColor.BOLD + "PREMIUM AKTIV"
                        : UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "PREMIUM",
                isPremium(uuid) ? UiTheme.MUTED + "100 Premium-Rewards + Premium-Quests"
                        : UiTheme.MUTED + "Zusaetzlicher Reward-Track",
                isPremium(uuid) ? UiTheme.STATUS_ACTIVE : UiTheme.STATUS_LOCKED));

        gui.setItem(16, panel((short) 10, UiTheme.MYTHIC + "QUESTS"));
        gui.setItem(25, panel((short) 1, UiTheme.LEGENDARY + "REWARDS"));
        gui.setItem(34, panel((short) 5, isPremium(uuid) ? UiTheme.SUCCESS + "PREMIUM" : UiTheme.DISABLED + "PREMIUM"));
        gui.setItem(40, UiItems.item(Material.BOOK_AND_QUILL, UiTheme.TEXT + "Season System",
                UiTheme.MUTED + "Free Track fuer alle.",
                UiTheme.MUTED + "Premium erweitert statt ersetzt.",
                UiTheme.MUTED + "Jedes Level hat eine Belohnung."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> Bukkit.dispatchCommand(p, "profile"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    public void openRewards(Player player, int requestedPage) {
        int maxPage = (MAX_LEVEL - 1) / PER_PAGE;
        final int page = Math.max(0, Math.min(maxPage, requestedPage));
        UUID uuid = player.getUniqueId();
        int playerLevel = progress.getLevel(uuid);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Pass Rewards " + (page + 1) + "/" + (maxPage + 1)), 54);

        gui.setItem(4, UiItems.item(Material.EXP_BOTTLE, UiTheme.PRIMARY.toString() + ChatColor.BOLD + "SEASON " + progress.getSeason(),
                UiTheme.MUTED + "Level " + UiTheme.TEXT + playerLevel + "/" + MAX_LEVEL,
                seasonBar(playerLevel),
                isPremium(uuid) ? UiTheme.LEGENDARY + "PREMIUM ACTIVE" : UiTheme.MUTED + "FREE PASS"));
        gui.setItem(9, UiItems.item(Material.IRON_INGOT, UiTheme.TEXT.toString() + ChatColor.BOLD + "FREE",
                UiTheme.MUTED + "Fuer jeden Spieler."));
        gui.setItem(27, UiItems.item(Material.GOLD_INGOT, UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "PREMIUM",
                isPremium(uuid) ? UiTheme.SUCCESS + "Freigeschaltet" : UiTheme.STATUS_LOCKED));

        int firstLevel = page * PER_PAGE + 1;
        for (int column = 0; column < PER_PAGE; column++) {
            final int rewardLevel = firstLevel + column;
            if (rewardLevel > MAX_LEVEL) {
                gui.setItem(TRACK_SLOTS[column], panel((short) 15, " "));
                gui.setItem(RAIL_SLOTS[column], panel((short) 15, " "));
                gui.setItem(PREMIUM_SLOTS[column], panel((short) 15, " "));
                continue;
            }
            gui.setItem(TRACK_SLOTS[column], rewardItem(player, false, rewardLevel),
                    (p,e,s) -> { claim(p, false, rewardLevel); openRewards(p, page); });
            gui.setItem(RAIL_SLOTS[column], railItem(playerLevel, rewardLevel));
            gui.setItem(PREMIUM_SLOTS[column], rewardItem(player, true, rewardLevel),
                    (p,e,s) -> { claim(p, true, rewardLevel); openRewards(p, page); });
        }

        gui.setItem(38, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Reward-Regel",
                UiTheme.MUTED + "Jedes Level besitzt einen Free-Reward.",
                UiTheme.MUTED + "Premium schaltet den zweiten Reward frei."));
        if (page > 0) gui.setItem(46, UiItems.item(Material.ARROW, UiTheme.MUTED + "Vorherige Seite"), (p,e,s) -> openRewards(p, page - 1));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        gui.setItem(UiTheme.NAV_HOME, UiItems.home(), (p,e,s) -> open(p));
        if (page < maxPage) gui.setItem(UiTheme.NAV_NEXT, UiItems.next(), (p,e,s) -> openRewards(p, page + 1));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    /** True bedeutet: neuer Premium-Zustand wurde erfolgreich persistent gespeichert. */
    public synchronized boolean setPremium(UUID uuid, boolean enabled) {
        String path = "players." + uuid + ".premium";
        boolean previous = data.getBoolean(path, false);
        data.set(path, enabled);
        if (saveNow()) return true;
        data.set(path, previous);
        plugin.getLogger().warning("Premium-Pass-Aenderung fuer " + uuid + " wurde wegen Save-Fehler verworfen.");
        return false;
    }

    public synchronized boolean isPremium(UUID uuid) {
        return data.getBoolean("players." + uuid + ".premium", false);
    }

    /**
     * Claim wird vor jeder Auszahlung persistent reserviert. Dadurch kann derselbe
     * Reward weder durch Rapid-Click noch durch ein Restart-Fenster doppelt ausgezahlt werden.
     */
    private synchronized void claim(Player player, boolean premium, int level) {
        UUID uuid = player.getUniqueId();
        if (level < 1 || level > MAX_LEVEL) return;
        if (progress.getLevel(uuid) < level) {
            player.sendMessage(UiTheme.DANGER + "Dafuer brauchst du Season-Level " + level + ".");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }
        if (premium && !isPremium(uuid)) {
            player.sendMessage(UiTheme.LEGENDARY + "Dieser Reward gehoert zum Premium Track.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.6F, 0.6F);
            return;
        }

        String path = claimPath(uuid, premium, level);
        if (data.getBoolean(path, false)) {
            player.sendMessage(UiTheme.WARNING + "Diesen Reward hast du bereits abgeholt.");
            return;
        }

        data.set(path, true);
        if (!saveNow()) {
            data.set(path, false);
            player.sendMessage(UiTheme.DANGER + "Reward konnte nicht sicher gespeichert werden. Bitte spaeter erneut versuchen.");
            plugin.getLogger().warning("Battle-Pass-Claim abgebrochen: Persistenz fehlgeschlagen fuer " + uuid + " Level " + level);
            return;
        }

        long coins = rewardCoins(premium, level);
        int stars = rewardStars(premium, level);
        try {
            economy.deposit(uuid, coins, "BATTLE_PASS", (premium ? "Premium" : "Free") + " Level " + level);
        } catch (RuntimeException ex) {
            data.set(path, false);
            if (!saveNow()) {
                plugin.getLogger().severe("Battle-Pass-Claim konnte nach Economy-Fehler nicht freigegeben werden: " + uuid + " Level " + level);
            }
            plugin.getLogger().warning("Battle-Pass-Auszahlung fehlgeschlagen fuer " + uuid + " Level " + level + ": " + ex.getMessage());
            player.sendMessage(UiTheme.DANGER + "Reward-Auszahlung fehlgeschlagen. Der Claim wurde nicht verbraucht.");
            return;
        }

        if (stars > 0) {
            try {
                SkyKingsCurrencyItems.give(player, stars);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Battle-Pass-Sterne konnten nicht ausgegeben werden fuer " + uuid + " Level " + level + ": " + ex.getMessage());
            }
        }

        String starText = stars > 0 ? " • +" + stars + " Sterne" : "";
        player.sendMessage(UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "BATTLE PASS " + UiTheme.SUCCESS + "Level " + level
                + UiTheme.MUTED + " • +" + numbers.format(coins) + " Coins" + starText);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, premium ? 1.6F : 1.3F);
    }

    private ItemStack rewardItem(Player player, boolean premium, int level) {
        UUID uuid = player.getUniqueId();
        boolean unlocked = progress.getLevel(uuid) >= level;
        boolean claimed = data.getBoolean(claimPath(uuid, premium, level), false);
        boolean premiumLocked = premium && !isPremium(uuid);
        Material material = rewardMaterial(level, premium, claimed);
        String state = claimed ? UiTheme.STATUS_COMPLETED
                : premiumLocked ? UiTheme.STATUS_LOCKED
                : unlocked ? UiTheme.STATUS_READY : UiTheme.STATUS_LOCKED;
        String action = claimed ? UiTheme.MUTED + "Bereits abgeholt"
                : premiumLocked ? UiTheme.DISABLED + "Premium erforderlich"
                : unlocked ? UiItems.action("Klicken zum Abholen")
                : UiTheme.MUTED + "Erreiche Level " + level;
        int stars = rewardStars(premium, level);
        return UiItems.item(material,
                (premium ? UiTheme.LEGENDARY : UiTheme.TEXT) + "Level " + level,
                UiTheme.MUTED + numbers.format(rewardCoins(premium, level)) + " Coins",
                stars > 0 ? UiTheme.MUTED.toString() + stars + " SkyKings Sterne" : UiTheme.DARK + "Coin Reward",
                "", state, action);
    }

    private ItemStack railItem(int currentLevel, int level) {
        boolean reached = currentLevel >= level;
        boolean next = currentLevel + 1 == level;
        short dataValue = reached ? (short) 5 : next ? (short) 4 : (short) 7;
        String name = reached ? UiTheme.SUCCESS + "▼ " + level
                : next ? UiTheme.WARNING + "▼ " + level
                : UiTheme.DISABLED + "▼ " + level;
        return UiItems.item(Material.STAINED_GLASS_PANE, dataValue, name,
                reached ? UiTheme.STATUS_COMPLETED : next ? UiTheme.STATUS_ACTIVE : UiTheme.STATUS_LOCKED);
    }

    private Material rewardMaterial(int level, boolean premium, boolean claimed) {
        if (claimed) return Material.STAINED_GLASS_PANE;
        if (level == MAX_LEVEL) return Material.NETHER_STAR;
        if (level % 25 == 0) return premium ? Material.DIAMOND : Material.EMERALD;
        if (level % 10 == 0) return premium ? Material.GOLD_INGOT : Material.IRON_INGOT;
        if (level % 5 == 0) return premium ? Material.GOLD_NUGGET : Material.QUARTZ;
        return premium ? Material.GOLD_NUGGET : Material.PAPER;
    }

    private long rewardCoins(boolean premium, int level) {
        long base = level * (premium ? 2_500L : 2_000L);
        if (level % 10 == 0) base += premium ? 100_000L : 75_000L;
        if (level % 25 == 0) base += premium ? 200_000L : 150_000L;
        if (level == MAX_LEVEL) base += premium ? 750_000L : 500_000L;
        return base;
    }

    private int rewardStars(boolean premium, int level) {
        int stars = 0;
        if (level % 5 == 0) stars += premium ? 2 : 1;
        if (level % 10 == 0) stars += premium ? 2 : 1;
        if (level % 25 == 0) stars += premium ? 3 : 2;
        if (level == MAX_LEVEL) stars += premium ? 10 : 5;
        return stars;
    }

    private String claimPath(UUID uuid, boolean premium, int level) {
        return "players." + uuid + ".claimed." + (premium ? "premium" : "free") + "." + level;
    }

    private String seasonBar(int level) {
        int filled = Math.min(20, Math.max(0, (int) Math.floor(level * 20D / MAX_LEVEL)));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 20; i++) out.append(i < filled ? UiTheme.SUCCESS + "■" : UiTheme.DISABLED + "■");
        return out.toString();
    }

    private ItemStack panel(short dataValue, String name) {
        return UiItems.item(Material.STAINED_GLASS_PANE, dataValue, name);
    }

    public synchronized void save() { saveNow(); }

    private boolean saveNow() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Battle-Pass-Datenordner konnte nicht erstellt werden.");
                return false;
            }
            data.save(file);
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().warning("battlepass.yml konnte nicht gespeichert werden: " + ex.getMessage());
            return false;
        }
    }
}
