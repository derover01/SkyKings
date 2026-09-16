package net.skykings.combat.collection;

import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.combat.stats.PvpStatsService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class HeadCollectionDurabilityTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void firstUniqueUnlockIsDurableBeforeHandlerReturns() throws Exception {
        File folder = temporaryFolder.newFolder("collection-success");
        JavaPlugin plugin = plugin(folder);
        KillContextService context = mock(KillContextService.class);
        PvpStatsService stats = mock(PvpStatsService.class);
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        when(context.consume(victim)).thenReturn("Nahkampf");

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        OfflinePlayer target = mock(OfflinePlayer.class);
        when(target.getName()).thenReturn("Victim");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(killer)).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer(victim)).thenReturn(target);

            HeadCollectionService service = new HeadCollectionService(plugin, context, stats);
            service.onKill(kill(killer, victim));
            assertTrue(service.hasCollected(killer, victim));

            HeadCollectionService reloaded = new HeadCollectionService(plugin, context, stats);
            assertTrue(reloaded.hasCollected(killer, victim));
        }
    }

    @Test
    public void failedUniqueUnlockSaveRollsBackInMemoryProgress() throws Exception {
        File invalidFolder = temporaryFolder.newFile("collection-not-a-directory");
        JavaPlugin plugin = plugin(invalidFolder);
        KillContextService context = mock(KillContextService.class);
        PvpStatsService stats = mock(PvpStatsService.class);
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        when(context.consume(victim)).thenReturn("PvP");

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(killer)).thenReturn(null);

            HeadCollectionService service = new HeadCollectionService(plugin, context, stats);
            service.onKill(kill(killer, victim));
            assertFalse(service.hasCollected(killer, victim));
            assertTrue(service.collectedCount(killer) == 0);
        }
    }

    private SkyKingsPlayerKillEvent kill(UUID killer, UUID victim) {
        return new SkyKingsPlayerKillEvent(killer, victim, 100L, 1.0D, 0L, 100L, 1);
    }

    private JavaPlugin plugin(File folder) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("HeadCollectionDurabilityTest"));
        return plugin;
    }
}
