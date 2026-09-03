package net.skykings.core.profile;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.storage.DataStore;
import net.skykings.core.storage.DataStoreException;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerProfileDurableSaveTest {

    @Test
    public void saveNowWritesCachedProfileImmediately() {
        DataStore store = mock(DataStore.class);
        ExecutorService executor = mock(ExecutorService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = new PlayerProfile(uuid, "Durable", Rank.SPIELER, 123L, 0L, 1L, 2L);
        when(store.loadProfile(uuid)).thenReturn(Optional.of(profile));

        PlayerProfileServiceImpl service = new PlayerProfileServiceImpl(store, executor, logging, Logger.getLogger("test"));
        service.loadExisting(uuid);

        assertTrue(service.saveNow(uuid));
        verify(store).saveProfile(profile);
    }

    @Test
    public void saveNowFailsClosedWhenDataStoreWriteThrows() {
        DataStore store = mock(DataStore.class);
        ExecutorService executor = mock(ExecutorService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = new PlayerProfile(uuid, "Durable", Rank.SPIELER, 123L, 0L, 1L, 2L);
        when(store.loadProfile(uuid)).thenReturn(Optional.of(profile));
        doThrow(new DataStoreException("disk down")).when(store).saveProfile(profile);

        PlayerProfileServiceImpl service = new PlayerProfileServiceImpl(store, executor, logging, Logger.getLogger("test"));
        service.loadExisting(uuid);

        assertFalse(service.saveNow(uuid));
    }
}
