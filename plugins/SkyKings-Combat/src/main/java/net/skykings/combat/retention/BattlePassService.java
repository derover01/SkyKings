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

/**
 * Season Battle Pass mit eigenem Hub, Quest-Portal und horizontalem Free/Premium-Reward-Track.
 * Die Logik bleibt 1.8-kompatibel; die Ansichten sind bewusst wie Panels aufgebaut statt als Item-Wand.
 */
public final class BattlePassService implements Listener {
    private static final int[] MILESTONES = {
            5, 10, 15, 20, 25, 30, 35, 40, 45, 50,
            55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    };
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

    /** Einstieg wie ein eigenes Battle-Pass-Portal statt direkt in eine Reward-Truhe. */
    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        int level = progress.getLevel(uuid);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Battle Pass"), 45);

        gui.setItem(10, UiItems.item(Material.EXP_BOTTLE, UiTheme.PRIMARY.toString() + ChatColor.BOLD + "DEIN PASS",
                UiTheme.MUTED + "Season " + UiTheme.TEXT + progress.getSeason(),
                UiTheme.MUTED + "Level " + UiTheme.TEXT + level + "/100",
                UiTheme.MUTED + "XP " + UiTheme.TEXT + numbers.format(progress.getXp(uuid)),
                "", seasonBar(level),
                level >= 100 ? UiTheme.STATUS_COMPLETED : UiTheme.STATUS_ACTIVE));
        gui.setItem(19, panel((short) 9, UiTheme.PRIMARY + "PASS"));
        gui.setItem(28, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Naechstes Level",
                level >= 100 ? UiTheme.SUCCESS + "Season-Pfad abgeschlossen"
                        : UiTheme.MUTED + "Noch " + UiTheme.TEXT + numbers.format(progress.xpToNext(uuid)) + " XP",
                UiTheme.MUTED + "Quests und legitime PvP-Aktivitaet",
                UiTheme.MUTED + "fuellen deinen Season-Pfad."));

        gui.setItem(14, UiItems.item(Material.ENCHANTED_BOOK, UiTheme.MYTHIC.toString() + ChatColor.BOLD + "QUESTS",
                UiTheme.MUTED + "Daily, Weekly und Premium.",
                UiTheme.MUTED + "Aufgaben treiben deinen Pass voran.",
                "", UiItems.action("Questboard oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "quests"));
        gui.setItem(23, UiItems.item(Material.CHEST, UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "REWARDS",
                UiTheme.MUTED + "20 Season-Meilensteine.",
                UiTheme.MUTED + "Free- und Premium-Track getrennt.",
                "", UiItems.action("Reward Track oeffnen")), (p,e,s) -> openRewards(p, 0));
        gui.setItem(32, UiItems.item(Material.GOLD_INGOT,
                isPremium(uuid) ? UiTheme.SUCCESS.toString() + ChatColor.BOLD + "PREMIUM AKTIV"
                        : UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "PREMIUM",
                isPremium(uuid) ? UiTheme.MUTED + "Premium-Track + Premium-Quests"
                        : UiTheme.MUTED + "Zusaetzlicher Reward-Track",
                isPremium(uuid) ? UiTheme.STATUS_ACTIVE : UiTheme.STATUS_LOCKED));

        gui.setItem(16, panel((short) 10, UiTheme.MYTHIC + "QUESTS"));
        gui.setItem(25, panel((short) 1, UiTheme.LEGENDARY + "REWARDS"));
        gui.setItem(34, panel((short) 5, isPremium(uuid) ? UiTheme.SUCCESS + "PREMIUM" : UiTheme.DISABLED + "PREMIUM"));

        gui.setItem(40, UiItems.item(Material.BOOK_AND_QUILL, UiTheme.TEXT + "Season System",
                UiTheme.MUTED + "Free Track fuer alle.",
                UiTheme.MUTED + "Premium erweitert statt ersetzt.",
                UiTheme.MUTED + "Season-Fortschritt bleibt persistent."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> Bukkit.dispatchCommand(p, "profile"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    /** Horizontale Free/Premium-Rails, angelehnt an moderne Battle-Pass-UIs. */
    public void openRewards(Player player, int requestedPage) {
        int maxPage = (MILESTONES.length - 1) / PER_PAGE;
        final int page = Math.max(0, Math.min(maxPage, requestedPage));
        UUID uuid = player.getUniqueId();
        int playerLevel = progress.getLevel(uuid);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Pass Rewards " + (page + 1) + "/" + (maxPage + 1)), 54);

        gui.setItem(4, UiItems.item(Material.EXP_BOTTLE, UiTheme.PRIMARY.toString() + ChatColor.BOLD + "SEASON " + progress.getSeason(),
                UiTheme.MUTED + "Level " + UiTheme.TEXT + playerLevel + "/100",
                seasonBar(playerLevel),
                isPremium(uuid) ? UiTheme.LEGENDARY + "PREMIUM ACTIVE" : UiTheme.MUTED + "FREE PASS"));
        gui.setItem(9, UiItems.item(Material.IRON_INGOT, UiTheme.TEXT.toString() + ChatColor.BOLD + "FREE",
                UiTheme.MUTED + "Fuer jeden Spieler."));
        gui.setItem(27, UiItems.item(Material.GOLD_INGOT, UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "PREMIUM",
                isPremium(uuid) ? UiTheme.SUCCESS + "Freigeschaltet" : UiTheme.STATUS_LOCKED));

        int start = page * PER_PAGE;
        for (int column = 0; column < PER_PAGE; column++) {
            int rewardIndex = start + column;
            if (rewardIndex >= MILESTONES.length) {
                gui.setItem(TRACK_SLOTS[column], panel((short) 15, " "));
                gui.setItem(RAIL_SLOTS[column], panel((short) 15, " "));
                gui.setItem(PREMIUM_SLOTS[column], panel((short) 15, " "));
                continue;
            }
            final int milestone = MILESTONES[rewardIndex];
            gui.setItem(TRACK_SLOTS[column], rewardItem(player, false, milestone),
                    (p,e,s) -> { claim(p, false, milestone); openRewards(p, page); });
            gui.setItem(RAIL_SLOTS[column], railItem(playerLevel, milestone));
            gui.setItem(PREMIUM_SLOTS[column], rewardItem(player, true, milestone),
                    (p,e,s) -> { claim(p, true, milestone); openRewards(p, page); });
        }

        gui.setItem(38, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Reward-Regel",
                UiTheme.MUTED + "Erreichtes Level + nicht abgeholt",
                UiTheme.MUTED + "= READY. Premium benoetigt Premium."));
        if (page > 0) gui.setItem(46, UiItems.item(Material.ARROW, UiTheme.MUTED + "Vorherige Seite"), (p,e,s) -> openRewards(p, page - 1));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        gui.setItem(UiTheme.NAV_HOME, UiItems.home(), (p,e,s) -> open(p));
        if (page < maxPage) gui.setItem(UiTheme.NAV_NEXT, UiItems.next(), (p,e,s) -> openRewards(p, page + 1));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    public boolean setPremium(UUID uuid, boolean enabled) {
        data.set("players." + uuid + ".premium", enabled);
        save();
        return enabled;
    }

    public boolean isPremium(UUID uuid) { return data.getBoolean("players." + uuid + ".premium", false); }

    private void claim(Player player, boolean premium, int level) {
        UUID uuid = player.getUniqueId();
        if (progress.getLevel(uuid) < level) {
            player.sendMessage(UiTheme.DANGER + "Dafuer brauchst du Season-Level " + level + ".");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return;
        }
        if (premium && !isPremium(uuid)) {
            player.sendMessage(UiTheme.LEGENDARY + "Dieser Reward gehoert zum Premium Track.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.6F, 0.6F); return;
        }
        String path = claimPath(uuid, premium, level);
        if (data.getBoolean(path, false)) {
            player.sendMessage(UiTheme.WARNING + "Diesen Reward hast du bereits abgeholt.");
            return;
        }

        long coins = rewardCoins(premium, level);
        int stars = rewardStars(premium, level);
        economy.deposit(uuid, coins, "BATTLE_PASS", (premium ? "Premium" : "Free") + " Level " + level);
        SkyKingsCurrencyItems.give(player, stars);
        data.set(path, true); save();
        player.sendMessage(UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "BATTLE PASS " + UiTheme.SUCCESS + "Level " + level
                + UiTheme.MUTED + " • +" + numbers.format(coins) + " Coins • +" + stars + " Sterne");
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
        return UiItems.item(material,
                (premium ? UiTheme.LEGENDARY : UiTheme.TEXT) + "Level " + level,
                UiTheme.MUTED + numbers.format(rewardCoins(premium, level)) + " Coins",
                UiTheme.MUTED.toString() + rewardStars(premium, level) + " SkyKings Sterne",
                "", state, action);
    }

    private ItemStack railItem(int currentLevel, int milestone) {
        boolean reached = currentLevel >= milestone;
        boolean next = currentLevel < milestone && currentLevel >= milestone - 5;
        short dataValue = reached ? (short) 5 : next ? (short) 4 : (short) 7;
        String name = reached ? UiTheme.SUCCESS + "▼ " + milestone
                : next ? UiTheme.WARNING + "▼ " + milestone
                : UiTheme.DISABLED + "▼ " + milestone;
        return UiItems.item(Material.STAINED_GLASS_PANE, dataValue, name,
                reached ? UiTheme.STATUS_COMPLETED : next ? UiTheme.STATUS_ACTIVE : UiTheme.STATUS_LOCKED);
    }

    private Material rewardMaterial(int level, boolean premium, boolean claimed) {
        if (claimed) return Material.STAINED_GLASS_PANE;
        if (level == 100) return Material.NETHER_STAR;
        if (level % 25 == 0) return premium ? Material.DIAMOND : Material.EMERALD;
        if (level % 10 == 0) return premium ? Material.GOLD_INGOT : Material.IRON_INGOT;
        return premium ? Material.GOLD_NUGGET : Material.QUARTZ;
    }

    private long rewardCoins(boolean premium, int level) {
        long base = premium ? level * 12_500L : level * 10_000L;
        if (level == 100) base += premium ? 500_000L : 250_000L;
        return base;
    }

    private int rewardStars(boolean premium, int level) {
        int divisor = premium ? 10 : 20;
        int stars = Math.max(1, level / divisor);
        if (level == 100) stars += premium ? 10 : 5;
        return stars;
    }

    private String claimPath(UUID uuid, boolean premium, int level) {
        return "players." + uuid + ".claimed." + (premium ? "premium" : "free") + "." + level;
    }

    private String seasonBar(int level) {
        int filled = Math.min(20, Math.max(0, (int) Math.floor(level * 20D / 100D)));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 20; i++) out.append(i < filled ? UiTheme.SUCCESS + "■" : UiTheme.DISABLED + "■");
        return out.toString();
    }

    private ItemStack panel(short dataValue, String name) {
        return UiItems.item(Material.STAINED_GLASS_PANE, dataValue, name);
    }

    public void save() {
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("battlepass.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
