package net.skykings.core.economy;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.profile.PlayerProfileService;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EconomyDurablePersistenceTest {

    @Test
    public void persistNowDelegatesToSynchronousProfileSave() {
        PlayerProfileService profiles = mock(PlayerProfileService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        when(profiles.saveNow(uuid)).thenReturn(true);

        EconomyServiceImpl economy = new EconomyServiceImpl(profiles, logging);
        assertTrue(economy.persistNow(uuid));
        verify(profiles).saveNow(uuid);
    }

    @Test
    public void persistNowPropagatesFailedCommit() {
        PlayerProfileService profiles = mock(PlayerProfileService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        when(profiles.saveNow(uuid)).thenReturn(false);

        EconomyServiceImpl economy = new EconomyServiceImpl(profiles, logging);
        assertFalse(economy.persistNow(uuid));
    }
}
