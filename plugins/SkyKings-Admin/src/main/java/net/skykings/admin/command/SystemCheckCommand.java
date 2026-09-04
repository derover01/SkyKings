package net.skykings.admin.command;

import net.skykings.admin.casino.CasinoSettlementJournal;
import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.event.EventReturnRecoveryService;
import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.discord.DiscordNotifier;
import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import net.skykings.core.resourcepack.ResourcePackService;
import net.skykings.core.shop.player.PlayerShopPurchaseJournal;
import net.skykings.core.shop.player.PlayerShopRevenueClaimJournal;
import net.skykings.core.shop.player.PlayerShopStore;
import net.skykings.core.trade.TradeEscrowJournal;
import net.skykings.crates.RewardSettlementJournal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Schneller Phase-10 Runtime-Check fuer Staff nach Deploy/Restart. */
public final class SystemCheckCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skykings.admin.systemcheck")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS SYSTEM CHECK");
        sender.sendMessage(ChatColor.DARK_GRAY + "Server " + Bukkit.getVersion() + " | Java " + System.getProperty("java.version"));

        sender.sendMessage(ChatColor.AQUA + "Module & Services");
        plugin(sender, "SkyKings-Core");
        plugin(sender, "SkyKings-Combat");
        plugin(sender, "SkyKings-Crates");
        plugin(sender, "SkyKings-Admin");
        plugin(sender, "LuckPerms");
        plugin(sender, "Vault");
        SkyKingsCoreAPI coreApi = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        check(sender, coreApi != null, "Core API");
        check(sender, coreApi != null && coreApi.getClanService() != null, "Clan Service");
        check(sender, coreApi != null && coreApi.getEconomyService() != null, "Economy Service");
        check(sender, coreApi != null && coreApi.getNetherstarService() != null, "Netherstar Service");
        check(sender, coreApi != null && coreApi.getRankService() != null, "Rank Service");
        check(sender, coreApi != null && coreApi.getVoucherPermissionService() != null, "Voucher Permission Service");
        check(sender, Bukkit.getServicesManager().load(IslandAccessService.class) != null, "Island Access API");
        check(sender, Bukkit.getServicesManager().load(PlotAccessService.class) != null, "Plot Access API");
        check(sender, CombatTagServiceImpl.liveInstance() != null, "CombatTag Live Service");
        check(sender, EventParticipationService.global() != null, "Event Participation Runtime");
        DiscordNotifier discord = Bukkit.getServicesManager().load(DiscordNotifier.class);
        check(sender, discord != null, "Discord Bridge");

        sender.sendMessage(ChatColor.AQUA + "Kritische Commands");
        String[] commands = {
                "is", "plot", "warp", "craterewards", "battlepass", "premiumpass", "quests", "kit", "prefix",
                "duel", "lms", "clanwar", "eventarena", "skymap", "casino", "jackpot", "playershop", "pack",
                "verlosen", "freitag", "casinonpc", "addcoins", "setcoins"
        };
        for (String commandName : commands) command(sender, commandName);

        sender.sendMessage(ChatColor.AQUA + "Offizielle Maps");
        check(sender, Bukkit.getWorld("SkyPvP") != null, "SkyPvP Produktionswelt");
        check(sender, Bukkit.getWorld("SkyIslands") != null, "SkyIslands Welt");
        check(sender, Bukkit.getWorld("SkyPlots") != null, "SkyPlots Welt");
        check(sender, Bukkit.getWorld("SkyCommunityEvent") != null, "SkyCommunityEvent Welt");

        sender.sendMessage(ChatColor.AQUA + "Persistenz & Recovery");
        eventReturnRecovery(sender);
        casinoSettlementRecovery(sender);
        rewardSettlementRecovery(sender);
        jackpotRecovery(sender);
        tradeEscrowRecovery(sender);
        playerShopPurchaseRecovery(sender);
        playerShopRevenueClaimRecovery(sender);
        playerShopLegacyReview(sender);
        resourcePack(sender);
        fileCheck(sender, "SkyKings-Core", "island-starter-claims.txt", "Island Starter-Claim Store", true);
        fileCheck(sender, "SkyKings-Crates", "issued-items.txt", "Issued Item Registry", true);
        fileCheck(sender, "SkyKings-Crates", "redeemed-vouchers.txt", "Voucher Redemption Store", true);

        EventParticipationService participation = EventParticipationService.global();
        int eventPlayers = participation == null ? 0 : participation.snapshot().size();
        sender.sendMessage(ChatColor.GRAY + "Aktive Event-Spieler: " + ChatColor.WHITE + eventPlayers);
        if (discord == null || !discord.isEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "[OPTIONAL]" + ChatColor.GRAY + " Discord deaktiviert/nicht konfiguriert");
        } else {
            sender.sendMessage((discord.isConfigured("events") ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                    + ChatColor.GRAY + " Discord Events Channel");
            sender.sendMessage((discord.isConfigured("audit") ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                    + ChatColor.GRAY + " Discord Audit Channel");
            sender.sendMessage((discord.isConfigured("status") ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                    + ChatColor.GRAY + " Discord Status Channel");
        }
        sender.sendMessage(ChatColor.GRAY + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        sender.sendMessage(ChatColor.DARK_GRAY + "Runtime-Gate: danach Island-Starter, Crate, Voucher, PlayerShop, Pack, Casino, Giveaway und Multiplayer-Flows manuell pruefen.");
        return true;
    }

    private void eventReturnRecovery(CommandSender sender) {
        boolean installed = EventReturnRecoveryService.isInstalled();
        check(sender, installed, "Event Return Recovery");
        if (!installed) return;
        int pending = EventReturnRecoveryService.pendingCount();
        if (pending > 0) sender.sendMessage(ChatColor.YELLOW + "[RECOVERY]" + ChatColor.GRAY + " Event-Rueckkehrpositionen warten: " + ChatColor.WHITE + pending);
        else sender.sendMessage(ChatColor.GREEN + "[OK]" + ChatColor.GRAY + " Event Return Queue leer");
    }

    private void casinoSettlementRecovery(CommandSender sender) {
        CasinoSettlementJournal journal = CasinoSettlementJournal.active();
        if (journal == null) { check(sender, false, "Casino Settlement Journal"); return; }
        int review = journal.reviewRequiredCount();
        if (review > 0) {
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " Casino-Settlements manuell pruefen: "
                    + ChatColor.WHITE + review + ChatColor.GRAY + " Runde(n)"
                    + ChatColor.DARK_GRAY + " | plugins/SkyKings-Admin/" + CasinoSettlementJournal.FILE_NAME);
            return;
        }
        check(sender, true, "Casino Settlement Journal");
    }

    private void rewardSettlementRecovery(CommandSender sender) {
        RewardSettlementJournal journal = RewardSettlementJournal.active();
        if (journal == null) { check(sender, false, "Crate/Voucher Reward Journal"); return; }
        int review = journal.reviewRequiredCount();
        if (review > 0) {
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " Crate/Voucher-Rewards manuell pruefen: "
                    + ChatColor.WHITE + review + ChatColor.GRAY + " Settlement(s)"
                    + ChatColor.DARK_GRAY + " | plugins/SkyKings-Crates/" + RewardSettlementJournal.FILE_NAME);
            return;
        }
        check(sender, true, "Crate/Voucher Reward Journal");
    }

    private void jackpotRecovery(CommandSender sender) {
        Plugin core = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (core == null) { check(sender, false, "Jackpot Recovery Status"); return; }
        File file = new File(core.getDataFolder(), "jackpot.yml");
        if (!file.exists()) { check(sender, true, "Jackpot Recovery Status"); return; }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String status = data.getString("recovery.status", "");
        if ("REVIEW_REQUIRED".equalsIgnoreCase(status)) {
            String winner = data.getString("recovery.winner-name", data.getString("recovery.winner", "unknown"));
            long payout = data.getLong("recovery.payout", 0L);
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " Jackpot Recovery erforderlich"
                    + ChatColor.DARK_GRAY + " | Gewinner " + winner + " | Payout " + payout);
            return;
        }
        check(sender, true, "Jackpot Recovery Status");
    }

    private void tradeEscrowRecovery(CommandSender sender) {
        TradeEscrowJournal journal = TradeEscrowJournal.active();
        if (journal == null) { check(sender, false, "Trade Escrow Journal"); return; }
        int review = journal.reviewRequiredCount();
        int recoverable = journal.recoverableSessionCount();
        if (review > 0) {
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " Trade Escrow manuell pruefen: "
                    + ChatColor.WHITE + review + ChatColor.GRAY + " Session(s)"
                    + ChatColor.DARK_GRAY + " | plugins/SkyKings-Core/" + TradeEscrowJournal.FILE_NAME);
            return;
        }
        if (recoverable > 0) {
            sender.sendMessage(ChatColor.YELLOW + "[RECOVERY]" + ChatColor.GRAY + " Trade Escrow wartet auf Spieler-Join: "
                    + ChatColor.WHITE + recoverable + ChatColor.GRAY + " Session(s)");
            return;
        }
        check(sender, true, "Trade Escrow Journal");
    }

    private void playerShopPurchaseRecovery(CommandSender sender) {
        PlayerShopPurchaseJournal journal = PlayerShopPurchaseJournal.active();
        if (journal == null) { check(sender, false, "PlayerShop Purchase Journal"); return; }
        int review = journal.reviewRequiredCount();
        if (review > 0) {
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " PlayerShop-Kaeufe manuell pruefen: "
                    + ChatColor.WHITE + review + ChatColor.GRAY + " Transaktion(en)"
                    + ChatColor.DARK_GRAY + " | plugins/SkyKings-Core/" + PlayerShopPurchaseJournal.FILE_NAME);
            return;
        }
        check(sender, true, "PlayerShop Purchase Journal");
    }

    private void playerShopRevenueClaimRecovery(CommandSender sender) {
        PlayerShopRevenueClaimJournal journal = PlayerShopRevenueClaimJournal.active();
        if (journal == null) { check(sender, false, "PlayerShop Revenue Claim Journal"); return; }
        int review = journal.reviewRequiredCount();
        if (review > 0) {
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " PlayerShop-Einnahmen-Claims manuell pruefen: "
                    + ChatColor.WHITE + review + ChatColor.GRAY + " Transaktion(en)"
                    + ChatColor.DARK_GRAY + " | plugins/SkyKings-Core/" + PlayerShopRevenueClaimJournal.FILE_NAME);
            return;
        }
        check(sender, true, "PlayerShop Revenue Claim Journal");
    }

    private void playerShopLegacyReview(CommandSender sender) {
        Plugin core = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (core == null) { check(sender, false, "PlayerShop Legacy Migration"); return; }
        File review = new File(core.getDataFolder(), PlayerShopStore.LEGACY_REVIEW_FILE);
        if (review.exists()) {
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " PlayerShop Legacy-Migration blockiert"
                    + ChatColor.DARK_GRAY + " | " + review.getName());
            return;
        }
        check(sender, true, "PlayerShop Legacy Migration");
    }

    private void resourcePack(CommandSender sender) {
        Plugin core = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (!(core instanceof JavaPlugin)) { check(sender, false, "Resource Pack Delivery"); return; }
        JavaPlugin corePlugin = (JavaPlugin) core;
        if (!corePlugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            sender.sendMessage(ChatColor.YELLOW + "[OPTIONAL]" + ChatColor.GRAY + " Resource Pack Delivery deaktiviert");
            return;
        }
        String url = corePlugin.getConfig().getString("resource-pack.url", "");
        String error = ResourcePackService.validationError(url);
        if (error == null) sender.sendMessage(ChatColor.GREEN + "[OK]" + ChatColor.GRAY + " Resource Pack Delivery (HTTPS-URL konfiguriert)");
        else sender.sendMessage(ChatColor.RED + "[FEHLT]" + ChatColor.GRAY + " Resource Pack Delivery: " + ChatColor.WHITE + error);
    }

    private void fileCheck(CommandSender sender, String pluginName, String fileName, String label, boolean optionalBeforeFirstUse) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) { check(sender, false, label); return; }
        File file = new File(plugin.getDataFolder(), fileName);
        if (file.exists()) sender.sendMessage(ChatColor.GREEN + "[OK]" + ChatColor.GRAY + " " + label);
        else if (optionalBeforeFirstUse) sender.sendMessage(ChatColor.YELLOW + "[NEU]" + ChatColor.GRAY + " " + label + " wird beim ersten Einsatz angelegt");
        else check(sender, false, label);
    }

    private void plugin(CommandSender sender, String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        check(sender, plugin != null && plugin.isEnabled(), name);
    }

    private void command(CommandSender sender, String name) {
        PluginCommand command = Bukkit.getPluginCommand(name);
        check(sender, command != null && command.getPlugin().isEnabled(), "/" + name);
    }

    private void check(CommandSender sender, boolean ok, String name) { sender.sendMessage(status(ok) + " " + name); }
    private String status(boolean ok) { return ok ? ChatColor.GREEN + "[OK]" + ChatColor.GRAY : ChatColor.RED + "[FEHLT]" + ChatColor.GRAY; }
}
