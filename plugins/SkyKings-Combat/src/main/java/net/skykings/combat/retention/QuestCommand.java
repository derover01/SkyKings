package net.skykings.combat.retention;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /quests als Free-/Premium-Questboard mit getrennten visuellen Panels. */
public final class QuestCommand implements CommandExecutor {
    private final QuestService quests;
    public QuestCommand(QuestService quests) { this.quests = quests; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        open((Player) sender); return true;
    }

    private void open(Player player) {
        UUID uuid = player.getUniqueId();
        boolean premium = quests.isPremium(uuid);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Battle Pass Quests"), 54);

        gui.setItem(4, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "QUEST CENTER",
                UiTheme.MUTED + "Free Quests oben.",
                UiTheme.MUTED + "Premium Quests unten.",
                UiTheme.MUTED + "Rewards werden automatisch ausgezahlt.",
                premium ? UiTheme.LEGENDARY + "PREMIUM ACTIVE" : UiTheme.MUTED + "FREE PASS"));

        gui.setItem(9, panel((short) 7, UiTheme.TEXT + "FREE QUESTS"));
        gui.setItem(17, panel((short) 7, UiTheme.TEXT + "FREE QUESTS"));
        gui.setItem(10, quest(Material.IRON_SWORD, "Daily • PvP Jaeger",
                "5 legitime PvP-Kills", quests.get(uuid, "daily.kills"), 5,
                quests.claimed(uuid, "daily.claimed-kills"), "150k Coins • 2 Sterne • 500 XP", false));
        gui.setItem(12, quest(Material.ENDER_PEARL, "Daily • Pearl Runner",
                "20 Enderperlen nutzen", quests.get(uuid, "daily.pearls"), 20,
                quests.claimed(uuid, "daily.claimed-pearls"), "75k Coins • 1 Stern • 350 XP", false));
        gui.setItem(14, quest(Material.DIAMOND_SWORD, "Daily • Unstoppable",
                "5er Killstreak erreichen", quests.get(uuid, "daily.streak"), 5,
                quests.claimed(uuid, "daily.claimed-streak"), "200k Coins • 3 Sterne • 750 XP", false));
        gui.setItem(16, quest(Material.NETHER_STAR, "Weekly • Kampfkoenig",
                "30 legitime PvP-Kills", quests.get(uuid, "weekly.kills"), 30,
                quests.claimed(uuid, "weekly.claimed-kills"), "500k Coins • 5 Sterne • 2.000 XP", false));
        gui.setItem(22, quest(Material.BEACON, "Weekly • Kingmaker",
                "King Altar 3x erobern", quests.get(uuid, "weekly.altar"), 3,
                quests.claimed(uuid, "weekly.claimed-altar"), "350k Coins • 4 Sterne • 1.500 XP", false));

        gui.setItem(27, panel((short) (premium ? 5 : 15), premium ? UiTheme.SUCCESS + "PREMIUM QUESTS" : UiTheme.DISABLED + "PREMIUM LOCKED"));
        gui.setItem(35, panel((short) (premium ? 5 : 15), premium ? UiTheme.SUCCESS + "PREMIUM QUESTS" : UiTheme.DISABLED + "PREMIUM LOCKED"));
        if (premium) {
            gui.setItem(28, quest(Material.GOLD_SWORD, "Premium Daily • Hunter",
                    "10 legitime PvP-Kills", quests.get(uuid, "premium.daily.kills"), 10,
                    quests.claimed(uuid, "premium.daily.claimed-kills"), "300k Coins • 4 Sterne • 800 XP", true));
            gui.setItem(30, quest(Material.EYE_OF_ENDER, "Premium Daily • Void Runner",
                    "40 Enderperlen nutzen", quests.get(uuid, "premium.daily.pearls"), 40,
                    quests.claimed(uuid, "premium.daily.claimed-pearls"), "150k Coins • 3 Sterne • 600 XP", true));
            gui.setItem(32, quest(Material.GOLDEN_APPLE, "Premium Weekly • Dominator",
                    "75 legitime PvP-Kills", quests.get(uuid, "premium.weekly.kills"), 75,
                    quests.claimed(uuid, "premium.weekly.claimed-kills"), "1.25m Coins • 12 Sterne • 3.500 XP", true));
            gui.setItem(34, quest(Material.BEACON, "Premium Weekly • Crowned",
                    "King Altar 7x erobern", quests.get(uuid, "premium.weekly.altar"), 7,
                    quests.claimed(uuid, "premium.weekly.claimed-altar"), "750k Coins • 8 Sterne • 2.500 XP", true));
        } else {
            gui.setItem(31, UiItems.item(Material.GOLD_INGOT, UiTheme.LEGENDARY + "Premium Questpool",
                    UiTheme.MUTED + "Zusaetzliche Daily-/Weekly-Aufgaben.",
                    UiTheme.MUTED + "Mehr Season-XP, ohne Free Quests zu ersetzen.",
                    "", UiTheme.STATUS_LOCKED));
        }

        gui.setItem(40, UiItems.item(Material.CHEST, UiTheme.LEGENDARY + "Reward Track",
                UiTheme.MUTED + "Quest-XP schiebt den",
                UiTheme.MUTED + "Battle-Pass-Levelpfad nach vorne.",
                "", UiItems.action("Rewards ansehen")), (p,e,s) -> {
            BattlePassService pass = BattlePassService.active();
            if (pass != null) pass.openRewards(p, 0);
        });
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> Bukkit.dispatchCommand(p, "profile"));
        gui.setItem(UiTheme.NAV_HOME, UiItems.home(), (p,e,s) -> {
            BattlePassService pass = BattlePassService.active();
            if (pass != null) pass.open(p);
        });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private org.bukkit.inventory.ItemStack quest(Material material, String name, String task,
                                                  int current, int target, boolean claimed, String reward, boolean premium) {
        int shown = Math.min(current, target);
        return UiItems.item(material,
                (premium ? UiTheme.LEGENDARY : UiTheme.PRIMARY) + name,
                UiTheme.TEXT + task,
                UiTheme.MUTED + "Fortschritt " + UiTheme.TEXT + shown + "/" + target,
                progress(shown, target),
                UiTheme.MUTED + "Reward " + UiTheme.WARNING + reward,
                "",
                claimed ? UiTheme.STATUS_COMPLETED : UiTheme.STATUS_ACTIVE);
    }

    private String progress(int current, int target) {
        int filled = Math.min(10, (int) Math.floor((current * 10D) / Math.max(1, target)));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 10; i++) out.append(i < filled ? UiTheme.SUCCESS + "■" : UiTheme.DISABLED + "■");
        return out.toString();
    }

    private org.bukkit.inventory.ItemStack panel(short data, String name) {
        return UiItems.item(Material.STAINED_GLASS_PANE, data, name);
    }
}
