package net.skykings.combat.community;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /friede bzw. /peace mit gegenseitiger Zustimmung und visuellem Hauptmenue. */
public final class PeaceCommand implements CommandExecutor {
    private final PeaceService peace;
    private final Map<UUID, UUID> requests = new HashMap<UUID, UUID>();

    public PeaceCommand(PeaceService peace) { this.peace = peace; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0]) || "list".equalsIgnoreCase(args[0])) {
            open(player); return true;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("accept".equals(sub)) { accept(player); return true; }
        if ("deny".equals(sub)) { deny(player); return true; }
        if ("remove".equals(sub) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            remove(player, target); return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return true;
        }
        request(player, target);
        return true;
    }

    private void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings " + ChatColor.GRAY + "| " + ChatColor.AQUA + "Friede", 54);
        decorate(gui);
        UUID pending = requests.get(player.getUniqueId());
        gui.setItem(4, item(Material.GOLDEN_APPLE, ChatColor.AQUA.toString() + ChatColor.BOLD + "FRIEDE",
                ChatColor.GRAY + "Aktive Friedensverbindungen: " + ChatColor.WHITE + peace.countFor(player.getUniqueId()),
                "",
                ChatColor.GRAY + "Friedenspartner koennen sich gegenseitig nicht verletzen."));

        if (pending != null) {
            OfflinePlayer from = Bukkit.getOfflinePlayer(pending);
            gui.setItem(20, item(Material.EMERALD_BLOCK, ChatColor.GREEN.toString() + ChatColor.BOLD + "ANFRAGE ANNEHMEN",
                    ChatColor.GRAY + "Von: " + ChatColor.WHITE + safeName(from), "", ChatColor.GREEN + "Klicken zum Annehmen"), (p,e,s) -> accept(p));
            gui.setItem(24, item(Material.REDSTONE_BLOCK, ChatColor.RED.toString() + ChatColor.BOLD + "ANFRAGE ABLEHNEN",
                    ChatColor.GRAY + "Von: " + ChatColor.WHITE + safeName(from), "", ChatColor.RED + "Klicken zum Ablehnen"), (p,e,s) -> deny(p));
        } else {
            gui.setItem(22, item(Material.PAPER, ChatColor.YELLOW.toString() + ChatColor.BOLD + "SPIELER EINLADEN",
                    ChatColor.GRAY + "Sende mit folgendem Befehl eine Friedensanfrage:", "",
                    ChatColor.AQUA + "/friede <Spieler>", "",
                    ChatColor.DARK_GRAY + "Beide Spieler muessen zustimmen."));
        }

        List<UUID> partners = peace.partners(player.getUniqueId());
        int slot = 28;
        for (UUID uuid : partners) {
            if (slot > 34) break;
            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
            final String name = safeName(off);
            ItemStack skull = playerHead(name, ChatColor.GREEN + name,
                    ChatColor.GRAY + "Status: " + (off.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"),
                    "", ChatColor.YELLOW + "Zum Beenden: /friede remove " + name);
            gui.setItem(slot++, skull);
        }
        if (partners.isEmpty()) {
            gui.setItem(31, item(Material.BARRIER, ChatColor.GRAY + "Noch keine Friedenspartner",
                    ChatColor.DARK_GRAY + "Nutze /friede <Spieler>."));
        }
        GuiManager.active().open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.45F);
    }

    private void request(Player player, Player target) {
        if (peace.isPeace(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ihr habt bereits Frieden."); return;
        }
        requests.put(target.getUniqueId(), player.getUniqueId());
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "FRIEDE " + ChatColor.GREEN + "Anfrage an " + target.getName() + " gesendet.");
        target.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "FRIEDENSANFRAGE");
        target.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GRAY + " moechte Frieden mit dir schliessen.");
        target.sendMessage(ChatColor.GREEN + "/friede accept " + ChatColor.DARK_GRAY + "• " + ChatColor.RED + "/friede deny");
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.6F, 1.3F);
        target.playSound(target.getLocation(), Sound.NOTE_PLING, 0.8F, 1.6F);
    }

    private void accept(Player player) {
        UUID from = requests.remove(player.getUniqueId());
        if (from == null) {
            player.sendMessage(ChatColor.RED + "Keine offene Friedensanfrage.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return;
        }
        peace.add(player.getUniqueId(), from);
        Player other = Bukkit.getPlayer(from);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "FRIEDEN GESCHLOSSEN!"
                + (other == null ? "" : ChatColor.GRAY + " mit " + ChatColor.WHITE + other.getName()));
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.45F);
        if (other != null) {
            other.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "FRIEDEN GESCHLOSSEN! " + ChatColor.WHITE + player.getName() + ChatColor.GRAY + " hat angenommen.");
            other.playSound(other.getLocation(), Sound.LEVEL_UP, 0.8F, 1.45F);
        }
    }

    private void deny(Player player) {
        UUID from = requests.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(from == null ? ChatColor.RED + "Keine offene Friedensanfrage." : ChatColor.YELLOW + "Friedensanfrage abgelehnt.");
        player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 0.75F);
    }

    private void remove(Player player, Player target) {
        if (peace.remove(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Frieden mit " + target.getName() + " beendet.");
            target.sendMessage(ChatColor.YELLOW + player.getName() + " hat euren Frieden beendet.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 0.75F);
        } else player.sendMessage(ChatColor.RED + "Ihr habt keinen aktiven Frieden.");
    }

    private void decorate(GuiSession gui) {
        ItemStack dark = pane((short) 15, " "); ItemStack cyan = pane((short) 9, ChatColor.AQUA + "Friede");
        for (int i = 0; i < 54; i++) if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, dark);
        gui.setItem(9, cyan); gui.setItem(17, cyan); gui.setItem(36, cyan); gui.setItem(44, cyan);
    }

    private ItemStack playerHead(String owner, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta(); meta.setOwner(owner); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); item.setItemMeta(meta); return item;
    }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
    private String safeName(OfflinePlayer player) { return player.getName() == null ? player.getUniqueId().toString().substring(0, 8) : player.getName(); }
}
