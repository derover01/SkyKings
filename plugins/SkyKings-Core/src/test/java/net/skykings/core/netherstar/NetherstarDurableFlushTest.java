package net.skykings.core.netherstar;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.profile.PlayerProfileService;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NetherstarDurableFlushTest {

    @Test
    public void persistNowDelegatesToSynchronousProfileSave() {
        PlayerProfileService profiles = mock(PlayerProfileService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        when(profiles.saveNow(uuid)).thenReturn(true);

        NetherstarServiceImpl service = new NetherstarServiceImpl(profiles, logging);

        assertTrue(service.persistNow(uuid));
        verify(profiles).saveNow(uuid);
    }

    @Test
    public void persistNowPropagatesProfileSaveFailure() {
        PlayerProfileService profiles = mock(PlayerProfileService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        when(profiles.saveNow(uuid)).thenReturn(false);

        NetherstarServiceImpl service = new NetherstarServiceImpl(profiles, logging);

        assertFalse(service.persistNow(uuid));
        verify(profiles).saveNow(uuid);
    }
}
