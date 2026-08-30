package net.skykings.core.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RankTest {

    @Test
    public void freeRanksAreOrderedAscending() {
        assertTrue(Rank.SPIELER.getTier() < Rank.IRON.getTier());
        assertTrue(Rank.IRON.getTier() < Rank.GOLD.getTier());
        assertTrue(Rank.GOLD.getTier() < Rank.EPIC.getTier());
        assertTrue(Rank.EPIC.getTier() < Rank.DIAMOND.getTier());
    }

    @Test
    public void paidRanksAreOrderedAscending() {
        assertTrue(Rank.KNIGHT.getTier() < Rank.PHOENIX.getTier());
        assertTrue(Rank.PHOENIX.getTier() < Rank.ETERNAL.getTier());
        assertTrue(Rank.ETERNAL.getTier() < Rank.EXILE.getTier());
        assertTrue(Rank.EXILE.getTier() < Rank.ENDLING.getTier());
        assertTrue(Rank.ENDLING.getTier() < Rank.KING.getTier());
    }

    @Test
    public void everyPaidRankOutranksEveryFreeRank() {
        assertTrue(Rank.KNIGHT.getTier() > Rank.DIAMOND.getTier());
    }

    @Test
    public void categoriesAreCorrect() {
        for (Rank rank : new Rank[]{Rank.SPIELER, Rank.IRON, Rank.GOLD, Rank.EPIC, Rank.DIAMOND}) {
            assertTrue(rank + " sollte FREE sein", rank.isFree());
            assertFalse(rank + " sollte nicht PAID sein", rank.isPaid());
        }
        for (Rank rank : new Rank[]{Rank.KNIGHT, Rank.PHOENIX, Rank.ETERNAL, Rank.EXILE, Rank.ENDLING, Rank.KING}) {
            assertTrue(rank + " sollte PAID sein", rank.isPaid());
            assertFalse(rank + " sollte nicht FREE sein", rank.isFree());
        }
    }

    @Test
    public void hasAtLeastIsReflexiveAndDirectional() {
        assertTrue(Rank.GOLD.isAtLeast(Rank.GOLD));
        assertTrue(Rank.GOLD.isAtLeast(Rank.IRON));
        assertFalse(Rank.IRON.isAtLeast(Rank.GOLD));
        assertTrue(Rank.KNIGHT.isAtLeast(Rank.DIAMOND));
        assertFalse(Rank.DIAMOND.isAtLeast(Rank.KNIGHT));
    }

    @Test
    public void includedRanksContainSelfAndAllLowerRanks() {
        List<Rank> included = Rank.GOLD.getIncludedRanks();
        assertEquals(3, included.size());
        assertEquals(Rank.SPIELER, included.get(0));
        assertEquals(Rank.IRON, included.get(1));
        assertEquals(Rank.GOLD, included.get(2));
    }

    @Test
    public void kingIncludesEveryRank() {
        assertEquals(Rank.values().length, Rank.KING.getIncludedRanks().size());
    }
}
