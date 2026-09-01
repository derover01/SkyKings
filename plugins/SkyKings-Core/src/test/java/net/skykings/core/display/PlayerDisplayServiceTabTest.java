package net.skykings.core.display;

import org.bukkit.ChatColor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerDisplayServiceTabTest {

    @Test
    public void normalRankKeepsFullPlayerName() {
        String result = PlayerDisplayService.formatTabName(ChatColor.AQUA + "King", "MartinSchmidt");
        assertTrue(result.endsWith(ChatColor.WHITE + "MartinSchmidt"));
        assertFalse(result.contains("[CLAN]"));
    }

    @Test
    public void longRankFallsBackToFullPlayerNameInsteadOfCuttingIt() {
        String playerName = "LongPlayerName16";
        String result = PlayerDisplayService.formatTabName(
                ChatColor.GOLD + "ExtremelyLongPremiumOwnerRank", playerName);

        assertEquals(ChatColor.WHITE + playerName, result);
        assertTrue(result.endsWith(playerName));
        assertTrue(result.length() <= PlayerDisplayService.TAB_NAME_LIMIT);
    }

    @Test
    public void playerNameIsNeverSubstringTruncated() {
        String playerName = "1234567890ABCDEF";
        String result = PlayerDisplayService.formatTabName(ChatColor.DARK_RED + "Owner", playerName);
        assertTrue(result.endsWith(playerName));
    }
}
