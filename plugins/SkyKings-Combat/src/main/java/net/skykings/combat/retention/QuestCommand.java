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

import java.util.UUID;

/** /quests als Free-/Premium-Questboard mit klarer Aufgabenbeschreibung. */
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
        GuiSession gui = GuiSession.create(player, UiTheme.title("Quests"), 54);

        gui.setItem(4, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "Dein Questboard",
                UiTheme.MUTED + "Daily reset: taeglich",
                UiTheme.MUTED + "Weekly reset: woechentlich",
                premium ? UiTheme.LEGENDARY + "Premium Quests aktiv" : UiTheme.MUTED + "Free Questpool"));

        gui.setItem(11, quest(Material.IRON_SWORD, "Daily • PvP Jaeger",
                "Besiege 5 verschiedene Spieler", "im Open-World-PvP.",
                quests.get(uuid, "daily.kills"), 5, quests.claimed(uuid, "daily.claimed-kills"),
                "150.000 Coins + 2 Sterne", false));
        gui.setItem(13, quest(Material.ENDER_PEARL, "Daily • Pearl Runner",
                "Nutze 20 Enderperlen", "ausserhalb von Events.",
                quests.get(uuid, "daily.pearls"), 20, quests.claimed(uuid, "daily.claimed-pearls"),
                "75.000 Coins + 1 Stern", false));
        gui.setItem(15, quest(Material.DIAMOND_SWORD, "Weekly • Kampfkoenig",
                "Besiege 30 verschiedene Spieler", "im legitimen Open-World-PvP.",
                quests.get(uuid, "weekly.kills"), 30, quests.claimed(uuid, "weekly.claimed-kills"),
                "500.000 Coins + 5 Sterne", false));

        if (premium) {
            gui.setItem(29, quest(Material.GOLD_SWORD, "Premium Daily • Hunter",
                    "Besiege 10 verschiedene Spieler", "im Open-World-PvP.",
                    quests.get(uuid, "premium.daily.kills"), 10, quests.claimed(uuid, "premium.daily.claimed-kills"),
                    "300.000 Coins + 4 Sterne", true));
            gui.setItem(31, quest(Material.EYE_OF_ENDER, "Premium Daily • Void Runner",
                    "Nutze 40 Enderperlen", "ausserhalb von Events.",
                    quests.get(uuid, "premium.daily.pearls"), 40, quests.claimed(uuid, "premium.daily.claimed-pearls"),
                    "150.000 Coins + 3 Sterne", true));
            gui.setItem(33, quest(Material.GOLDEN_APPLE, "Premium Weekly • Dominator",
                    "Besiege 75 verschiedene Spieler", "im legitimen Open-World-PvP.",
                    quests.get(uuid, "premium.weekly.kills"), 75, quests.claimed(uuid, "premium.weekly.claimed-kills"),
                    "1.250.000 Coins + 12 Sterne", true));
        } else {
            gui.setItem(31, UiItems.item(Material.GOLD_INGOT, UiTheme.LEGENDARY + "Premium Questpool",
                    UiTheme.MUTED + "Premium-Spieler erhalten zusaetzliche",
                    UiTheme.MUTED + "Daily- und Weekly-Aufgaben.",
                    "", UiTheme.DISABLED + "Mit Free Pass nicht sichtbar."));
        }

        gui.setItem(40, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "SkyKings Sterne",
                UiTheme.MUTED + "Quests geben echte physische Sterne.",
                UiTheme.MUTED + "Sie landen direkt im Inventar."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); org.bukkit.Bukkit.dispatchCommand(p, "profile"); });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private org.bukkit.inventory.ItemStack quest(Material material, String name, String task1, String task2,
                                                  int current, int target, boolean claimed, String reward, boolean premium) {
        int shown = Math.min(current, target);
        return UiItems.item(material,
                (premium ? UiTheme.LEGENDARY : UiTheme.PRIMARY) + name,
                UiTheme.TEXT + "Aufgabe:",
                UiTheme.MUTED + task1,
                UiTheme.MUTED + task2,
                "",
                UiTheme.MUTED + "Fortschritt: " + UiTheme.TEXT + shown + "/" + target,
                progress(shown, target),
                UiTheme.MUTED + "Reward: " + UiTheme.WARNING + reward,
                "",
                claimed ? UiTheme.STATUS_COMPLETED : (shown >= target ? UiTheme.STATUS_READY : UiTheme.STATUS_ACTIVE));
    }

    private String progress(int current, int target) {
        int filled = Math.min(10, (int) Math.floor((current * 10D) / Math.max(1, target)));
        StringBuilder out = new StringBuilder(UiTheme.SUCCESS.toString());
        for (int i = 0; i < 10; i++) { if (i == filled) out.append(UiTheme.DISABLED); out.append('■'); }
        return out.toString();
    }
}
