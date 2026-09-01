package net.skykings.combat.retention;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.UUID;

/** Battle-Pass-Quest-Center mit Daily-/Weekly-/Premium-Systemquests. */
public final class QuestCommand implements CommandExecutor {
    private enum Page { DAILY, WEEKLY, PREMIUM }

    private final QuestService quests;
    public QuestCommand(QuestService quests) { this.quests = quests; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Page page = Page.DAILY;
        if (args.length > 0) {
            String raw = args[0].toLowerCase(Locale.ROOT);
            if (raw.startsWith("week") || raw.equals("woche")) page = Page.WEEKLY;
            else if (raw.startsWith("prem")) page = Page.PREMIUM;
        }
        open((Player) sender, page);
        return true;
    }

    private void open(Player player, Page page) {
        UUID uuid = player.getUniqueId();
        boolean premium = quests.isPremium(uuid);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Quest Center"), 54);

        int dailyDone = completedDaily(uuid);
        int weeklyDone = completedWeekly(uuid);
        int premiumDone = completedPremium(uuid);
        gui.setItem(4, UiItems.item(Material.ENCHANTED_BOOK, UiTheme.PRIMARY + "QUEST CENTER",
                UiTheme.MUTED + "Daily " + UiTheme.TEXT + dailyDone + "/4",
                UiTheme.MUTED + "Weekly " + UiTheme.TEXT + weeklyDone + "/5",
                UiTheme.MUTED + "Premium " + (premium ? UiTheme.LEGENDARY.toString() + premiumDone + "/8" : UiTheme.DISABLED + "LOCKED"),
                "", UiTheme.MUTED + "Rewards werden automatisch ausgezahlt."));

        gui.setItem(10, tab(Material.WATCH, "DAILY", page == Page.DAILY, dailyDone + "/4 erledigt"),
                (p,e,s) -> open(p, Page.DAILY));
        gui.setItem(13, tab(Material.BEACON, "WEEKLY", page == Page.WEEKLY, weeklyDone + "/5 erledigt"),
                (p,e,s) -> open(p, Page.WEEKLY));
        gui.setItem(16, tab(Material.GOLD_INGOT, "PREMIUM", page == Page.PREMIUM,
                premium ? premiumDone + "/8 erledigt" : "Premium erforderlich"),
                (p,e,s) -> open(p, Page.PREMIUM));

        for (int slot = 18; slot <= 26; slot++) gui.setItem(slot, panel((short) 15, " "));

        if (page == Page.DAILY) renderDaily(gui, uuid);
        else if (page == Page.WEEKLY) renderWeekly(gui, uuid);
        else renderPremium(gui, uuid, premium);

        gui.setItem(46, UiItems.item(Material.EXP_BOTTLE, UiTheme.PRIMARY + "Season XP",
                UiTheme.MUTED + "Jede abgeschlossene Quest",
                UiTheme.MUTED + "schiebt den Battle Pass weiter."));
        gui.setItem(48, UiItems.item(Material.CHEST, UiTheme.LEGENDARY + "REWARD TRACK",
                UiTheme.MUTED + "100 Free + 100 Premium Rewards",
                "", UiItems.action("Rewards ansehen")), (p,e,s) -> {
            BattlePassService pass = BattlePassService.active();
            if (pass != null) pass.openRewards(p, 0);
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.home(), (p,e,s) -> {
            BattlePassService pass = BattlePassService.active();
            if (pass != null) pass.open(p);
        });
        gui.setItem(UiTheme.NAV_NEXT, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "BATTLE PASS",
                UiTheme.MUTED + "Zur Pass-Uebersicht"), (p,e,s) -> {
            BattlePassService pass = BattlePassService.active();
            if (pass != null) pass.open(p);
        });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void renderDaily(GuiSession gui, UUID uuid) {
        gui.setItem(28, quest(Material.IRON_SWORD, "PvP Jaeger", "5 legitime PvP-Kills",
                quests.get(uuid, "daily.kills"), 5, quests.claimed(uuid, "daily.claimed-kills"),
                "150k Coins • 2 Sterne • 500 XP", false));
        gui.setItem(30, quest(Material.ENDER_PEARL, "Pearl Runner", "20 Enderperlen nutzen",
                quests.get(uuid, "daily.pearls"), 20, quests.claimed(uuid, "daily.claimed-pearls"),
                "75k Coins • 1 Stern • 350 XP", false));
        gui.setItem(32, quest(Material.DIAMOND_SWORD, "Unstoppable", "5er Killstreak erreichen",
                quests.get(uuid, "daily.streak"), 5, quests.claimed(uuid, "daily.claimed-streak"),
                "200k Coins • 3 Sterne • 750 XP", false));
        gui.setItem(34, quest(Material.IRON_CHESTPLATE, "Challenger", "1 Duel gewinnen",
                quests.get(uuid, "daily.duels"), 1, quests.claimed(uuid, "daily.claimed-duels"),
                "125k Coins • 2 Sterne • 600 XP", false));
        gui.setItem(40, UiItems.item(Material.WATCH, UiTheme.TEXT + "DAILY RESET",
                UiTheme.MUTED + "Neue Daily-Aufgaben mit",
                UiTheme.MUTED + "dem naechsten Kalendertag."));
    }

    private void renderWeekly(GuiSession gui, UUID uuid) {
        gui.setItem(28, quest(Material.NETHER_STAR, "Kampfkoenig", "30 legitime PvP-Kills",
                quests.get(uuid, "weekly.kills"), 30, quests.claimed(uuid, "weekly.claimed-kills"),
                "500k Coins • 5 Sterne • 2.000 XP", false));
        gui.setItem(30, quest(Material.BEACON, "Kingmaker", "King Altar 3x erobern",
                quests.get(uuid, "weekly.altar"), 3, quests.claimed(uuid, "weekly.claimed-altar"),
                "350k Coins • 4 Sterne • 1.500 XP", false));
        gui.setItem(32, quest(Material.CHEST, "Crate Runner", "3 Crates oeffnen",
                quests.get(uuid, "weekly.crates"), 3, quests.claimed(uuid, "weekly.claimed-crates"),
                "300k Coins • 4 Sterne • 1.500 XP", false));
        gui.setItem(34, quest(Material.GOLD_INGOT, "Head Hunter", "1 Bounty kassieren",
                quests.get(uuid, "weekly.bounties"), 1, quests.claimed(uuid, "weekly.claimed-bounties"),
                "350k Coins • 5 Sterne • 1.750 XP", false));
        gui.setItem(40, quest(Material.ENDER_CHEST, "Treasure Hunter", "2 Rare/Epic Map Chests",
                quests.get(uuid, "weekly.rare-chests"), 2, quests.claimed(uuid, "weekly.claimed-rare-chests"),
                "250k Coins • 4 Sterne • 1.500 XP", false));
        gui.setItem(42, UiItems.item(Material.WATCH, UiTheme.TEXT + "WEEKLY RESET",
                UiTheme.MUTED + "Reset mit der Kalenderwoche."));
    }

    private void renderPremium(GuiSession gui, UUID uuid, boolean premium) {
        if (!premium) {
            gui.setItem(31, UiItems.item(Material.GOLD_INGOT, UiTheme.LEGENDARY + "PREMIUM QUESTS",
                    UiTheme.MUTED + "Acht zusaetzliche Aufgaben.",
                    UiTheme.MUTED + "Mehr Season-XP und Rewards.",
                    UiTheme.MUTED + "Free Quests bleiben erhalten.",
                    "", UiTheme.STATUS_LOCKED));
            gui.setItem(40, panel((short) 1, UiTheme.LEGENDARY + "PREMIUM LOCKED"));
            return;
        }
        gui.setItem(28, quest(Material.GOLD_SWORD, "Hunter", "10 legitime PvP-Kills",
                quests.get(uuid, "premium.daily.kills"), 10, quests.claimed(uuid, "premium.daily.claimed-kills"),
                "300k Coins • 4 Sterne • 800 XP", true));
        gui.setItem(30, quest(Material.EYE_OF_ENDER, "Void Runner", "40 Enderperlen nutzen",
                quests.get(uuid, "premium.daily.pearls"), 40, quests.claimed(uuid, "premium.daily.claimed-pearls"),
                "150k Coins • 3 Sterne • 600 XP", true));
        gui.setItem(32, quest(Material.GOLDEN_APPLE, "Dominator", "75 legitime PvP-Kills",
                quests.get(uuid, "premium.weekly.kills"), 75, quests.claimed(uuid, "premium.weekly.claimed-kills"),
                "1.25m Coins • 12 Sterne • 3.500 XP", true));
        gui.setItem(34, quest(Material.BEACON, "Crowned", "King Altar 7x erobern",
                quests.get(uuid, "premium.weekly.altar"), 7, quests.claimed(uuid, "premium.weekly.claimed-altar"),
                "750k Coins • 8 Sterne • 2.500 XP", true));
        gui.setItem(37, quest(Material.DIAMOND_CHESTPLATE, "Duelist", "3 Duels gewinnen",
                quests.get(uuid, "premium.weekly.duels"), 3, quests.claimed(uuid, "premium.weekly.claimed-duels"),
                "600k Coins • 7 Sterne • 2.250 XP", true));
        gui.setItem(39, quest(Material.SKULL_ITEM, "Most Wanted", "2 Bounties kassieren",
                quests.get(uuid, "premium.weekly.bounties"), 2, quests.claimed(uuid, "premium.weekly.claimed-bounties"),
                "700k Coins • 8 Sterne • 2.500 XP", true));
        gui.setItem(41, quest(Material.TRAPPED_CHEST, "Crate Master", "5 Crates oeffnen",
                quests.get(uuid, "premium.weekly.crates"), 5, quests.claimed(uuid, "premium.weekly.claimed-crates"),
                "500k Coins • 6 Sterne • 2.000 XP", true));
        gui.setItem(43, quest(Material.ENDER_CHEST, "Relic Hunter", "4 Rare/Epic Map Chests",
                quests.get(uuid, "premium.weekly.rare-chests"), 4, quests.claimed(uuid, "premium.weekly.claimed-rare-chests"),
                "500k Coins • 6 Sterne • 2.000 XP", true));
    }

    private ItemStack tab(Material material, String name, boolean active, String detail) {
        return UiItems.item(material,
                (active ? UiTheme.PRIMARY : UiTheme.MUTED) + (active ? "▶ " : "") + name,
                UiTheme.MUTED + detail,
                "", active ? UiTheme.STATUS_ACTIVE : UiItems.action("Klicken zum Oeffnen"));
    }

    private ItemStack quest(Material material, String name, String task,
                            int current, int target, boolean claimed, String reward, boolean premium) {
        int shown = Math.min(current, target);
        String status = claimed ? UiTheme.STATUS_COMPLETED
                : shown >= target ? UiTheme.STATUS_READY : UiTheme.STATUS_ACTIVE;
        return UiItems.item(material,
                (premium ? UiTheme.LEGENDARY : UiTheme.PRIMARY) + name,
                UiTheme.TEXT + task,
                "", progress(shown, target),
                UiTheme.MUTED.toString() + shown + "/" + target + " abgeschlossen",
                UiTheme.MUTED + "Reward " + UiTheme.WARNING + reward,
                "", status);
    }

    private int completedDaily(UUID uuid) {
        int out = 0;
        if (quests.claimed(uuid, "daily.claimed-kills")) out++;
        if (quests.claimed(uuid, "daily.claimed-pearls")) out++;
        if (quests.claimed(uuid, "daily.claimed-streak")) out++;
        if (quests.claimed(uuid, "daily.claimed-duels")) out++;
        return out;
    }

    private int completedWeekly(UUID uuid) {
        int out = 0;
        if (quests.claimed(uuid, "weekly.claimed-kills")) out++;
        if (quests.claimed(uuid, "weekly.claimed-altar")) out++;
        if (quests.claimed(uuid, "weekly.claimed-crates")) out++;
        if (quests.claimed(uuid, "weekly.claimed-bounties")) out++;
        if (quests.claimed(uuid, "weekly.claimed-rare-chests")) out++;
        return out;
    }

    private int completedPremium(UUID uuid) {
        int out = 0;
        if (quests.claimed(uuid, "premium.daily.claimed-kills")) out++;
        if (quests.claimed(uuid, "premium.daily.claimed-pearls")) out++;
        if (quests.claimed(uuid, "premium.weekly.claimed-kills")) out++;
        if (quests.claimed(uuid, "premium.weekly.claimed-altar")) out++;
        if (quests.claimed(uuid, "premium.weekly.claimed-duels")) out++;
        if (quests.claimed(uuid, "premium.weekly.claimed-bounties")) out++;
        if (quests.claimed(uuid, "premium.weekly.claimed-crates")) out++;
        if (quests.claimed(uuid, "premium.weekly.claimed-rare-chests")) out++;
        return out;
    }

    private String progress(int current, int target) {
        int filled = Math.min(12, (int) Math.floor((current * 12D) / Math.max(1, target)));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 12; i++) out.append(i < filled ? UiTheme.SUCCESS + "■" : UiTheme.DISABLED + "■");
        return out.toString();
    }

    private ItemStack panel(short data, String name) {
        return UiItems.item(Material.STAINED_GLASS_PANE, data, name);
    }
}
