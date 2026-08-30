package net.skykings.core.integration.luckperms;

import net.skykings.core.model.Rank;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RankGroupMappingTest {

    @Test
    public void everyRankHasExactlyTheDocumentedGroupName() {
        assertEquals("spieler", RankGroupMapping.groupNameFor(Rank.SPIELER));
        assertEquals("iron", RankGroupMapping.groupNameFor(Rank.IRON));
        assertEquals("gold", RankGroupMapping.groupNameFor(Rank.GOLD));
        assertEquals("epic", RankGroupMapping.groupNameFor(Rank.EPIC));
        assertEquals("diamond", RankGroupMapping.groupNameFor(Rank.DIAMOND));
        assertEquals("knight", RankGroupMapping.groupNameFor(Rank.KNIGHT));
        assertEquals("phoenix", RankGroupMapping.groupNameFor(Rank.PHOENIX));
        assertEquals("eternal", RankGroupMapping.groupNameFor(Rank.ETERNAL));
        assertEquals("exile", RankGroupMapping.groupNameFor(Rank.EXILE));
        assertEquals("endling", RankGroupMapping.groupNameFor(Rank.ENDLING));
        assertEquals("king", RankGroupMapping.groupNameFor(Rank.KING));
    }

    @Test
    public void everyRankIsMapped() {
        for (Rank rank : Rank.values()) {
            assertNotNull("Kein Mapping fuer " + rank, RankGroupMapping.groupNameFor(rank));
        }
    }

    @Test
    public void managedGroupNamesContainsExactlyElevenGroups() {
        assertEquals(11, RankGroupMapping.managedGroupNames().size());
    }
}
