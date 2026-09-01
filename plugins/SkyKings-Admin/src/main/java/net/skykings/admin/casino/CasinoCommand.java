package net.skykings.admin.casino;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Ingame-Casino mit Coins oder gespeicherten SkyKings-Sternen. */
public final class CasinoCommand implements CommandExecutor {

    private enum Currency { COINS, STARS }
    private enum Game { COIN_FLIP, CROWN_DICE, LUCKY_7, WHEEL }

    private final SkyKingsCoreAPI core;
    private final GuiManager gui;
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, Long> lastPlay = new HashMap<UUID, Long>();

    public CasinoCommand(SkyKingsCoreAPI core) {
        this.core = core;
        this.gui = core.getGuiManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        openHub((Player) sender);
        return true;
    }

    /** Einstieg fuer physische Casino-Stationen/NPCs auf der Void-Crown-Map. */
    public void openStation(Player player, String station) {
        if (player == null) return;
        String normalized = station == null ? "hub" : station.trim().toLowerCase(Locale.ROOT);
        if ("hub".equals(normalized) || "reception".equals(normalized)) {
            openHub(player);
            return;
        }
        if ("jackpot".equals(normalized)) {
            player.closeInventory();
            player.performCommand("jackpot");
            return;
        }
        Game game = parseGame(normalized);
        if (game == null) {
            openHub(player);
            return;
        }
        openCurrencyChoice(player, game);
    }

    public void openHub(final Player player) {
        GuiSession session = GuiSession.create(player, UiTheme.title("Void Crown Casino"), 54);
        session.setItem(4, UiItems.item(Material.NETHER_STAR,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "VOID CROWN CASINO",
                UiTheme.MUTED + "Spiele mit Coins oder SkyKings Sternen.",
                UiTheme.WARNING + "Alle Spiele haben einen kleinen House-Edge.",
                UiTheme.MUTED + "Nur Ingame-Waehrungen."));

        session.setItem(20, UiItems.item(Material.GOLD_INGOT,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "COIN CASINO",
                UiTheme.MUTED + "Kontostand: " + UiTheme.TEXT + UiFormat.number(core.getEconomyService().getBalance(player.getUniqueId())),
                UiItems.action("Spiele mit Coins")), (p,e,s) -> openGames(p, Currency.COINS));

        session.setItem(24, UiItems.item(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "NETHERSTAR LOUNGE",
                UiTheme.MUTED + "Sterne: " + UiTheme.TEXT + UiFormat.number(core.getNetherstarService().getBalance(player.getUniqueId())),
                UiItems.action("Spiele mit Sternen")), (p,e,s) -> openGames(p, Currency.STARS));

        session.setItem(31, UiItems.item(Material.EMERALD_BLOCK,
                ChatColor.GREEN.toString() + ChatColor.BOLD + "SERVER JACKPOT",
                UiTheme.MUTED + "Der bestehende serverweite Jackpot.",
                UiItems.action("Jackpot oeffnen")), (p,e,s) -> {
            p.closeInventory();
            p.performCommand("jackpot");
        });

        session.setItem(49, UiItems.item(Material.PAPER,
                UiTheme.TEXT + "Fair-Play",
                UiTheme.MUTED + "Coin Flip / Dice ca. 95% RTP",
                UiTheme.MUTED + "Lucky 7 ca. 95% RTP",
                UiTheme.MUTED + "Wheel ca. 95,5% RTP"));
        gui.open(session);
        SoundFeedback.menuOpen(player);
    }

    private void openCurrencyChoice(final Player player, final Game game) {
        GuiSession session = GuiSession.create(player, UiTheme.title("Casino | " + plainGameName(game)), 27);
        session.setItem(4, UiItems.item(gameIcon(game), gameName(game),
                UiTheme.MUTED + gameDescription(game),
                UiTheme.MUTED + "Waehle deine Waehrung."));
        session.setItem(11, UiItems.item(Material.GOLD_INGOT,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "COINS",
                UiTheme.MUTED + "Guthaben: " + UiTheme.TEXT + UiFormat.number(balance(player, Currency.COINS)),
                UiItems.action("Coin-Einsatz waehlen")), (p,e,s) -> openBets(p, Currency.COINS, game));
        session.setItem(15, UiItems.item(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "SKYKINGS STERNE",
                UiTheme.MUTED + "Guthaben: " + UiTheme.TEXT + UiFormat.number(balance(player, Currency.STARS)),
                UiItems.action("Sterne-Einsatz waehlen")), (p,e,s) -> openBets(p, Currency.STARS, game));
        session.setItem(22, UiItems.item(Material.ARROW, UiTheme.TEXT + "Casino Hub", UiItems.action("Zurueck")), (p,e,s) -> openHub(p));
        gui.open(session);
        SoundFeedback.menuOpen(player);
    }

    private void openGames(final Player player, final Currency currency) {
        GuiSession session = GuiSession.create(player, UiTheme.title(currency == Currency.COINS ? "Casino | Coins" : "Casino | Sterne"), 54);
        session.setItem(4, UiItems.item(currency == Currency.COINS ? Material.GOLD_INGOT : Material.NETHER_STAR,
                currency == Currency.COINS ? ChatColor.GOLD + "COIN CASINO" : ChatColor.LIGHT_PURPLE + "NETHERSTAR LOUNGE",
                UiTheme.MUTED + "Guthaben: " + UiTheme.TEXT + UiFormat.number(balance(player, currency))));

        game(session, 10, Material.GOLD_NUGGET, ChatColor.YELLOW + "Coin Flip",
                "49% Gewinnchance", "Auszahlung: 1,94x", p -> openBets(p, currency, Game.COIN_FLIP));
        game(session, 12, Material.NETHER_BRICK_ITEM, ChatColor.RED + "Crown Dice",
                "Wuerfel 4-6 gewinnt", "Auszahlung: 1,90x", p -> openBets(p, currency, Game.CROWN_DICE));
        game(session, 14, Material.REDSTONE, ChatColor.LIGHT_PURPLE + "Lucky 7",
                "Zwei Wuerfel muessen 7 ergeben", "Auszahlung: 5,70x", p -> openBets(p, currency, Game.LUCKY_7));
        game(session, 16, Material.RECORD_11, ChatColor.AQUA + "Wheel of Fortune",
                "0x bis 10x Multiplikator", "RTP ca. 95,5%", p -> openBets(p, currency, Game.WHEEL));
        session.setItem(31, UiItems.item(Material.EMERALD_BLOCK, ChatColor.GREEN + "Server Jackpot",
                UiTheme.MUTED + "Gegen andere Spieler statt gegen das Haus.", UiItems.action("Oeffnen")), (p,e,s) -> {
            p.closeInventory(); p.performCommand("jackpot");
        });
        session.setItem(45, UiItems.item(Material.ARROW, UiTheme.TEXT + "Zurueck", UiItems.action("Casino Hub")), (p,e,s) -> openHub(p));
        session.setItem(49, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Void Crown", UiItems.action("Casino Hub")), (p,e,s) -> openHub(p));
        gui.open(session);
        SoundFeedback.menuOpen(player);
    }

    private void openBets(final Player player, final Currency currency, final Game game) {
        GuiSession session = GuiSession.create(player, UiTheme.title("Casino | Einsatz"), 54);
        long[] bets = currency == Currency.COINS
                ? new long[]{50000L, 250000L, 1000000L, 5000000L, 25000000L, 100000000L}
                : new long[]{1L, 5L, 10L, 25L, 50L, 100L};
        int[] slots = {10,12,14,28,30,32};
        for (int i = 0; i < bets.length; i++) {
            final long bet = bets[i];
            session.setItem(slots[i], UiItems.item(currency == Currency.COINS ? Material.GOLD_INGOT : Material.NETHER_STAR,
                    UiTheme.TEXT + "Einsatz: " + UiFormat.number(bet) + unit(currency),
                    UiTheme.MUTED + "Guthaben: " + UiTheme.TEXT + UiFormat.number(balance(player,currency)) + unit(currency),
                    UiItems.action("Jetzt spielen")), (p,e,s) -> play(p,currency,game,bet));
        }
        session.setItem(4, UiItems.item(gameIcon(game), gameName(game),
                UiTheme.MUTED + gameDescription(game), UiTheme.WARNING + "Einsatz wird sofort gebucht."));
        session.setItem(45, UiItems.item(Material.ARROW,UiTheme.TEXT + "Zurueck",UiItems.action("Waehrung")),(p,e,s)->openCurrencyChoice(p,game));
        gui.open(session);
        SoundFeedback.menuOpen(player);
    }

    private synchronized void play(Player player, Currency currency, Game game, long bet) {
        long now = System.currentTimeMillis();
        Long last = lastPlay.get(player.getUniqueId());
        if (last != null && now - last < 750L) {
            player.sendMessage(UiTheme.WARNING + "Warte kurz vor dem naechsten Spiel.");
            SoundFeedback.warning(player);
            return;
        }
        if (bet <= 0L || !withdraw(player,currency,bet)) {
            player.sendMessage(UiTheme.DANGER + "Nicht genug " + (currency == Currency.COINS ? "Coins" : "SkyKings Sterne") + ".");
            SoundFeedback.error(player);
            return;
        }
        lastPlay.put(player.getUniqueId(), now);

        double multiplier;
        String resultText;
        switch (game) {
            case COIN_FLIP:
                boolean crown = random.nextInt(100) < 49;
                multiplier = crown ? 1.94D : 0D;
                resultText = crown ? "KRONE!" : "ZAHL - verloren";
                break;
            case CROWN_DICE:
                int die = random.nextInt(6) + 1;
                multiplier = die >= 4 ? 1.90D : 0D;
                resultText = "Wuerfel: " + die + (die >= 4 ? " - GEWINN!" : " - verloren");
                break;
            case LUCKY_7:
                int a = random.nextInt(6) + 1;
                int b = random.nextInt(6) + 1;
                int sum = a + b;
                multiplier = sum == 7 ? 5.70D : 0D;
                resultText = a + " + " + b + " = " + sum + (sum == 7 ? " - LUCKY 7!" : " - verloren");
                break;
            default:
                int roll = random.nextInt(100);
                if (roll < 40) multiplier = 0D;
                else if (roll < 55) multiplier = 0.5D;
                else if (roll < 80) multiplier = 1D;
                else if (roll < 94) multiplier = 2D;
                else if (roll < 99) multiplier = 5D;
                else multiplier = 10D;
                resultText = "Wheel: " + multiplierText(multiplier);
                break;
        }

        long payout = Math.max(0L, Math.round(bet * multiplier));
        if (payout > 0L) deposit(player,currency,payout,game);
        long net = payout - bet;
        player.closeInventory();
        if (net > 0L) {
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + resultText);
            player.sendMessage(UiTheme.SUCCESS + "+" + UiFormat.number(net) + unit(currency) + " Gewinn");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.9F, 1.45F);
        } else if (net == 0L) {
            player.sendMessage(ChatColor.YELLOW + resultText + ChatColor.GRAY + " • Einsatz zurueck.");
            SoundFeedback.notify(player);
        } else {
            player.sendMessage(ChatColor.RED + resultText);
            player.sendMessage(UiTheme.DANGER + "-" + UiFormat.number(-net) + unit(currency));
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.55F, 0.8F);
        }
        openBets(player,currency,game);
    }

    private boolean withdraw(Player p, Currency c, long amount) {
        if (c == Currency.COINS) return core.getEconomyService().withdraw(p.getUniqueId(),amount,"CASINO","Void Crown Einsatz");
        return core.getNetherstarService().withdraw(p.getUniqueId(),amount,"CASINO","Void Crown Einsatz");
    }

    private void deposit(Player p, Currency c, long amount, Game game) {
        if (c == Currency.COINS) core.getEconomyService().deposit(p.getUniqueId(),amount,"CASINO","Void Crown " + game.name());
        else core.getNetherstarService().deposit(p.getUniqueId(),amount,"CASINO","Void Crown " + game.name());
    }

    private long balance(Player p, Currency c) {
        return c == Currency.COINS ? core.getEconomyService().getBalance(p.getUniqueId()) : core.getNetherstarService().getBalance(p.getUniqueId());
    }

    private void game(GuiSession session,int slot,Material icon,String title,String line1,String line2,OpenAction action) {
        session.setItem(slot,UiItems.item(icon,title,UiTheme.MUTED + line1,UiTheme.MUTED + line2,UiItems.action("Einsatz waehlen")),(p,e,s)->action.open(p));
    }

    private Game parseGame(String value) {
        if ("coinflip".equals(value) || "coin-flip".equals(value) || "flip".equals(value)) return Game.COIN_FLIP;
        if ("dice".equals(value) || "crowndice".equals(value) || "crown-dice".equals(value)) return Game.CROWN_DICE;
        if ("lucky7".equals(value) || "lucky-7".equals(value) || "seven".equals(value)) return Game.LUCKY_7;
        if ("wheel".equals(value) || "fortune".equals(value) || "wheeloffortune".equals(value)) return Game.WHEEL;
        return null;
    }

    private Material gameIcon(Game game) {
        switch (game) {
            case COIN_FLIP: return Material.GOLD_NUGGET;
            case CROWN_DICE: return Material.NETHER_BRICK_ITEM;
            case LUCKY_7: return Material.REDSTONE;
            default: return Material.RECORD_11;
        }
    }

    private String gameName(Game game) {
        switch (game) {
            case COIN_FLIP: return ChatColor.YELLOW + "Coin Flip";
            case CROWN_DICE: return ChatColor.RED + "Crown Dice";
            case LUCKY_7: return ChatColor.LIGHT_PURPLE + "Lucky 7";
            default: return ChatColor.AQUA + "Wheel of Fortune";
        }
    }

    private String plainGameName(Game game) {
        switch (game) {
            case COIN_FLIP: return "Coin Flip";
            case CROWN_DICE: return "Crown Dice";
            case LUCKY_7: return "Lucky 7";
            default: return "Wheel";
        }
    }

    private String gameDescription(Game game) {
        switch (game) {
            case COIN_FLIP: return "49% Chance • 1,94x Auszahlung";
            case CROWN_DICE: return "4-6 gewinnt • 1,90x Auszahlung";
            case LUCKY_7: return "Summe 7 • 5,70x Auszahlung";
            default: return "Gewichtetes Rad • 0x bis 10x";
        }
    }

    private String unit(Currency c) { return c == Currency.COINS ? " Coins" : " Sterne"; }
    private String multiplierText(double d) { return d == Math.rint(d) ? ((long)d) + "x" : String.format(java.util.Locale.US,"%.1fx",d); }
    private interface OpenAction { void open(Player player); }
}
