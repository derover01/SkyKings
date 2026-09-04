package net.skykings.admin.casino;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CasinoSettlementJournalTest {

    @Test
    public void inProgressSettlementSurvivesRestartAndBlocksPlayer() throws Exception {
        File dir = Files.createTempDirectory("sk-casino-settlement").toFile();
        CasinoSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin(player, "COINS", "COIN_FLIP", 100_000L, 50_000L, 97_000L, 147_000L);

        assertNotNull(tx);
        assertTrue(journal.hasPendingFor(player));
        assertEquals(1, journal.reviewRequiredCount());

        CasinoSettlementJournal restarted = journal(dir);
        assertTrue(restarted.hasPendingFor(player));
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void completedSettlementDoesNotBlockPlayerAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("sk-casino-settlement-complete").toFile();
        CasinoSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin(player, "STARS", "WHEEL", 100L, 10L, 20L, 110L);
        assertNotNull(tx);
        assertTrue(journal.complete(tx));
        assertFalse(journal.hasPendingFor(player));
        assertEquals(0, journal.reviewRequiredCount());

        CasinoSettlementJournal restarted = journal(dir);
        assertFalse(restarted.hasPendingFor(player));
        assertEquals(0, restarted.reviewRequiredCount());
    }

    private CasinoSettlementJournal journal(File dataFolder) {
        return new CasinoSettlementJournal(dataFolder, Logger.getLogger("CasinoSettlementJournalTest"));
    }
}
