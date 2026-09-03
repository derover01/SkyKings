package net.skykings.core.trade;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TradeEscrowJournalTest {

    @Test
    public void activeEscrowSurvivesRestartAndRecoversExactlyOnce() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockBukkitItemFactory()) {
            File dir = Files.createTempDirectory("sk-trade-active").toFile();
            File file = new File(dir, TradeEscrowJournal.FILE_NAME);
            TradeEscrowJournal journal = new TradeEscrowJournal(file, Logger.getLogger("test"));

            UUID left = UUID.randomUUID();
            UUID right = UUID.randomUUID();
            TradeSession session = new TradeSession(left, right);
            session.getLeft().setItems(Arrays.asList(new ItemStack(Material.DIAMOND, 3)));
            session.getLeft().setCoins(250_000L);

            assertTrue(journal.saveActive(session));
            assertEquals(1, journal.recoverableSessionCount());
            assertEquals(0, journal.reviewRequiredCount());

            TradeEscrowJournal restarted = new TradeEscrowJournal(file, Logger.getLogger("test"));
            assertEquals(1, restarted.recoverableSessionCount());
            assertEquals(0, restarted.reviewRequiredCount());

            List<TradeEscrowJournal.RecoveryEntry> entries = restarted.recoveriesFor(left);
            assertEquals(1, entries.size());
            assertEquals(1, entries.get(0).items.size());
            assertEquals(Material.DIAMOND, entries.get(0).items.get(0).getType());
            assertEquals(3, entries.get(0).items.get(0).getAmount());

            assertTrue(restarted.beginRecovery(session.getId(), left));
            assertEquals(TradeEscrowJournal.State.RECOVERING, restarted.stateOf(session.getId()));
            assertTrue(restarted.completeRecovery(session.getId(), left));
            assertEquals(0, restarted.recoverableSessionCount());
            assertTrue(restarted.recoveriesFor(left).isEmpty());
        }
    }

    @Test
    public void preparedInboundNeverAutoRecoversAfterCrash() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockBukkitItemFactory()) {
            File dir = Files.createTempDirectory("sk-trade-prepared").toFile();
            File file = new File(dir, TradeEscrowJournal.FILE_NAME);
            TradeEscrowJournal journal = new TradeEscrowJournal(file, Logger.getLogger("test"));

            UUID left = UUID.randomUUID();
            TradeSession session = new TradeSession(left, UUID.randomUUID());
            assertTrue(journal.prepareInbound(session, left, 4, new ItemStack(Material.EMERALD, 12)));
            assertEquals(TradeEscrowJournal.State.PREPARED, journal.stateOf(session.getId()));

            TradeEscrowJournal restarted = new TradeEscrowJournal(file, Logger.getLogger("test"));
            assertEquals(1, restarted.reviewRequiredCount());
            assertEquals(0, restarted.recoverableSessionCount());
            assertTrue(restarted.recoveriesFor(left).isEmpty());
            assertEquals(TradeEscrowJournal.State.REVIEW_REQUIRED, restarted.stateOf(session.getId()));
        }
    }

    @Test
    public void returningOrSettlingStateBecomesManualReview() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockBukkitItemFactory()) {
            File dir = Files.createTempDirectory("sk-trade-transient").toFile();
            File file = new File(dir, TradeEscrowJournal.FILE_NAME);
            TradeEscrowJournal journal = new TradeEscrowJournal(file, Logger.getLogger("test"));

            UUID left = UUID.randomUUID();
            TradeSession session = new TradeSession(left, UUID.randomUUID());
            session.getLeft().setItems(Arrays.asList(new ItemStack(Material.GOLD_INGOT, 5)));
            assertTrue(journal.saveActive(session));
            assertTrue(journal.markReturning(session, "TEST_RETURN"));

            TradeEscrowJournal afterReturnCrash = new TradeEscrowJournal(file, Logger.getLogger("test"));
            assertEquals(1, afterReturnCrash.reviewRequiredCount());
            assertEquals(TradeEscrowJournal.State.REVIEW_REQUIRED, afterReturnCrash.stateOf(session.getId()));

            TradeSession settling = new TradeSession(UUID.randomUUID(), UUID.randomUUID());
            settling.getRight().setItems(Arrays.asList(new ItemStack(Material.IRON_INGOT, 7)));
            assertTrue(afterReturnCrash.saveActive(settling));
            assertTrue(afterReturnCrash.markSettling(settling));

            TradeEscrowJournal afterSettlementCrash = new TradeEscrowJournal(file, Logger.getLogger("test"));
            assertEquals(2, afterSettlementCrash.reviewRequiredCount());
            assertEquals(TradeEscrowJournal.State.REVIEW_REQUIRED, afterSettlementCrash.stateOf(settling.getId()));
            assertFalse(afterSettlementCrash.recoverableSessionCount() > 0);
        }
    }

    private MockedStatic<Bukkit> mockBukkitItemFactory() {
        ItemFactory itemFactory = Mockito.mock(ItemFactory.class);
        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);
        return bukkit;
    }
}
