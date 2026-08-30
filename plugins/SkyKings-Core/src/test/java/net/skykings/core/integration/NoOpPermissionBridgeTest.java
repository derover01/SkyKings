package net.skykings.core.integration;

import net.skykings.core.model.Rank;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;

/** Verifiziert den Fallback-Zustand, wenn kein LuckPerms installiert ist (siehe Auftrag Phase 1B). */
public class NoOpPermissionBridgeTest {

    @Test
    public void isNeverAvailable() {
        assertFalse(new NoOpPermissionBridge().isAvailable());
    }

    @Test
    public void syncRankNeverThrowsForAnyRank() {
        NoOpPermissionBridge bridge = new NoOpPermissionBridge();
        UUID uuid = UUID.randomUUID();
        for (Rank rank : Rank.values()) {
            bridge.syncRank(uuid, rank);
        }
    }
}
