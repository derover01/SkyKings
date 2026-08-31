package net.skykings.combat.map.secret;

import net.skykings.combat.map.MapLootTier;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;

/** Staff-Setup fuer versteckte Loot-Caches. */
public final class SecretLootRoomCommand implements CommandExecutor {
    private final SecretLootRoomService service;

    public SecretLootRoomCommand(SecretLootRoomService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.secretroom")) {
            player.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) { usage(player); return true; }

        if ("add".equalsIgnoreCase(args[0])) {
            if (args.length < 3) { usage(player); return true; }
            Block target = player.getTargetBlock((HashSet<Byte>) null, 6);
            if (target == null || target.getType() != Material.CHEST) {
                player.sendMessage(UiTheme.DANGER + "Schau eine Truhe innerhalb von 6 Bloecken an.");
                SoundFeedback.error(player);
                return true;
            }
            MapLootTier tier = MapLootTier.parse(args[2]);
            if (tier == null || tier == MapLootTier.SUPPLY) {
                player.sendMessage(UiTheme.DANGER + "Tier: common, rare oder epic.");
                return true;
            }
            long minutes = tier.getCooldownMillis() / 60_000L;
            if (args.length >= 4) {
                try { minutes = Math.max(1L, Long.parseLong(args[3])); }
                catch (NumberFormatException ex) { player.sendMessage(UiTheme.DANGER + "Cooldown muss in Minuten angegeben werden."); return true; }
            }
            if (!service.add(args[1], target, tier, minutes)) {
                player.sendMessage(UiTheme.DANGER + "Secret Cache konnte nicht erstellt werden.");
                return true;
            }
            player.sendMessage(UiTheme.SUCCESS + "Secret Cache gespeichert.");
            player.sendMessage(UiTheme.MUTED + tier.getDisplay() + " • " + minutes + " Minuten Cooldown");
            SoundFeedback.success(player);
            return true;
        }

        if ("remove".equalsIgnoreCase(args[0]) && args.length >= 2) {
            if (service.remove(args[1])) {
                player.sendMessage(UiTheme.SUCCESS + "Secret Cache entfernt.");
                SoundFeedback.back(player);
            } else player.sendMessage(UiTheme.DANGER + "Secret Cache nicht gefunden.");
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            player.sendMessage(UiTheme.TEXT + "Secret Caches");
            if (service.getRooms().isEmpty()) player.sendMessage(UiTheme.MUTED + "Keine Caches eingerichtet.");
            for (SecretLootRoomService.Room room : service.getRooms().values()) {
                long seconds = Math.max(0L, (room.nextReady - System.currentTimeMillis() + 999L) / 1000L);
                player.sendMessage(UiTheme.PRIMARY + room.id + UiTheme.MUTED + " • " + room.tier.getDisplay()
                        + " • " + (seconds <= 0 ? UiTheme.SUCCESS + "READY" : UiTheme.WARNING + UiFormat.durationSeconds(seconds)));
            }
            return true;
        }
        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(UiTheme.TEXT + "Secret Loot Rooms");
        player.sendMessage(UiTheme.WARNING + "/secretroom add <ID> <common|rare|epic> [Minuten]");
        player.sendMessage(UiTheme.WARNING + "/secretroom remove <ID>");
        player.sendMessage(UiTheme.WARNING + "/secretroom list");
    }
}
