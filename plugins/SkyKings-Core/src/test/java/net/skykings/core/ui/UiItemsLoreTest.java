package net.skykings.core.ui;

import org.bukkit.ChatColor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiItemsLoreTest {

    @Test
    public void wrapsLongLoreIntoReadableLines() {
        List<String> lines = UiItems.wrapLore(ChatColor.GRAY
                + "Diese Beschreibung ist absichtlich deutlich laenger als eine normale Tooltip-Zeile auf dem Server");

        assertTrue(lines.size() >= 3);
        for (String line : lines) {
            String visible = ChatColor.stripColor(line);
            assertTrue("Lore line too wide: " + visible, visible.length() <= 32);
        }
    }

    @Test
    public void carriesColorAcrossWrappedLines() {
        List<String> lines = UiItems.wrapLore(ChatColor.AQUA
                + "Diese farbige Beschreibung wird auf mehrere kurze Zeilen verteilt ohne den aktiven Farbcode zu verlieren");

        assertTrue(lines.size() > 1);
        assertTrue(lines.get(1).startsWith(ChatColor.AQUA.toString()));
    }

    @Test
    public void keepsIntentionalBlankLines() {
        List<String> lines = UiItems.wrapLore(ChatColor.GRAY + "Kurz", "", ChatColor.YELLOW + "Aktion");
        assertEquals(3, lines.size());
        assertEquals("", lines.get(1));
    }
}
