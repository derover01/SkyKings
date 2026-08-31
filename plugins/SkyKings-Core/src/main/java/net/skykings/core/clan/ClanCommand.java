package net.skykings.core.clan;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.ConfirmationMenu;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/** Clans im zentralen SkyKings UI-System, inklusive kleinem geschuetztem Clan Hub. */
public final class ClanCommand implements CommandExecutor {
    private final ClanService clans;
    private final ClanBaseService bases;

    public ClanCommand(ClanService clans) { this(clans, null); }
    public ClanCommand(ClanService clans, ClanBaseService bases) { this.clans = clans; this.bases = bases; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0]) || "info".equalsIgnoreCase(args[0])) {
            open(player); return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("base".equals(sub)) {
            handleBase(player, args);
            return true;
        }
        if ("create".equals(sub) && args.length >= 3) {
            ClanService.Clan clan = clans.create(player, args[1], args[2]);
            if (clan == null) {
                player.sendMessage(UiTheme.DANGER + "Clan konnte nicht erstellt werden.");
                player.sendMessage(UiTheme.MUTED + "Name 3-16 Zeichen • Tag 2-5 Zeichen • beides muss frei sein.");
                SoundFeedback.error(player);
            } else {
                player.sendMessage(UiTheme.SUCCESS + "Clan erstellt");
                player.sendMessage(UiTheme.TEXT + "[" + clan.getTag() + "] " + clan.getName());
                SoundFeedback.reward(player);
            }
            return true;
        }
        if ("invite".equals(sub) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || target.equals(player)) { error(player, "Spieler nicht gefunden."); return true; }
            if (!clans.invite(player.getUniqueId(), target.getUniqueId())) {
                error(player, "Einladung nicht moeglich. Pruefe Owner, Clanplatz und Zielspieler.");
                return true;
            }
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            player.sendMessage(UiTheme.SUCCESS + "Einladung gesendet");
            target.sendMessage(UiTheme.PRIMARY + "Clan Einladung");
            target.sendMessage(UiTheme.TEXT + "[" + clan.getTag() + "] " + clan.getName());
            target.sendMessage(UiTheme.WARNING + "/clan accept" + UiTheme.MUTED + " oder " + UiTheme.WARNING + "/clan deny");
            SoundFeedback.notify(target);
            return true;
        }
        if ("accept".equals(sub)) {
            ClanService.Clan clan = clans.accept(player.getUniqueId());
            if (clan == null) error(player, "Keine gueltige Clan-Einladung.");
            else {
                player.sendMessage(UiTheme.SUCCESS + "Clan beigetreten");
                player.sendMessage(UiTheme.TEXT + "[" + clan.getTag() + "] " + clan.getName());
                SoundFeedback.reward(player);
                broadcast(clan, UiTheme.TEXT + player.getName() + UiTheme.MUTED + " ist beigetreten.");
            }
            return true;
        }
        if ("deny".equals(sub)) {
            clans.deny(player.getUniqueId());
            player.sendMessage(UiTheme.MUTED + "Clan-Einladung abgelehnt.");
            SoundFeedback.back(player);
            return true;
        }
        if ("leave".equals(sub)) {
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            if (clan == null) { error(player, "Du bist in keinem Clan."); return true; }
            if (clan.isOwner(player.getUniqueId())) { error(player, "Als Owner musst du den Clan aufloesen."); return true; }
            ConfirmationMenu.open(player,
                    UiItems.item(Material.WOOD_DOOR, UiTheme.TEXT + "Clan verlassen", UiTheme.MUTED + clan.getName()),
                    "Clan verlassen",
                    "Du verlaesst " + clan.getName(),
                    () -> leaveConfirmed(player, clan),
                    null);
            return true;
        }
        if ("kick".equals(sub) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { error(player, "Spieler muss online sein."); return true; }
            ClanService.Clan clan = clans.getClan(player.getUniqueId());
            if (!clans.kick(player.getUniqueId(), target.getUniqueId())) { error(player, "Spieler konnte nicht entfernt werden."); return true; }
            target.sendMessage(UiTheme.DANGER + "Du wurdest aus dem Clan entfernt.");
            player.sendMessage(UiTheme.SUCCESS + target.getName() + " wurde entfernt.");
            if (clan != null) broadcast(clan, UiTheme.TEXT + target.getName() + UiTheme.MUTED + " wurde entfernt.");
            return true;
        }
        if ("disband".equals(sub)) {
            final ClanService.Clan clan = clans.getClan(player.getUniqueId());
            if (clan == null || !clan.isOwner(player.getUniqueId())) { error(player, "Nur der Clan-Owner kann den Clan aufloesen."); return true; }
            ConfirmationMenu.open(player,
                    UiItems.item(Material.TNT, UiTheme.DANGER + "Clan aufloesen",
                            UiTheme.MUTED + "[" + clan.getTag() + "] " + clan.getName(),
                            UiTheme.DANGER + "Diese Aktion ist permanent."),
                    "Clan aufloesen",
                    "Clan und Clan Base werden entfernt",
                    () -> disbandConfirmed(player, clan),
                    null);
            return true;
        }
        usage(player); return true;
    }

    private void handleBase(Player player, String[] args) {
        if (bases == null) { error(player, "Clan Base Service ist nicht bereit."); return; }
        ClanService.Clan clan = clans.getClan(player.getUniqueId());
        if (clan == null) { error(player, "Du bist in keinem Clan."); return; }
        if (args.length == 1 || "menu".equalsIgnoreCase(args[1])) { openBase(player, clan); return; }
        if ("create".equalsIgnoreCase(args[1])) {
            if (!clan.isOwner(player.getUniqueId())) { error(player, "Nur der Clan-Owner kann eine Base erstellen."); return; }
            if (!bases.create(player)) { error(player, "Clan Base existiert bereits oder konnte nicht erstellt werden."); return; }
            player.sendMessage(UiTheme.SUCCESS + "Clan Base erstellt");
            player.sendMessage(UiTheme.MUTED + "33x33 geschuetzter Hub in " + ClanBaseService.WORLD_NAME + ".");
            return;
        }
        if ("home".equalsIgnoreCase(args[1]) || "tp".equalsIgnoreCase(args[1])) {
            if (!bases.teleport(player)) error(player, "Dein Clan besitzt noch keine Base.");
            return;
        }
        if ("sethome".equalsIgnoreCase(args[1])) {
            if (!bases.setHome(player)) error(player, "Nur der Owner kann innerhalb der Clan Base das Home setzen.");
            else { player.sendMessage(UiTheme.SUCCESS + "Clan Base Home gesetzt."); SoundFeedback.success(player); }
            return;
        }
        openBase(player, clan);
    }

    private void open(Player player) {
        ClanService.Clan clan = clans.getClan(player.getUniqueId());
        ClanService.Clan invite = clans.pendingInvite(player.getUniqueId());
        GuiSession gui = GuiSession.create(player, UiTheme.title("Clans"), 54);

        if (clan == null) {
            gui.setItem(22, UiItems.item(Material.BANNER,
                    UiTheme.PRIMARY + "Clan gruenden",
                    UiTheme.MUTED + "Erstelle deine eigene Gruppe.",
                    UiTheme.TEXT + "/clan create <Name> <Tag>"));
            if (invite != null) {
                gui.setItem(20, UiItems.item(Material.EMERALD_BLOCK,
                        UiTheme.SUCCESS + "Einladung annehmen",
                        UiTheme.TEXT + "[" + invite.getTag() + "] " + invite.getName(),
                        "",
                        UiItems.action("Klicken zum Annehmen")), (p,e,s) -> {
                    ClanService.Clan accepted = clans.accept(p.getUniqueId());
                    if (accepted != null) { SoundFeedback.reward(p); open(p); }
                    else error(p, "Einladung ist nicht mehr gueltig.");
                });
                gui.setItem(24, UiItems.item(Material.REDSTONE_BLOCK,
                        UiTheme.DANGER + "Einladung ablehnen",
                        UiTheme.TEXT + "[" + invite.getTag() + "] " + invite.getName()), (p,e,s) -> {
                    clans.deny(p.getUniqueId()); SoundFeedback.back(p); open(p);
                });
            }
        } else {
            gui.setItem(4, UiItems.item(Material.DIAMOND,
                    UiTheme.TEXT + "[" + clan.getTag() + "] " + clan.getName(),
                    UiTheme.MUTED + "Mitglieder " + UiTheme.TEXT + clan.getMembers().size() + " / " + ClanService.MAX_MEMBERS,
                    UiTheme.MUTED + "Friendly Fire " + UiTheme.DANGER + "AUS",
                    UiTheme.MUTED + "Rolle " + UiTheme.TEXT + (clan.isOwner(player.getUniqueId()) ? "Owner" : "Mitglied")));

            int slot = 19;
            for (UUID member : clan.getMembers()) {
                if (slot > 34) break;
                OfflinePlayer off = Bukkit.getOfflinePlayer(member);
                String name = off.getName() == null ? member.toString().substring(0, 8) : off.getName();
                gui.setItem(slot++, UiItems.head(name,
                        UiTheme.TEXT + name,
                        member.equals(clan.getOwner()) ? UiTheme.LEGENDARY + "Owner" : UiTheme.MUTED + "Mitglied",
                        off.isOnline() ? UiTheme.SUCCESS + "ONLINE" : UiTheme.DISABLED + "OFFLINE"));
            }

            boolean hasBase = bases != null && bases.get(clan.getId()) != null;
            gui.setItem(38, UiItems.item(Material.CHEST,
                    UiTheme.PRIMARY + "Clan Base",
                    hasBase ? UiTheme.STATUS_READY : UiTheme.STATUS_LOCKED,
                    UiTheme.MUTED + (hasBase ? "Geschuetzter 33x33 Hub" : "Noch nicht erstellt"),
                    "",
                    UiItems.action("Klicken zum Oeffnen")), (p,e,s) -> openBase(p, clan));

            if (clan.isOwner(player.getUniqueId())) {
                gui.setItem(40, UiItems.item(Material.PAPER,
                        UiTheme.PRIMARY + "Spieler einladen",
                        UiTheme.TEXT + "/clan invite <Spieler>"));
                gui.setItem(42, UiItems.item(Material.TNT,
                        UiTheme.DANGER + "Clan aufloesen",
                        UiTheme.MUTED + "Confirmation erforderlich"), (p,e,s) -> onCommand(p, null, "clan", new String[]{"disband"}));
            } else {
                gui.setItem(42, UiItems.item(Material.WOOD_DOOR,
                        UiTheme.DANGER + "Clan verlassen",
                        UiTheme.MUTED + "Confirmation erforderlich"), (p,e,s) -> onCommand(p, null, "clan", new String[]{"leave"}));
            }
        }
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openBase(Player player, ClanService.Clan clan) {
        if (bases == null) { error(player, "Clan Base Service ist nicht bereit."); return; }
        ClanBaseService.BaseData base = bases.get(clan.getId());
        GuiSession gui = GuiSession.create(player, UiTheme.title("Clan Base"), 27);
        if (base == null) {
            gui.setItem(13, UiItems.item(Material.LOCKED_CHEST,
                    UiTheme.MUTED + "Keine Clan Base",
                    UiTheme.MUTED + "Ein kleiner geschuetzter Hub in einer offenen Welt.",
                    UiTheme.TEXT + "33x33 Claim • gemeinsamer Vault",
                    "",
                    clan.isOwner(player.getUniqueId()) ? UiItems.action("Klicken zum Erstellen") : UiTheme.DISABLED + "Owner muss die Base erstellen"),
                    clan.isOwner(player.getUniqueId()) ? (p,e,s) -> {
                        p.closeInventory();
                        if (!bases.create(p)) error(p, "Clan Base konnte nicht erstellt werden.");
                    } : null);
        } else {
            gui.setItem(11, UiItems.item(Material.ENDER_PEARL,
                    UiTheme.PRIMARY + "Zur Clan Base",
                    UiTheme.MUTED + "Teleport zum Clan Hub.",
                    "",
                    UiItems.action("Klicken zum Teleportieren")), (p,e,s) -> { p.closeInventory(); bases.teleport(p); });
            gui.setItem(13, UiItems.item(Material.CHEST,
                    UiTheme.TEXT + "Clan Vault",
                    UiTheme.STATUS_READY,
                    UiTheme.MUTED + "Gemeinsame physische Truhe im Starterraum.",
                    UiTheme.MUTED + "Nur Clanmitglieder haben Zugriff."));
            gui.setItem(15, UiItems.item(Material.COMPASS,
                    UiTheme.TEXT + "Base #" + base.getIndex(),
                    UiTheme.MUTED + "Claim 33x33",
                    UiTheme.MUTED + "Center " + UiTheme.TEXT + base.getCenterX() + ", " + base.getCenterZ(),
                    UiTheme.MUTED + ClanBaseService.WORLD_NAME));
        }
        gui.setItem(18, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); open(p); });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void leaveConfirmed(Player player, ClanService.Clan clan) {
        if (clans.leave(player.getUniqueId())) {
            broadcast(clan, UiTheme.TEXT + player.getName() + UiTheme.MUTED + " hat den Clan verlassen.");
            player.sendMessage(UiTheme.SUCCESS + "Clan verlassen.");
            SoundFeedback.success(player);
        } else error(player, "Clan konnte nicht verlassen werden.");
    }

    private void disbandConfirmed(Player player, ClanService.Clan clan) {
        broadcast(clan, UiTheme.DANGER + "Clan wurde aufgeloest.");
        if (bases != null) bases.remove(clan.getId());
        if (clans.disband(player.getUniqueId())) {
            player.sendMessage(UiTheme.SUCCESS + "Clan aufgeloest.");
            SoundFeedback.warning(player);
        } else error(player, "Clan konnte nicht aufgeloest werden.");
    }

    private void broadcast(ClanService.Clan clan, String message) {
        for (UUID uuid : clan.getMembers()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) online.sendMessage(UiTheme.PRIMARY + "Clan " + message);
        }
    }

    private void usage(Player p) {
        p.sendMessage(UiTheme.TEXT + "Clans");
        p.sendMessage(UiTheme.WARNING + "/clan create <Name> <Tag>");
        p.sendMessage(UiTheme.WARNING + "/clan invite <Spieler> | accept | deny");
        p.sendMessage(UiTheme.WARNING + "/clan base [create|home|sethome]");
        p.sendMessage(UiTheme.WARNING + "/clan leave | kick <Spieler> | disband");
    }

    private void error(Player player, String text) {
        player.sendMessage(UiTheme.DANGER + text);
        SoundFeedback.error(player);
    }
}
