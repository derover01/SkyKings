package net.skykings.core.shop;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShopSettlementJournalTest {

    @Test
    public void pendingPurchaseSurvivesRestartAndBlocksPlayer() throws Exception {
        File dir = Files.createTempDirectory("sk-shop-settlement").toFile();
        ShopSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin(player, "system", "blocks-diamond", "COINS",
                25_000L, "DIAMOND", 16);

        assertNotNull(tx);
        assertTrue(journal.hasPendingFor(player));
        assertEquals(1, journal.reviewRequiredCount());

        ShopSettlementJournal restarted = journal(dir);
        assertTrue(restarted.hasPendingFor(player));
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void pendingSaleSurvivesRestartAndBlocksPlayer() throws Exception {
        File dir = Files.createTempDirectory("sk-shop-sale-settlement").toFile();
        ShopSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.beginSale(player, "SELL_ALL", 125_000L, 64);

        assertNotNull(tx);
        assertTrue(journal.hasPendingFor(player));
        assertEquals(1, journal.reviewRequiredCount());

        ShopSettlementJournal restarted = journal(dir);
        assertTrue(restarted.hasPendingFor(player));
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void completedPurchaseDoesNotBlockAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("sk-shop-settlement-complete").toFile();
        ShopSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin(player, "system", "obsidian", "NETHERSTARS",
                5L, "OBSIDIAN", 32);
        assertNotNull(tx);
        assertTrue(journal.complete(tx));
        assertFalse(journal.hasPendingFor(player));
        assertEquals(0, journal.reviewRequiredCount());

        ShopSettlementJournal restarted = journal(dir);
        assertFalse(restarted.hasPendingFor(player));
        assertEquals(0, restarted.reviewRequiredCount());
    }

    private ShopSettlementJournal journal(File dataFolder) {
        return new ShopSettlementJournal(dataFolder, Logger.getLogger("ShopSettlementJournalTest"));
    }
}
