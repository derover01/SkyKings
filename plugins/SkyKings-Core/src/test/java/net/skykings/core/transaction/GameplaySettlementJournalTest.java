package net.skykings.core.transaction;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GameplaySettlementJournalTest {

    @Test
    public void pendingSettlementSurvivesRestartAndBlocksPlayer() throws Exception {
        File dir = Files.createTempDirectory("sk-gameplay-settlement").toFile();
        GameplaySettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin(player, "KIT_GRANT", "diamond", "cooldown-ms=60000");

        assertNotNull(tx);
        assertTrue(journal.hasPendingFor(player));
        assertEquals(1, journal.reviewRequiredCount());

        GameplaySettlementJournal restarted = journal(dir);
        assertTrue(restarted.hasPendingFor(player));
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void completedSettlementDoesNotBlockAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("sk-gameplay-settlement-complete").toFile();
        GameplaySettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin(player, "ENDERCHEST_PAGE_PURCHASE", "3", "price=7500000");
        assertNotNull(tx);
        assertTrue(journal.complete(tx));
        assertFalse(journal.hasPendingFor(player));
        assertEquals(0, journal.reviewRequiredCount());

        GameplaySettlementJournal restarted = journal(dir);
        assertFalse(restarted.hasPendingFor(player));
        assertEquals(0, restarted.reviewRequiredCount());
    }

    private GameplaySettlementJournal journal(File dataFolder) {
        return new GameplaySettlementJournal(dataFolder, Logger.getLogger("GameplaySettlementJournalTest"));
    }
}
