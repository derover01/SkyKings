package net.skykings.core.profile;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.storage.DataStore;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PlayerProfileServiceImplTest {

    @Test
    public void loadExistingLoadsAndCachesPersistedProfileWithoutCreatingAnything() {
        DataStore dataStore = mock(DataStore.class);
        ExecutorService executor = mock(ExecutorService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        PlayerProfile persisted = new PlayerProfile(uuid, "Winner", Rank.SPIELER, 500L, 0L, 1L, 2L);
        when(dataStore.loadProfile(uuid)).thenReturn(Optional.of(persisted));

        PlayerProfileServiceImpl service = new PlayerProfileServiceImpl(dataStore, executor, logging, Logger.getLogger("test"));

        assertSame(persisted, service.loadExisting(uuid));
        assertSame(persisted, service.getCached(uuid));
        verify(dataStore).loadProfile(uuid);
        verify(dataStore, never()).saveProfile(any(PlayerProfile.class));
        verifyNoInteractions(logging);
    }

    @Test
    public void loadExistingReturnsNullForUnknownProfileWithoutCreatingIt() {
        DataStore dataStore = mock(DataStore.class);
        ExecutorService executor = mock(ExecutorService.class);
        LoggingService logging = mock(LoggingService.class);
        UUID uuid = UUID.randomUUID();
        when(dataStore.loadProfile(uuid)).thenReturn(Optional.<PlayerProfile>empty());

        PlayerProfileServiceImpl service = new PlayerProfileServiceImpl(dataStore, executor, logging, Logger.getLogger("test"));

        assertNull(service.loadExisting(uuid));
        assertNull(service.getCached(uuid));
        verify(dataStore).loadProfile(uuid);
        verify(dataStore, never()).saveProfile(any(PlayerProfile.class));
        verifyNoInteractions(logging);
    }
}
