package net.skykings.core.clan;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/** /clan mit Oldschool-Funktionen und visuellem SkyKings-Menue. */
public final class ClanCommand implements CommandExecutor {
    private final ClanService clans;

    public ClanCommand(ClanService clans) { this.clans = clans; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0]) || "info".equalsIgnoreCase(args[0])) {
            open(player); return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("create".equals(sub) && args.length >= 3) {
            ClanService.Clan clan = clans.create(player, args[1], args[2]);
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "Clan konnte nicht erstellt werden. Name 3-16 Zeichen, Tag 2-5 Zeichen; beides muss frei sein.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            } else {
                player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "CLAN ERSTELLT! " + ChatColor.YELLOW + "[" + clan.getTag() + "] " + clan.getName());
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.45F);
            }
            return true;
        }
        if ("invite".equals(sub) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || target.equals(player)) { player.sendMessage(ChatColor.RED + "Spieler nicht gefunden."); return true; }
            if (!clans.invite(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Einladung nicht moeglich. Du musst Clan-Owner sein, der Spieler clanlos und der Clan darf nicht voll sein.");
                return true;
            }
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Einladung an " + target.getName() + " gesendet.");
            target.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "CLAN EINLADUNG " + ChatColor.YELLOW + "[" + clan.getTag() + "] " + clan.getName());
            target.sendMessage(ChatColor.GREEN + "/clan accept " + ChatColor.GRAY + "oder " + ChatColor.RED + "/clan deny");
            target.playSound(target.getLocation(), Sound.NOTE_PLING, 0.8F, 1.6F);
            return true;
        }
        if ("accept".equals(sub)) {
            ClanService.Clan clan = clans.accept(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "Keine gueltige Clan-Einladung vorhanden.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            } else {
                player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "CLAN BEIGETRETEN! " + ChatColor.YELLOW + "[" + clan.getTag() + "] " + clan.getName());
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.4F);
                broadcast(clan, ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " ist dem Clan beigetreten.");
            }
            return true;
        }
        if ("deny".equals(sub)) {
            clans.deny(player.getUniqueId()); player.sendMessage(ChatColor.YELLOW + "Clan-Einladung abgelehnt.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.6F, 0.7F); return true;
        }
        if ("leave".equals(sub)) {
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            if (clan == null) { player.sendMessage(ChatColor.RED + "Du bist in keinem Clan."); return true; }
            if (clan.isOwner(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Als Owner musst du /clan disband verwenden."); return true;
            }
            if (clans.leave(player.getUniqueId())) {
                broadcast(clan, ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " hat den Clan verlassen.");
                player.sendMessage(ChatColor.YELLOW + "Du hast den Clan verlassen.");
            }
            return true;
        }
        if ("kick".equals(sub) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            if (!clans.kick(player.getUniqueId(), target.getUniqueId())) { player.sendMessage(ChatColor.RED + "Kick nicht moeglich."); return true; }
            target.sendMessage(ChatColor.RED + "Du wurdest aus dem Clan entfernt.");
            player.sendMessage(ChatColor.YELLOW + target.getName() + " wurde aus dem Clan entfernt.");
            if (clan != null) broadcast(clan, ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " wurde aus dem Clan entfernt.");
            return true;
        }
        if ("disband".equals(sub)) {
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            if (clan == null || !clan.isOwner(player.getUniqueId())) { player.sendMessage(ChatColor.RED + "Nur der Clan-Owner kann den Clan aufloesen."); return true; }
            broadcast(clan, ChatColor.RED.toString() + ChatColor.BOLD + "Der Clan wurde aufgeloest.");
            clans.disband(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.ANVIL_BREAK, 0.6F, 0.8F);
            return true;
        }
        usage(player); return true;
    }

    private void open(Player player) {
        ClanService.Clan clan = clans.getClan(player.getUniqueId());
        ClanService.Clan invite = clans.pendingInvite(player.getUniqueId());
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings " + ChatColor.GRAY + "| " + ChatColor.GOLD + "Clan", 54);
        decorate(gui);
        if (clan == null) {
            gui.setItem(22, item(Material.BANNER, ChatColor.GOLD.toString() + ChatColor.BOLD + "DEINEN CLAN GRUENDEN",
                    ChatColor.GRAY + "Erstelle deine eigene Gruppe im Koenigreich.", "",
                    ChatColor.YELLOW + "/clan create <Name> <Tag>",
                    ChatColor.DARK_GRAY + "Name: 3-16 Zeichen • Tag: 2-5 Zeichen"));
            if (invite != null) {
                gui.setItem(20, item(Material.EMERALD_BLOCK, ChatColor.GREEN.toString() + ChatColor.BOLD + "EINLADUNG ANNEHMEN",
                        ChatColor.YELLOW + "[" + invite.getTag() + "] " + invite.getName(), "", ChatColor.GREEN + "Klicken"), (p,e,s) -> {
                    p.closeInventory(); onCommand(p, null, "clan", new String[]{"accept"});
                });
                gui.setItem(24, item(Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "EINLADUNG ABLEHNEN",
                        ChatColor.YELLOW + "[" + invite.getTag() + "] " + invite.getName(), "", ChatColor.RED + "Klicken"), (p,e,s) -> {
                    p.closeInventory(); onCommand(p, null, "clan", new String[]{"deny"});
                });
            }
        } else {
            gui.setItem(4, item(Material.DIAMOND, ChatColor.GOLD.toString() + ChatColor.BOLD + "[" + clan.getTag() + "] " + clan.getName(),
                    ChatColor.GRAY + "Mitglieder: " + ChatColor.WHITE + clan.getMembers().size() + "/" + ClanService.MAX_MEMBERS,
                    ChatColor.GRAY + "Friendly Fire: " + ChatColor.RED + "AUS",
                    ChatColor.GRAY + "Deine Rolle: " + (clan.isOwner(player.getUniqueId()) ? ChatColor.GOLD + "Owner" : ChatColor.WHITE + "Mitglied")));
            int slot = 19;
            for (UUID member : clan.getMembers()) {
                if (slot > 34) break;
                OfflinePlayer off = Bukkit.getOfflinePlayer(member);
                String name = off.getName() == null ? member.toString().substring(0, 8) : off.getName();
                gui.setItem(slot++, head(name,
                        (member.equals(clan.getOwner()) ? ChatColor.GOLD : ChatColor.GREEN) + name,
                        member.equals(clan.getOwner()) ? ChatColor.GOLD + "Clan-Owner" : ChatColor.GRAY + "Mitglied",
                        off.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));
            }
            if (clan.isOwner(player.getUniqueId())) {
                gui.setItem(40, item(Material.PAPER, ChatColor.YELLOW.toString() + ChatColor.BOLD + "SPIELER EINLADEN",
                        ChatColor.AQUA + "/clan invite <Spieler>"));
            } else {
                gui.setItem(40, item(Material.WOOD_DOOR, ChatColor.RED.toString() + ChatColor.BOLD + "CLAN VERLASSEN",
                        ChatColor.GRAY + "Befehl: " + ChatColor.RED + "/clan leave"));
            }
        }
        GuiManager.active().open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.25F);
    }

    private void broadcast(ClanService.Clan clan, String message) {
        for (UUID uuid : clan.getMembers()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) online.sendMessage(ChatColor.GOLD + "[Clan] " + message);
        }
    }

    private void usage(Player p) {
        p.sendMessage(ChatColor.DARK_GRAY + "---------------- " + ChatColor.GOLD + ChatColor.BOLD + "SKYKINGS CLAN" + ChatColor.DARK_GRAY + " ----------------");
        p.sendMessage(ChatColor.GOLD + "/clan" + ChatColor.GRAY + " - Clan-Menue");
        p.sendMessage(ChatColor.GOLD + "/clan create <Name> <Tag>");
        p.sendMessage(ChatColor.GOLD + "/clan invite <Spieler> | accept | deny");
        p.sendMessage(ChatColor.GOLD + "/clan leave | kick <Spieler> | disband");
    }

    private void decorate(GuiSession gui) {
        ItemStack dark = pane((short) 15, " "); ItemStack gold = pane((short) 4, ChatColor.GOLD + "Clan");
        for (int i = 0; i < 54; i++) if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, dark);
        gui.setItem(0, gold); gui.setItem(8, gold); gui.setItem(45, gold); gui.setItem(53, gold);
    }
    private ItemStack head(String owner, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta(); meta.setOwner(owner); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); item.setItemMeta(meta); return item;
    }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
}
