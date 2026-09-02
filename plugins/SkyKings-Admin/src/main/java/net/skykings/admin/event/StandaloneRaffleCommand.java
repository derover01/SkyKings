package net.skykings.admin.event;

import net.skykings.core.sound.SoundFeedback;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Globale Staff-Verlosung, die unabhaengig vom Freitags-Event funktioniert.
 * /verlosen fertig bleibt fuer den Freitag-Drop-Flow kompatibel.
 */
public final class StandaloneRaffleCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final FridayEventService friday;
    private final Random random = new Random();
    private boolean running;

    public StandaloneRaffleCommand(JavaPlugin plugin, FridayEventService friday) {
        this.plugin = plugin;
        this.friday = friday;
    }

    @Override
    public synchronized boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skykings.admin.friday")) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }

        if (args.length > 0 && "fertig".equalsIgnoreCase(args[0])) {
            if (running) {
                sender.sendMessage(ChatColor.YELLOW + "Warte, bis die aktuelle Verlosung beendet ist.");
                return true;
            }
            return friday.onCommand(sender, command, label, args);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "/verlosen muss ingame mit dem Gewinn in der Hand benutzt werden.");
            return true;
        }
        if (running) {
            sender.sendMessage(ChatColor.YELLOW + "Es laeuft bereits eine Verlosung.");
            return true;
        }

        Player admin = (Player) sender;
        ItemStack hand = admin.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR || hand.getAmount() <= 0) {
            admin.sendMessage(ChatColor.RED + "Halte den kompletten Gewinn-Stack in der Hand und nutze /verlosen.");
            SoundFeedback.error(admin);
            return true;
        }

        start(hand.clone());
        return true;
    }

    private synchronized void start(final ItemStack prize) {
        running = true;
        final String prizeName = describe(prize);
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "✦ VERLOSUNG ✦");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Es wird " + ChatColor.WHITE + prizeName + ChatColor.YELLOW + " verlost!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Alle aktuell Online-Spieler sind automatisch dabei.");
        playAll(Sound.NOTE_PLING, 0.7F, 1.0F);

        for (int i = 3; i >= 1; i--) {
            final int seconds = i;
            long delay = (3 - i) * 20L + 20L;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() {
                    synchronized (StandaloneRaffleCommand.this) {
                        if (!running) return;
                    }
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Ziehung in " + ChatColor.WHITE + seconds + ChatColor.GOLD + "...");
                    playAll(Sound.NOTE_PLING, 0.75F, 1.05F + (3 - seconds) * 0.2F);
                }
            }, delay);
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() { finish(prize, prizeName); }
        }, 85L);
    }

    private synchronized void finish(ItemStack prize, String prizeName) {
        if (!running) return;
        List<Player> players = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) if (player.isOnline()) players.add(player);
        if (players.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Die Verlosung wurde beendet: niemand ist online.");
        } else {
            Player winner = players.get(random.nextInt(players.size()));
            giveOrDrop(winner, prize.clone());
            Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + winner.getName()
                    + ChatColor.YELLOW + " gewinnt " + ChatColor.WHITE + prizeName + ChatColor.YELLOW + "!");
            playAll(Sound.LEVEL_UP, 0.9F, 1.45F);
            winner.playSound(winner.getLocation(), Sound.FIREWORK_LAUNCH, 0.8F, 1.2F);
        }
        running = false;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        for (ItemStack value : leftovers.values()) player.getWorld().dropItemNaturally(player.getLocation(), value);
        player.updateInventory();
    }

    private String describe(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        String name = meta != null && meta.hasDisplayName()
                ? meta.getDisplayName()
                : stack.getType().name().toLowerCase().replace('_', ' ');
        return stack.getAmount() + "x " + name;
    }

    private void playAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
