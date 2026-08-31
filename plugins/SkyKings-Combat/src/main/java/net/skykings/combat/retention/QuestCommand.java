package net.skykings.combat.retention;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
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
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings " + ChatColor.GRAY + "| " + ChatColor.GREEN + "Quests", 45);
        decorate(gui);
        gui.setItem(11, quest(Material.IRON_SWORD, ChatColor.GREEN.toString() + ChatColor.BOLD + "DAILY • PVP JAEGER",
                quests.get(uuid, "daily.kills"), 5, quests.claimed(uuid, "daily.claimed-kills"),
                "150.000 Coins + 2 SkyKings Sterne"));
        gui.setItem(13, quest(Material.ENDER_PEARL, ChatColor.AQUA.toString() + ChatColor.BOLD + "DAILY • PEARL RUNNER",
                quests.get(uuid, "daily.pearls"), 20, quests.claimed(uuid, "daily.claimed-pearls"),
                "75.000 Coins + 1 SkyKings Stern"));
        gui.setItem(15, quest(Material.DIAMOND_SWORD, ChatColor.GOLD.toString() + ChatColor.BOLD + "WEEKLY • KAMPFKOENIG",
                quests.get(uuid, "weekly.kills"), 30, quests.claimed(uuid, "weekly.claimed-kills"),
                "500.000 Coins + 5 SkyKings Sterne"));
        gui.setItem(22, item(Material.BOOK, ChatColor.YELLOW.toString() + ChatColor.BOLD + "DEIN QUESTBOARD",
                ChatColor.GRAY + "Daily Quests resetten jeden Tag.",
                ChatColor.GRAY + "Weekly Quests resetten jede Woche.",
                "",
                ChatColor.GREEN + "Rewards werden automatisch ausgezahlt.",
                ChatColor.DARK_GRAY + "Event-Kills und Kill-Farming zaehlen nicht."));
        gui.setItem(31, item(Material.NETHER_STAR, ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS REWARDS",
                ChatColor.GRAY + "Quests belohnen aktives Gameplay statt AFK-Grind.",
                ChatColor.GRAY + "Sterne sind die physische SkyKings-Waehrung."));
        GuiManager.active().open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.35F);
    }

    private ItemStack quest(Material material, String name, int current, int target, boolean claimed, String reward) {
        int shown = Math.min(current, target);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Fortschritt: " + ChatColor.WHITE + shown + "/" + target);
        lore.add(progress(shown, target));
        lore.add("");
        lore.add(ChatColor.GRAY + "Reward: " + ChatColor.YELLOW + reward);
        lore.add("");
        lore.add(claimed ? ChatColor.GREEN.toString() + ChatColor.BOLD + "ABGESCHLOSSEN ✔"
                : (shown >= target ? ChatColor.GREEN + "Geschafft - Reward wird ausgezahlt" : ChatColor.YELLOW + "Weiter spielen!"));
        meta.setLore(lore); item.setItemMeta(meta); return item;
    }

    private String progress(int current, int target) {
        int filled = Math.min(10, (int) Math.floor((current * 10D) / Math.max(1, target)));
        StringBuilder out = new StringBuilder();
        out.append(ChatColor.GREEN);
        for (int i = 0; i < 10; i++) {
            if (i == filled) out.append(ChatColor.DARK_GRAY);
            out.append('■');
        }
        return out.toString();
    }

    private void decorate(GuiSession gui) {
        ItemStack dark = pane((short) 15, " "); ItemStack green = pane((short) 5, ChatColor.GREEN + "Quests");
        for (int i = 0; i < 45; i++) if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, dark);
        gui.setItem(4, green); gui.setItem(40, green);
    }

    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); item.setItemMeta(meta); return item;
    }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
}
