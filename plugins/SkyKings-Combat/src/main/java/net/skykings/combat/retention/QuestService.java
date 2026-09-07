package net.skykings.combat.retention;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.event.KingAltarCaptureEvent;
import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.event.CrateOpenedEvent;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Daily/Weekly Quests mit Free-/Premium-Pool und direkter Season-XP-Anbindung. */
public final class QuestService implements Listener {
    private static volatile QuestService ACTIVE;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;
    private final GameplaySettlementJournal settlementJournal;

    public QuestService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.settlementJournal = GameplaySettlementJournal.active();
        if (this.settlementJournal == null) {
            plugin.getLogger().severe("Quest-Rewards starten ohne aktives Gameplay-Settlement-Journal. Rewards werden fail-closed blockiert.");
        }
        ACTIVE = this;
    }

    public static QuestService active() { return ACTIVE; }

    /** Nur volle, validierte SkyKings-Kills duerfen PvP-Quests fortschreiben. */
    @EventHandler
    public void onKill(SkyKingsPlayerKillEvent event) {
        if (event.getAntiFarmMultiplier() < 1.0D) return;
        Player killer = Bukkit.getPlayer(event.getKillerUuid());
        if (killer == null) return;
        prepare(killer.getUniqueId());
        add(killer, "daily.kills", 1);
        add(killer, "weekly.kills", 1);
        max(killer, "daily.streak", event.getNewKillstreak());
        if (isPremium(killer.getUniqueId())) {
            add(killer, "premium.daily.kills", 1);
            add(killer, "premium.weekly.kills", 1);
        }
        checkRewards(killer);
    }

    @EventHandler
    public void onKingAltar(KingAltarCaptureEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "weekly.altar", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.altar", 1);
        checkRewards(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        if (EventParticipationService.global().isInEvent(player.getUniqueId())) return;
        prepare(player.getUniqueId());
        add(player, "daily.pearls", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.daily.pearls", 1);
        checkRewards(player);
    }

    @EventHandler
    public void onCrateOpened(CrateOpenedEvent event) {
        if (event.getPlayer() == null) return;
        Player player = event.getPlayer();
        prepare(player.getUniqueId());
        add(player, "weekly.crates", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.crates", 1);
        checkRewards(player);
    }

    /**
     * QuestService ist vor DuelService registriert und liest deshalb den noch aktiven Duel-Sessionzustand.
     * So zaehlen sowohl normale Duel-Tode als auch Forfeit/Disconnect ohne Kopplung an private DuelService-Daten.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDuelDeath(PlayerDeathEvent event) {
        recordDuelOpponentWin(event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDuelQuit(PlayerQuitEvent event) {
        recordDuelOpponentWin(event.getPlayer());
    }

    private void recordDuelOpponentWin(Player loser) {
        if (loser == null) return;
        EventParticipationService runtime = EventParticipationService.global();
        EventParticipationService.Participation loserState = runtime.get(loser.getUniqueId());
        if (loserState == null || loserState.getType() != EventParticipationService.Type.DUEL) return;

        for (Map.Entry<UUID, EventParticipationService.Participation> entry : runtime.snapshot().entrySet()) {
            if (entry.getKey().equals(loser.getUniqueId())) continue;
            EventParticipationService.Participation other = entry.getValue();
            if (other == null || other.getType() != EventParticipationService.Type.DUEL) continue;
            if (!loserState.getSessionId().equals(other.getSessionId())) continue;
            Player winner = Bukkit.getPlayer(entry.getKey());
            if (winner != null && winner.isOnline()) recordDuelWin(winner);
            return;
        }
    }

    /** Wird nur fuer einen tatsaechlichen Duel-Sieg aufgerufen. */
    public void recordDuelWin(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "daily.duels", 1);
        add(player, "weekly.duels", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.duels", 1);
        checkRewards(player);
    }

    /** Pro Kill maximal einmal aufrufen, auch wenn Streak- und Player-Bounty gleichzeitig ausbezahlt werden. */
    public void recordBountyClaim(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "weekly.bounties", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.bounties", 1);
        checkRewards(player);
    }

    /** Nur wenn eine Rare/Epic Map-Loot-Chest tatsaechlich frisch geoeffnet/refilled wurde. */
    public void recordRareMapChest(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "weekly.rare-chests", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.rare-chests", 1);
        checkRewards(player);
    }

    public boolean isPremium(UUID uuid) {
        BattlePassService service = BattlePassService.active();
        return service != null && service.isPremium(uuid);
    }

    public synchronized void prepare(UUID uuid) {
        long day = dayId();
        int week = weekId();
        String p = "players." + uuid + ".";
        boolean changed = false;
        if (data.getLong(p + "daily.id", -1L) != day) {
            data.set(p + "daily", null);
            data.set(p + "daily.id", day);
            data.set(p + "premium.daily", null);
            changed = true;
        }
        if (data.getInt(p + "weekly.id", -1) != week) {
            data.set(p + "weekly", null);
            data.set(p + "weekly.id", week);
            data.set(p + "premium.weekly", null);
            changed = true;
        }
        if (changed && !saveNow()) {
            plugin.getLogger().severe("Quest-Rotation konnte nicht durable gespeichert werden; Fortschritt bleibt fail-closed auf dem In-Memory-Stand.");
        }
    }

    public synchronized int get(UUID uuid, String key) { prepare(uuid); return data.getInt("players." + uuid + "." + key, 0); }
    public synchronized boolean claimed(UUID uuid, String key) { prepare(uuid); return data.getBoolean("players." + uuid + "." + key, false); }

    private synchronized void add(Player player, String key, int amount) {
        if (amount <= 0) return;
        String p = "players." + player.getUniqueId() + "." + key;
        int before = data.getInt(p, 0);
        long candidate = (long) before + amount;
        int after = candidate > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) candidate;
        data.set(p, after);
        if (!saveNow()) {
            data.set(p, before);
            plugin.getLogger().warning("Quest-Fortschritt konnte nicht durable gespeichert werden: " + key + " fuer " + player.getUniqueId());
        }
    }

    private synchronized void max(Player player, String key, int value) {
        String p = "players." + player.getUniqueId() + "." + key;
        int before = data.getInt(p, 0);
        if (value > before) {
            data.set(p, value);
            if (!saveNow()) {
                data.set(p, before);
                plugin.getLogger().warning("Quest-Maximum konnte nicht durable gespeichert werden: " + key + " fuer " + player.getUniqueId());
            }
        }
    }

    private void checkRewards(Player player) {
        UUID uuid = player.getUniqueId();
        if (get(uuid, "daily.kills") >= 5 && !claimed(uuid, "daily.claimed-kills"))
            reward(player, "daily.claimed-kills", 150_000L, 2, 500, "Daily: 5 PvP-Kills");
        if (get(uuid, "daily.pearls") >= 20 && !claimed(uuid, "daily.claimed-pearls"))
            reward(player, "daily.claimed-pearls", 75_000L, 1, 350, "Daily: 20 Enderperlen");
        if (get(uuid, "daily.streak") >= 5 && !claimed(uuid, "daily.claimed-streak"))
            reward(player, "daily.claimed-streak", 200_000L, 3, 750, "Daily: 5er Killstreak");
        if (get(uuid, "daily.duels") >= 1 && !claimed(uuid, "daily.claimed-duels"))
            reward(player, "daily.claimed-duels", 125_000L, 2, 600, "Daily: 1 Duel-Sieg");

        if (get(uuid, "weekly.kills") >= 30 && !claimed(uuid, "weekly.claimed-kills"))
            reward(player, "weekly.claimed-kills", 500_000L, 5, 2_000, "Weekly: 30 PvP-Kills");
        if (get(uuid, "weekly.altar") >= 3 && !claimed(uuid, "weekly.claimed-altar"))
            reward(player, "weekly.claimed-altar", 350_000L, 4, 1_500, "Weekly: 3 King-Altar Captures");
        if (get(uuid, "weekly.crates") >= 3 && !claimed(uuid, "weekly.claimed-crates"))
            reward(player, "weekly.claimed-crates", 300_000L, 4, 1_500, "Weekly: 3 Crates oeffnen");
        if (get(uuid, "weekly.bounties") >= 1 && !claimed(uuid, "weekly.claimed-bounties"))
            reward(player, "weekly.claimed-bounties", 350_000L, 5, 1_750, "Weekly: 1 Bounty kassieren");
        if (get(uuid, "weekly.rare-chests") >= 2 && !claimed(uuid, "weekly.claimed-rare-chests"))
            reward(player, "weekly.claimed-rare-chests", 250_000L, 4, 1_500, "Weekly: 2 Rare Map Chests");

        if (!isPremium(uuid)) return;
        if (get(uuid, "premium.daily.kills") >= 10 && !claimed(uuid, "premium.daily.claimed-kills"))
            reward(player, "premium.daily.claimed-kills", 300_000L, 4, 800, "Premium Daily: 10 PvP-Kills");
        if (get(uuid, "premium.daily.pearls") >= 40 && !claimed(uuid, "premium.daily.claimed-pearls"))
            reward(player, "premium.daily.claimed-pearls", 150_000L, 3, 600, "Premium Daily: 40 Enderperlen");
        if (get(uuid, "premium.weekly.kills") >= 75 && !claimed(uuid, "premium.weekly.claimed-kills"))
            reward(player, "premium.weekly.claimed-kills", 1_250_000L, 12, 3_500, "Premium Weekly: 75 PvP-Kills");
        if (get(uuid, "premium.weekly.altar") >= 7 && !claimed(uuid, "premium.weekly.claimed-altar"))
            reward(player, "premium.weekly.claimed-altar", 750_000L, 8, 2_500, "Premium Weekly: 7 King-Altar Captures");
        if (get(uuid, "premium.weekly.duels") >= 3 && !claimed(uuid, "premium.weekly.claimed-duels"))
            reward(player, "premium.weekly.claimed-duels", 600_000L, 7, 2_250, "Premium Weekly: 3 Duel-Siege");
        if (get(uuid, "premium.weekly.bounties") >= 2 && !claimed(uuid, "premium.weekly.claimed-bounties"))
            reward(player, "premium.weekly.claimed-bounties", 700_000L, 8, 2_500, "Premium Weekly: 2 Bounties");
        if (get(uuid, "premium.weekly.crates") >= 5 && !claimed(uuid, "premium.weekly.claimed-crates"))
            reward(player, "premium.weekly.claimed-crates", 500_000L, 6, 2_000, "Premium Weekly: 5 Crates");
        if (get(uuid, "premium.weekly.rare-chests") >= 4 && !claimed(uuid, "premium.weekly.claimed-rare-chests"))
            reward(player, "premium.weekly.claimed-rare-chests", 500_000L, 6, 2_000, "Premium Weekly: 4 Rare Map Chests");
    }

    /**
     * Quest-Claim ist eine Write-Ahead-Transaktion: claimed wird durable reserviert, bevor
     * Coins, physische Sterne oder Season-XP mutiert werden. Nach der ersten Reward-Mutation
     * wird bei jedem unklaren Zustand fail-closed auf Staff-Review gegangen statt neu auszuzahlen.
     */
    private synchronized void reward(Player player, String claimedKey, long coins, int stars, int seasonXp, String name) {
        UUID uuid = player.getUniqueId();
        String claimPath = "players." + uuid + "." + claimedKey;
        if (data.getBoolean(claimPath, false)) return;

        if (settlementJournal == null || settlementJournal.hasPendingFor(uuid)) {
            reviewMessage(player);
            return;
        }
        if (!economy.canDeposit(uuid, coins)) {
            player.sendMessage(ChatColor.RED + "Quest-Reward kann aktuell nicht sicher gutgeschrieben werden.");
            return;
        }

        SeasonProgressService progress = SeasonProgressService.active();
        if (seasonXp > 0 && progress == null) {
            player.sendMessage(ChatColor.RED + "Season-Fortschritt ist nicht verfuegbar. Quest-Reward wurde nicht ausbezahlt.");
            return;
        }

        ItemStack starReward = stars > 0 ? SkyKingsCurrencyItems.star(stars) : null;
        if (starReward != null && !canFit(player, starReward)) {
            player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer deinen Quest-Reward.");
            return;
        }

        UUID transaction = settlementJournal.begin(uuid, "QUEST_REWARD", claimedKey,
                name + ", coins=" + coins + ", stars=" + stars + ", seasonXp=" + seasonXp);
        if (transaction == null) {
            player.sendMessage(ChatColor.RED + "Quest-Reward konnte nicht sicher vorbereitet werden.");
            return;
        }

        data.set(claimPath, true);
        if (!saveNow()) {
            data.set(claimPath, false);
            closeUnmutated(transaction, player, "QUEST_CLAIM_NOT_COMMITTED_JOURNAL_CLOSE_FAILED");
            player.sendMessage(ChatColor.RED + "Quest-Claim konnte nicht sicher gespeichert werden. Bitte spaeter erneut versuchen.");
            return;
        }

        try {
            economy.deposit(uuid, coins, "QUEST_REWARD", name);
        } catch (RuntimeException ex) {
            settlementJournal.noteFailure(transaction, "QUEST_COIN_MUTATION_FAILED_AFTER_CLAIM_COMMIT");
            plugin.getLogger().log(Level.SEVERE, "Quest-Coin-Auszahlung hat einen unklaren Zustand erreicht: " + uuid + " / " + claimedKey, ex);
            reviewMessage(player);
            return;
        }
        if (!economy.persistNow(uuid)) {
            settlementJournal.noteFailure(transaction, "QUEST_COIN_DURABLE_COMMIT_FAILED");
            reviewMessage(player);
            return;
        }

        if (starReward != null) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(starReward);
            if (left != null && !left.isEmpty()) {
                settlementJournal.noteFailure(transaction, "QUEST_STAR_DELIVERY_PARTIAL_AFTER_COIN_COMMIT");
                reviewMessage(player);
                return;
            }
            player.updateInventory();
            try {
                player.saveData();
            } catch (RuntimeException ex) {
                settlementJournal.noteFailure(transaction, "QUEST_PLAYERDATA_COMMIT_FAILED_AFTER_STAR_DELIVERY");
                plugin.getLogger().log(Level.SEVERE, "Quest-Sterne konnten nicht durable gespeichert werden: " + uuid + " / " + claimedKey, ex);
                reviewMessage(player);
                return;
            }
        }

        if (seasonXp > 0 && !progress.addXp(player, seasonXp, "Quest: " + name)) {
            settlementJournal.noteFailure(transaction, "QUEST_SEASON_XP_DURABLE_COMMIT_FAILED");
            reviewMessage(player);
            return;
        }

        if (!settlementJournal.complete(transaction)) {
            settlementJournal.noteFailure(transaction, "QUEST_COMMITTED_BUT_JOURNAL_CLOSE_FAILED");
            reviewMessage(player);
            return;
        }

        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "QUEST ABGESCHLOSSEN " + ChatColor.YELLOW + name
                + ChatColor.GRAY + " • +" + coins + " Coins • +" + stars + " Sterne • +" + seasonXp + " Season-XP");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.5F);
    }

    private boolean canFit(Player player, ItemStack reward) {
        int remaining = reward.getAmount();
        int maxStack = Math.max(1, reward.getMaxStackSize());
        for (int slot = 0; slot < 36; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || current.getType() == Material.AIR) return true;
            if (!current.isSimilar(reward)) continue;
            int free = Math.max(0, maxStack - current.getAmount());
            remaining -= free;
            if (remaining <= 0) return true;
        }
        return remaining <= 0;
    }

    private void closeUnmutated(UUID transaction, Player player, String reason) {
        if (settlementJournal.complete(transaction)) return;
        settlementJournal.noteFailure(transaction, reason);
        reviewMessage(player);
    }

    private void reviewMessage(Player player) {
        player.sendMessage(ChatColor.RED + "Ein Quest-Reward hat einen unklaren Speicherzustand. Bitte Staff informieren.");
        player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.7F);
    }

    private long dayId() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR);
    }

    private int weekId() {
        Calendar c = Calendar.getInstance();
        return c.getWeekYear() * 100 + c.get(Calendar.WEEK_OF_YEAR);
    }

    public synchronized void save() { saveNow(); }

    private boolean saveNow() {
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Quest-Datenordner konnte nicht erstellt werden.");
                return false;
            }
            data.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "quests.yml konnte nicht atomar gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }
}
