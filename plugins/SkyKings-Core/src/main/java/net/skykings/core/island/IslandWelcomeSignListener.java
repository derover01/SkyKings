package net.skykings.core.island;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

/** [Welcome]-Schilder schalten eine Insel fuer /is visit frei und sind zugleich der Besuchspunkt. */
public final class IslandWelcomeSignListener implements Listener {
    private final IslandService islands;

    public IslandWelcomeSignListener(IslandService islands) {
        this.islands = islands;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        String first = event.getLine(0);
        if (first == null || !"[welcome]".equalsIgnoreCase(ChatColor.stripColor(first).trim())) return;

        Player player = event.getPlayer();
        IslandService.IslandData island = islands.findAt(event.getBlock().getLocation());
        if (island == null || !island.owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Ein [Welcome]-Schild kannst du nur auf deiner eigenen Insel setzen.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
            return;
        }

        if (!islands.setWelcome(player.getUniqueId(), event.getBlock().getLocation(), player.getLocation().getYaw(), 0F)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Der Welcome-Punkt konnte nicht gespeichert werden.");
            return;
        }

        event.setLine(0, ChatColor.GREEN + "[Welcome]");
        event.setLine(1, ChatColor.WHITE + player.getName());
        event.setLine(2, ChatColor.GRAY + "SkyKings Island");
        event.setLine(3, ChatColor.AQUA + "Willkommen!");
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS ISLANDS "
                + ChatColor.GREEN + "Deine Insel ist jetzt besuchbar.");
        player.sendMessage(ChatColor.GRAY + "Besucher landen direkt an diesem Schild. Entfernst du es, wird die Insel wieder privat.");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.5F);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        if (type != Material.SIGN_POST && type != Material.WALL_SIGN) return;
        IslandService.IslandData island = islands.findAt(block.getLocation());
        if (island == null) return;
        if (islands.clearWelcomeAt(island.owner, block.getLocation())) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.YELLOW + "Welcome-Schild entfernt. " + ChatColor.GRAY + "Die Insel ist wieder privat.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 0.8F);
        }
    }
}
