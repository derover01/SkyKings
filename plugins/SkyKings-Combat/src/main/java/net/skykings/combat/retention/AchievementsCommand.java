package net.skykings.combat.retention;

import net.skykings.combat.map.zone.MapMasteryService;
import net.skykings.combat.stats.PvpStatsService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.pvp.PvpStatsSnapshot;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Achievement-Book auf Basis echter PvP-/Map-Ziele. */
public final class AchievementsCommand implements CommandExecutor {
    private final PvpStatsService stats;
    private final MapMasteryService mastery;

    public AchievementsCommand(PvpStatsService stats, MapMasteryService mastery) {
        this.stats = stats;
        this.mastery = mastery;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        open((Player) sender);
        return true;
    }

    private void open(Player player) {
        PvpStatsSnapshot s = stats.getStats(player.getUniqueId());
        GuiSession gui = GuiSession.create(player, UiTheme.title("Achievements"), 45);

        achievement(gui, 10, Material.IRON_SWORD, "First Blood", s.getKills() >= 1, "Erster PvP-Kill");
        achievement(gui, 11, Material.BLAZE_POWDER, "Unstoppable", s.getBestStreak() >= 10, "10er Killstreak");
        achievement(gui, 12, Material.DIAMOND_SWORD, "Untouchable", s.getBestStreak() >= 25, "25er Killstreak");
        achievement(gui, 13, Material.NETHER_STAR, "Sky Legend", s.getBestStreak() >= 50, "50er Killstreak");
        achievement(gui, 19, Material.GOLD_BLOCK, "King Slayer", mastery.getKingCaptures(player.getUniqueId()) >= 10, "10 King-Altar Captures");
        achievement(gui, 20, Material.COMPASS, "The Hunter", mastery.getHotZoneKills(player.getUniqueId()) >= 25, "25 Hot-Zone-Kills");
        achievement(gui, 21, Material.ENDER_PEARL, "End Raider", mastery.getEndKills(player.getUniqueId()) >= 10, "10 End-Zone-Kills");
        achievement(gui, 22, Material.MAP, "Explorer", mastery.getSecrets(player.getUniqueId()) >= 5, "5 Secrets finden");

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,slot) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "profile");
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.BOOK,
                UiTheme.PRIMARY + "Achievements",
                UiTheme.MUTED + "Permanente Combat- und Map-Ziele"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void achievement(GuiSession gui, int slot, Material material, String name, boolean unlocked, String condition) {
        gui.setItem(slot, UiItems.item(material,
                (unlocked ? UiTheme.SUCCESS : UiTheme.DISABLED) + name,
                UiTheme.MUTED + condition,
                "",
                unlocked ? UiTheme.STATUS_READY : UiTheme.STATUS_LOCKED));
    }
}
