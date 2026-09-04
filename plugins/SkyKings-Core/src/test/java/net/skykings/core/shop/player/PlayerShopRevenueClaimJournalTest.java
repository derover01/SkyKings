package net.skykings.core.shop.player;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerShopRevenueClaimJournalTest {

    @Test
    public void inProgressClaimSurvivesRestartAsReview() throws Exception {
        File dir = Files.createTempDirectory("sk-playershop-revenue").toFile();
        JavaPlugin plugin = plugin(dir);
        PlayerShopRevenueClaimJournal journal = new PlayerShopRevenueClaimJournal(plugin);

        UUID transaction = journal.begin(UUID.randomUUID(), UUID.randomUUID(), 12_345L);

        assertNotNull(transaction);
        assertEquals(1, journal.reviewRequiredCount());

        PlayerShopRevenueClaimJournal restarted = new PlayerShopRevenueClaimJournal(plugin);
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void completedClaimDoesNotRequireReviewAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("sk-playershop-revenue-complete").toFile();
        JavaPlugin plugin = plugin(dir);
        PlayerShopRevenueClaimJournal journal = new PlayerShopRevenueClaimJournal(plugin);

        UUID transaction = journal.begin(UUID.randomUUID(), UUID.randomUUID(), 25_000L);
        assertNotNull(transaction);
        assertTrue(journal.complete(transaction));
        assertEquals(0, journal.reviewRequiredCount());

        PlayerShopRevenueClaimJournal restarted = new PlayerShopRevenueClaimJournal(plugin);
        assertEquals(0, restarted.reviewRequiredCount());
    }

    private JavaPlugin plugin(File dataFolder) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PlayerShopRevenueClaimJournalTest"));
        return plugin;
    }
}
