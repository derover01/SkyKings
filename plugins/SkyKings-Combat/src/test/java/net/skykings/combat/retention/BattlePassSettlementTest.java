package net.skykings.combat.retention;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BattlePassSettlementTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void completedFreeRewardPersistsClaimAndDoesNotPayTwiceAfterReload() throws Exception {
        Fixture fixture = fixture(true);

        fixture.service.claim(fixture.player, false, 1);

        assertTrue(loadClaim(fixture.combatFolder, fixture.uuid, 1, false, 1));
        verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(2_000L), eq("BATTLE_PASS"), anyString());
        verify(fixture.economy, times(1)).persistNow(fixture.uuid);

        BattlePassService reloaded = new BattlePassService(fixture.combatPlugin, fixture.progress, fixture.economy);
        reloaded.claim(fixture.player, false, 1);

        verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(2_000L), eq("BATTLE_PASS"), anyString());
        verify(fixture.economy, times(1)).persistNow(fixture.uuid);
    }

    @Test
    public void failedCoinCommitKeepsClaimReservedAndSettlementPending() throws Exception {
        Fixture fixture = fixture(false);

        fixture.service.claim(fixture.player, false, 1);

        assertTrue(loadClaim(fixture.combatFolder, fixture.uuid, 1, false, 1));
        assertTrue(fixture.journal.hasPendingFor(fixture.uuid));
        verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(2_000L), eq("BATTLE_PASS"), anyString());
        verify(fixture.economy, times(1)).persistNow(fixture.uuid);

        BattlePassService reloaded = new BattlePassService(fixture.combatPlugin, fixture.progress, fixture.economy);
        reloaded.claim(fixture.player, false, 1);

        verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(2_000L), eq("BATTLE_PASS"), anyString());
        verify(fixture.economy, times(1)).persistNow(fixture.uuid);
    }

    @Test
    public void legacyClaimBelongsToMigrationSeasonButDoesNotBlockNextSeason() throws Exception {
        File coreFolder = temporaryFolder.newFolder("core-legacy-" + UUID.randomUUID());
        File combatFolder = temporaryFolder.newFolder("combat-legacy-" + UUID.randomUUID());
        Logger logger = Logger.getLogger("BattlePassSettlementTestLegacy");

        JavaPlugin corePlugin = mock(JavaPlugin.class);
        when(corePlugin.getDataFolder()).thenReturn(coreFolder);
        when(corePlugin.getLogger()).thenReturn(logger);
        new GameplaySettlementJournal(corePlugin);

        JavaPlugin combatPlugin = mock(JavaPlugin.class);
        when(combatPlugin.getDataFolder()).thenReturn(combatFolder);
        when(combatPlugin.getLogger()).thenReturn(logger);

        EconomyService economy = mock(EconomyService.class);
        when(economy.canDeposit(any(UUID.class), anyLong())).thenReturn(true);
        when(economy.persistNow(any(UUID.class))).thenReturn(true);

        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        File battlePassFile = new File(combatFolder, "battlepass.yml");
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("players." + uuid + ".claimed.free.1", true);
        legacy.save(battlePassFile);

        SeasonProgressService progress = new SeasonProgressService(combatPlugin);
        BattlePassService service = new BattlePassService(combatPlugin, progress, economy);

        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(battlePassFile);
        assertEquals(1, migrated.getInt("migration.legacy-claims-season"));

        service.claim(player, false, 1);
        verify(economy, never()).deposit(eq(uuid), eq(2_000L), eq("BATTLE_PASS"), anyString());

        assertEquals(2, progress.advanceSeasonAndResetXp());
        service.claim(player, false, 1);

        verify(economy, times(1)).deposit(eq(uuid), eq(2_000L), eq("BATTLE_PASS"), anyString());
        verify(economy, times(1)).persistNow(uuid);
        assertTrue(loadClaim(combatFolder, uuid, 2, false, 1));
    }

    private Fixture fixture(boolean persistCoins) throws Exception {
        File coreFolder = temporaryFolder.newFolder("core-" + UUID.randomUUID());
        File combatFolder = temporaryFolder.newFolder("combat-" + UUID.randomUUID());
        Logger logger = Logger.getLogger("BattlePassSettlementTest");

        JavaPlugin corePlugin = mock(JavaPlugin.class);
        when(corePlugin.getDataFolder()).thenReturn(coreFolder);
        when(corePlugin.getLogger()).thenReturn(logger);
        GameplaySettlementJournal journal = new GameplaySettlementJournal(corePlugin);

        JavaPlugin combatPlugin = mock(JavaPlugin.class);
        when(combatPlugin.getDataFolder()).thenReturn(combatFolder);
        when(combatPlugin.getLogger()).thenReturn(logger);

        EconomyService economy = mock(EconomyService.class);
        when(economy.canDeposit(any(UUID.class), anyLong())).thenReturn(true);
        when(economy.persistNow(any(UUID.class))).thenReturn(persistCoins);

        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        SeasonProgressService progress = new SeasonProgressService(combatPlugin);
        BattlePassService service = new BattlePassService(combatPlugin, progress, economy);
        return new Fixture(uuid, player, economy, combatPlugin, combatFolder, journal, progress, service);
    }

    private boolean loadClaim(File folder, UUID uuid, int season, boolean premium, int level) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new File(folder, "battlepass.yml"));
        return data.getBoolean("players." + uuid + ".seasons." + season + ".claimed."
                + (premium ? "premium" : "free") + "." + level, false);
    }

    private static final class Fixture {
        private final UUID uuid;
        private final Player player;
        private final EconomyService economy;
        private final JavaPlugin combatPlugin;
        private final File combatFolder;
        private final GameplaySettlementJournal journal;
        private final SeasonProgressService progress;
        private final BattlePassService service;

        private Fixture(UUID uuid, Player player, EconomyService economy, JavaPlugin combatPlugin,
                        File combatFolder, GameplaySettlementJournal journal,
                        SeasonProgressService progress, BattlePassService service) {
            this.uuid = uuid;
            this.player = player;
            this.economy = economy;
            this.combatPlugin = combatPlugin;
            this.combatFolder = combatFolder;
            this.journal = journal;
            this.progress = progress;
            this.service = service;
        }
    }
}
