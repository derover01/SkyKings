package net.skykings.combat.retention;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /quests als visuelles Daily-/Weekly-Questboard. */
public final class QuestCommand implements CommandExecutor {
    private final QuestService quests;

    public QuestCommand(QuestService quests) { this.quests = quests; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        open((Player) sender);
        return true;
    }

    private void open(Player player) {
        UUID uuid = player.getUniqueId();
        GuiSession gui = GuiSession.create(player, UiTheme.title("Quests"), 54);
        gui.setItem(11, quest(Material.IRON_SWORD, UiTheme.SUCCESS + "Daily • PvP Jaeger",
                quests.get(uuid, "daily.kills"), 5, quests.claimed(uuid, "daily.claimed-kills"),
                "150.000 Coins + 2 SkyKings Sterne"));
        gui.setItem(13, quest(Material.ENDER_PEARL, UiTheme.PRIMARY + "Daily • Pearl Runner",
                quests.get(uuid, "daily.pearls"), 20, quests.claimed(uuid, "daily.claimed-pearls"),
                "75.000 Coins + 1 SkyKings Stern"));
        gui.setItem(15, quest(Material.DIAMOND_SWORD, UiTheme.LEGENDARY + "Weekly • Kampfkoenig",
                quests.get(uuid, "weekly.kills"), 30, quests.claimed(uuid, "weekly.claimed-kills"),
                "500.000 Coins + 5 SkyKings Sterne"));
        gui.setItem(31, UiItems.item(Material.BOOK,
                UiTheme.PRIMARY + "Questboard",
                UiTheme.MUTED + "Daily: taeglicher Reset",
                UiTheme.MUTED + "Weekly: woechentlicher Reset",
                UiTheme.MUTED + "Event-Kills und Farming zaehlen nicht."));
        gui.setItem(33, UiItems.item(Material.NETHER_STAR,
                UiTheme.PRIMARY + "SkyKings Rewards",
                UiTheme.MUTED + "Aktives Gameplay wird belohnt.",
                UiTheme.MUTED + "Sterne bleiben physische Waehrung."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "profile");
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS,
                UiTheme.PRIMARY + "Quests",
                UiTheme.MUTED + "Daily • Weekly • Rewards"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private ItemStack quest(Material material, String name, int current, int target, boolean claimed, String reward) {
        int shown = Math.min(current, target);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<String>();
        lore.add(UiTheme.MUTED + "Fortschritt " + UiTheme.TEXT + shown + "/" + target);
        lore.add(progress(shown, target));
        lore.add("");
        lore.add(UiTheme.MUTED + "Reward " + UiTheme.WARNING + reward);
        lore.add("");
        lore.add(claimed ? UiTheme.STATUS_COMPLETED
                : (shown >= target ? UiTheme.STATUS_READY : UiTheme.WARNING + "IN PROGRESS"));
        meta.setLore(UiItems.wrapLore(lore.toArray(new String[0])));
        item.setItemMeta(meta);
        return item;
    }

    private String progress(int current, int target) {
        int filled = Math.min(10, (int) Math.floor((current * 10D) / Math.max(1, target)));
        StringBuilder out = new StringBuilder();
        out.append(ChatColor.AQUA);
        for (int i = 0; i < 10; i++) {
            if (i == filled) out.append(ChatColor.DARK_GRAY);
            out.append('■');
        }
        return out.toString();
    }
}
