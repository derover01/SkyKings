package net.skykings.core.shop.player;

import org.bukkit.Material;
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

public class PlayerShopPurchaseJournalTest {

    @Test
    public void inProgressPurchaseSurvivesRestartAsReview() throws Exception {
        File dir = Files.createTempDirectory("sk-playershop-purchase").toFile();
        JavaPlugin plugin = plugin(dir);
        PlayerShopPurchaseJournal journal = new PlayerShopPurchaseJournal(plugin);

        UUID transaction = journal.begin(UUID.randomUUID(), 3, UUID.randomUUID(), UUID.randomUUID(),
                Material.DIAMOND, (short) 0, 64, 32, 1_000L, 950L, 100L);

        assertNotNull(transaction);
        assertEquals(1, journal.reviewRequiredCount());

        PlayerShopPurchaseJournal restarted = new PlayerShopPurchaseJournal(plugin);
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void completedPurchaseDoesNotRequireReviewAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("sk-playershop-complete").toFile();
        JavaPlugin plugin = plugin(dir);
        PlayerShopPurchaseJournal journal = new PlayerShopPurchaseJournal(plugin);

        UUID transaction = journal.begin(UUID.randomUUID(), 0, UUID.randomUUID(), UUID.randomUUID(),
                Material.GOLDEN_APPLE, (short) 0, 64, 64, 2_000_000L, 1_900_000L, 0L);
        assertNotNull(transaction);
        assertTrue(journal.complete(transaction));
        assertEquals(0, journal.reviewRequiredCount());

        PlayerShopPurchaseJournal restarted = new PlayerShopPurchaseJournal(plugin);
        assertEquals(0, restarted.reviewRequiredCount());
    }

    private JavaPlugin plugin(File dataFolder) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PlayerShopPurchaseJournalTest"));
        return plugin;
    }
}
