package net.skykings.core.listener;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.profile.FakePlayerProfileService;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerLifecycleListenerTest {

    private InetAddress loopback;

    @Before
    public void setUp() throws UnknownHostException {
        loopback = InetAddress.getLoopbackAddress();
    }

    @Test
    public void preLoginIsAllowedWhenProfileLoadsSuccessfully() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
        CooldownService cooldownService = new NoOpCooldownService();
        PlayerLifecycleListener listener = new PlayerLifecycleListener(profileService, cooldownService, Logger.getLogger("test"));

        UUID uuid = UUID.randomUUID();
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent("Tester", loopback, uuid);

        listener.onAsyncPreLogin(event);

        assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult());
        assertNotNull(profileService.getCached(uuid));
    }

    @Test
    public void preLoginIsDisallowedWithGenericMessageWhenProfileLoadingThrows() {
        PlayerProfileService failingProfileService = new FakePlayerProfileService() {
            @Override
            public PlayerProfile loadOrCreate(UUID uuid, String currentName) {
                throw new RuntimeException("simulierter Datenbankfehler mit sensiblen Details");
            }
        };
        CooldownService cooldownService = new NoOpCooldownService();
        PlayerLifecycleListener listener = new PlayerLifecycleListener(failingProfileService, cooldownService, Logger.getLogger("test"));

        UUID uuid = UUID.randomUUID();
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent("Tester", loopback, uuid);

        listener.onAsyncPreLogin(event);

        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, event.getLoginResult());
        assertEquals(PlayerLifecycleListener.LOAD_FAILURE_MESSAGE, event.getKickMessage());
        // Die technische Fehlermeldung darf NICHT beim Spieler landen.
        org.junit.Assert.assertFalse(event.getKickMessage().contains("Datenbankfehler"));
    }

    // --- Test B: Profil laedt erfolgreich, aber CooldownService.loadForPlayer() wirft - Login muss trotzdem abgelehnt werden ---
    @Test
    public void preLoginIsDisallowedWithGenericMessageWhenCooldownLoadingThrowsEvenIfProfileSucceeded() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
        CooldownService failingCooldownService = new NoOpCooldownService() {
            @Override
            public void loadForPlayer(UUID uuid) {
                throw new RuntimeException("cooldown DB down - enthaelt sensible interne Details");
            }
        };
        PlayerLifecycleListener listener = new PlayerLifecycleListener(profileService, failingCooldownService, Logger.getLogger("test"));

        UUID uuid = UUID.randomUUID();
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent("Tester", loopback, uuid);
        listener.onAsyncPreLogin(event);

        // Das Profil selbst wurde erfolgreich geladen/erstellt (loadOrCreate laeuft vor loadForPlayer).
        assertNotNull(profileService.getCached(uuid));
        // Trotzdem muss der gesamte Login wegen der fehlgeschlagenen Cooldown-Persistenz abgelehnt werden.
        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, event.getLoginResult());
        assertEquals(PlayerLifecycleListener.LOAD_FAILURE_MESSAGE, event.getKickMessage());
        org.junit.Assert.assertFalse(event.getKickMessage().contains("cooldown DB down"));
    }

    @Test
    public void preLoginDoesNotOverrideAnAlreadyDisallowedResult() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
        CooldownService cooldownService = new NoOpCooldownService();
        PlayerLifecycleListener listener = new PlayerLifecycleListener(profileService, cooldownService, Logger.getLogger("test"));

        UUID uuid = UUID.randomUUID();
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent("Tester", loopback, uuid);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "Du bist gebannt.");

        listener.onAsyncPreLogin(event);

        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, event.getLoginResult());
        assertEquals("Du bist gebannt.", event.getKickMessage());
        // Profil wurde bewusst nicht geladen, da der Spieler ohnehin nicht joinen darf.
        assertNull(profileService.getCached(uuid));
    }

    @Test
    public void joinKicksPlayerDefensivelyWhenNoProfileWasLoaded() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
        CooldownService cooldownService = new NoOpCooldownService();
        PlayerLifecycleListener listener = new PlayerLifecycleListener(profileService, cooldownService, Logger.getLogger("test"));

        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Tester");

        PlayerJoinEvent event = new PlayerJoinEvent(player, "Tester joined");
        listener.onJoin(event);

        verify(player).kickPlayer(PlayerLifecycleListener.LOAD_FAILURE_MESSAGE);
    }

    @Test
    public void joinUpdatesPresenceWhenProfileWasLoaded() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
        CooldownService cooldownService = new NoOpCooldownService();
        PlayerLifecycleListener listener = new PlayerLifecycleListener(profileService, cooldownService, Logger.getLogger("test"));

        UUID uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "OldName", Rank.SPIELER, 0L, 0L, 0L, 0L));

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("NewName");

        PlayerJoinEvent event = new PlayerJoinEvent(player, "NewName joined");
        listener.onJoin(event);

        verify(player, org.mockito.Mockito.never()).kickPlayer(org.mockito.ArgumentMatchers.anyString());
        assertEquals("NewName", profileService.getCached(uuid).getLastKnownName());
    }

    /** Minimaler No-Op-Test-Double - vermeidet eine echte DataStore-Abhaengigkeit fuer diese Tests. */
    private static class NoOpCooldownService implements CooldownService {
        @Override
        public void loadForPlayer(UUID uuid) {
        }

        @Override
        public void unloadForPlayer(UUID uuid) {
        }

        @Override
        public boolean isActive(UUID uuid, String key) {
            return false;
        }

        @Override
        public long getRemainingMillis(UUID uuid, String key) {
            return 0L;
        }

        @Override
        public void set(UUID uuid, String key, long durationMillis) {
        }

        @Override
        public void remove(UUID uuid, String key) {
        }
    }
}
