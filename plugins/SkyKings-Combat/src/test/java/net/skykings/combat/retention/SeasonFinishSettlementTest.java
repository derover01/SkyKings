package net.skykings.combat.retention;

import net.skykings.combat.stats.PvpStatsService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SeasonFinishSettlementTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void successfulFinishArchivesBeforeDurableXpReset() throws Exception {
        File folder = temporaryFolder.newFolder("season-success");
        JavaPlugin plugin = plugin(folder);
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        SeasonProgressService progress = new SeasonProgressService(plugin);
        assertTrue(progress.addXp(player, 100, "test"));

        LegacyHallService hall = mock(LegacyHallService.class);
        when(hall.archive(anyInt(), anyList())).thenReturn(true);
        PvpStatsService stats = mock(PvpStatsService.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            SeasonMedalService medals = new SeasonMedalService(plugin, progress, stats, hall);
            assertTrue(medals.finishSeason());
        }

        assertEquals(2, progress.getSeason());
        assertEquals(0, progress.getXp(uuid));
        verify(hall, times(1)).archive(anyInt(), anyList());

        YamlConfiguration stored = YamlConfiguration.loadConfiguration(new File(folder, "season-medals.yml"));
        assertTrue(stored.getLong("finished-seasons.1.completed-at", 0L) > 0L);
        assertFalse(stored.contains("finish-state"));
    }

    @Test
    public void legacyArchiveFailureLeavesSeasonAndXpUntouchedAndResumable() throws Exception {
        File folder = temporaryFolder.newFolder("season-archive-fail");
        JavaPlugin plugin = plugin(folder);
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        SeasonProgressService progress = new SeasonProgressService(plugin);
        assertTrue(progress.addXp(player, 100, "test"));

        LegacyHallService hall = mock(LegacyHallService.class);
        when(hall.archive(anyInt(), anyList())).thenReturn(false).thenReturn(true);
        PvpStatsService stats = mock(PvpStatsService.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            SeasonMedalService medals = new SeasonMedalService(plugin, progress, stats, hall);
            assertFalse(medals.finishSeason());
            assertEquals(1, progress.getSeason());
            assertEquals(100, progress.getXp(uuid));

            YamlConfiguration pending = YamlConfiguration.loadConfiguration(new File(folder, "season-medals.yml"));
            assertEquals("PREPARED", pending.getString("finish-state.phase"));

            assertTrue(medals.finishSeason());
        }

        assertEquals(2, progress.getSeason());
        assertEquals(0, progress.getXp(uuid));
        verify(hall, times(2)).archive(anyInt(), anyList());
    }

    @Test
    public void resetFailureStaysPendingAndNextStartupFinalizesOnlyOldSeason() throws Exception {
        File folder = temporaryFolder.newFolder("season-reset-fail");
        JavaPlugin plugin = plugin(folder);
        UUID uuid = UUID.randomUUID();
        Map<UUID, Integer> ranking = new LinkedHashMap<UUID, Integer>();
        ranking.put(uuid, 500);

        SeasonProgressService failingProgress = mock(SeasonProgressService.class);
        when(failingProgress.getSeason()).thenReturn(1);
        when(failingProgress.getAllXp()).thenReturn(Collections.unmodifiableMap(ranking));
        when(failingProgress.advanceSeasonAndResetXp()).thenReturn(-1);

        LegacyHallService hall = mock(LegacyHallService.class);
        when(hall.archive(anyInt(), anyList())).thenReturn(true);
        PvpStatsService stats = mock(PvpStatsService.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            SeasonMedalService medals = new SeasonMedalService(plugin, failingProgress, stats, hall);
            assertFalse(medals.finishSeason());
        }

        YamlConfiguration pending = YamlConfiguration.loadConfiguration(new File(folder, "season-medals.yml"));
        assertEquals(1, pending.getInt("finish-state.season"));
        assertEquals("ARCHIVE_COMMITTED", pending.getString("finish-state.phase"));
        assertFalse(pending.contains("finished-seasons.1.completed-at"));

        SeasonProgressService alreadyResetProgress = mock(SeasonProgressService.class);
        when(alreadyResetProgress.getSeason()).thenReturn(2);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            new SeasonMedalService(plugin, alreadyResetProgress, stats, hall);
        }

        YamlConfiguration recovered = YamlConfiguration.loadConfiguration(new File(folder, "season-medals.yml"));
        assertTrue(recovered.getLong("finished-seasons.1.completed-at", 0L) > 0L);
        assertFalse(recovered.contains("finish-state"));
        verify(alreadyResetProgress, times(0)).advanceSeasonAndResetXp();
    }

    private JavaPlugin plugin(File folder) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("SeasonFinishSettlementTest"));
        return plugin;
    }
}
