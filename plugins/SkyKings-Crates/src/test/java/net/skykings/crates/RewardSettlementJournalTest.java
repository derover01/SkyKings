package net.skykings.crates;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RewardSettlementJournalTest {

    @Test
    public void pendingRewardSurvivesRestartAndBlocksPlayer() throws Exception {
        File dir = Files.createTempDirectory("sk-reward-settlement").toFile();
        RewardSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin("VOUCHER", UUID.randomUUID().toString(), player, "COINS", "50000");

        assertNotNull(tx);
        assertTrue(journal.hasPendingFor(player));
        assertEquals(1, journal.reviewRequiredCount());

        RewardSettlementJournal restarted = journal(dir);
        assertTrue(restarted.hasPendingFor(player));
        assertEquals(1, restarted.reviewRequiredCount());
    }

    @Test
    public void completedRewardDoesNotBlockAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("sk-reward-settlement-complete").toFile();
        RewardSettlementJournal journal = journal(dir);
        UUID player = UUID.randomUUID();

        UUID tx = journal.begin("CRATE", "STACK:royal", player, "ITEM", "diamond_reward");
        assertNotNull(tx);
        assertTrue(journal.complete(tx));
        assertFalse(journal.hasPendingFor(player));
        assertEquals(0, journal.reviewRequiredCount());

        RewardSettlementJournal restarted = journal(dir);
        assertFalse(restarted.hasPendingFor(player));
        assertEquals(0, restarted.reviewRequiredCount());
    }

    private RewardSettlementJournal journal(File dataFolder) {
        return new RewardSettlementJournal(dataFolder, Logger.getLogger("RewardSettlementJournalTest"));
    }
}
