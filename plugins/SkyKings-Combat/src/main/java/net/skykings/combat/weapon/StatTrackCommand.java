package net.skykings.combat.weapon;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** StatTrack anwenden, ansehen und fuer Staff ausgeben. */
public final class StatTrackCommand implements CommandExecutor {
    private final StatTrackItemService service;

    public StatTrackCommand(StatTrackItemService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            open(player);
            return true;
        }
        if ("apply".equalsIgnoreCase(args[0])) {
            ItemStack weapon = player.getItemInHand();
            if (!service.isTrackableWeapon(weapon)) {
                player.sendMessage(UiTheme.DANGER + "Halte ein Schwert, eine Axt oder einen Bogen in der Hand.");
                SoundFeedback.error(player);
                return true;
            }
            if (service.hasStatTrack(weapon)) {
                player.sendMessage(UiTheme.WARNING + "Diese Waffe besitzt bereits StatTrack.");
                return true;
            }
            if (!service.apply(player, weapon)) {
                player.sendMessage(UiTheme.DANGER + "Dir fehlt ein StatTrack Module.");
                SoundFeedback.error(player);
                return true;
            }
            player.sendMessage(UiTheme.SUCCESS + "StatTrack aktiviert.");
            player.sendMessage(UiTheme.MUTED + "Legitime PvP-Kills werden ab jetzt direkt auf der Waffe gespeichert.");
            return true;
        }
        if ("give".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("skykings.admin.stattrack")) {
                player.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(UiTheme.TEXT + "StatTrack ausgeben");
                player.sendMessage(UiTheme.WARNING + "/stattrack give <Spieler> [Menge]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden.");
                return true;
            }
            int amount = 1;
            if (args.length >= 3) {
                try { amount = Math.max(1, Math.min(64, Integer.parseInt(args[2]))); }
                catch (NumberFormatException ex) { amount = 1; }
            }
            target.getInventory().addItem(service.createModule(amount));
            target.updateInventory();
            target.sendMessage(UiTheme.PRIMARY + "StatTrack Module erhalten");
            SoundFeedback.reward(target);
            player.sendMessage(UiTheme.SUCCESS + "StatTrack Module ausgegeben.");
            return true;
        }
        player.sendMessage(UiTheme.TEXT + "Weapon StatTrack");
        player.sendMessage(UiTheme.WARNING + "/stattrack" + UiTheme.MUTED + " - Weapon History");
        player.sendMessage(UiTheme.WARNING + "/stattrack apply" + UiTheme.MUTED + " - Module anwenden");
        return true;
    }

    private void open(Player player) {
        ItemStack weapon = player.getItemInHand();
        StatTrackItemService.StatData data = service.decode(weapon);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Weapon History"), 27);
        if (data == null) {
            gui.setItem(13, UiItems.item(Material.NAME_TAG,
                    UiTheme.MUTED + "Kein StatTrack",
                    UiTheme.MUTED + "Halte eine StatTrack-Waffe in der Hand.",
                    "",
                    UiTheme.WARNING + "/stattrack apply"));
        } else {
            ItemStack display = weapon.clone();
            display.setAmount(1);
            gui.setItem(13, display);
            gui.setItem(22, UiItems.item(Material.BOOK,
                    UiTheme.PRIMARY + "History",
                    UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(data.getKills()),
                    UiTheme.MUTED + "Owner " + UiTheme.TEXT + data.getCurrentOwnerName(),
                    UiTheme.MUTED + "ID " + UiTheme.DISABLED + data.getId().toString().substring(0, 8),
                    "",
                    UiTheme.MUTED + "Die Werte bleiben beim Trading auf dem Item."));
        }
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }
}
