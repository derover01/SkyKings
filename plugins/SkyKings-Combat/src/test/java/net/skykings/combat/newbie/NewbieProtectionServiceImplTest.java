package net.skykings.combat.newbie;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NewbieProtectionServiceImplTest {

    private static final long TWENTY_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(20);

    private FakePlayerProfileService profileService;
    private NewbieProtectionServiceImpl service;

    @Before
    public void setUp() {
        profileService = new FakePlayerProfileService();
        service = new NewbieProtectionServiceImpl(profileService, TWENTY_MINUTES_MILLIS);
    }

    @Test
    public void freshlyCreatedPlayerIsProtected() {
        UUID uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "Newbie", Rank.SPIELER, 0L, 0L, System.currentTimeMillis(), 0L));

        assertTrue(service.isProtected(uuid));
    }

    @Test
    public void protectionExpiresAutomaticallyAfterTwentyMinutes() {
        UUID uuid = UUID.randomUUID();
        long createdAt = System.currentTimeMillis() - TWENTY_MINUTES_MILLIS - 1000L;
        profileService.put(new PlayerProfile(uuid, "OldEnough", Rank.SPIELER, 0L, 0L, createdAt, 0L));

        assertFalse(service.isProtected(uuid));
    }

    @Test
    public void disablingProtectionIsPermanentAndPersisted() {
        UUID uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "Newbie", Rank.SPIELER, 0L, 0L, System.currentTimeMillis(), 0L));

        service.disableProtection(uuid);

        assertFalse(service.isProtected(uuid));
        assertTrue("newbieProtectionDisabled muss auf dem Profil gesetzt sein",
                profileService.getCached(uuid).isNewbieProtectionDisabled());
        assertTrue("Deaktivierung muss persistiert werden (save() aufgerufen)", profileService.getSaveCallCount() > 0);
    }

    @Test
    public void disablingAlreadyDisabledProtectionDoesNotSaveAgain() {
        UUID uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "Newbie", Rank.SPIELER, 0L, 0L, System.currentTimeMillis(), 0L));

        service.disableProtection(uuid);
        int savesAfterFirstDisable = profileService.getSaveCallCount();
        service.disableProtection(uuid);

        assertTrue(profileService.getSaveCallCount() == savesAfterFirstDisable);
    }

    @Test
    public void unknownOrOfflineUuidIsNotProtected() {
        assertFalse(service.isProtected(UUID.randomUUID()));
    }

    @Test
    public void disablingForUnknownUuidDoesNothing() {
        service.disableProtection(UUID.randomUUID());
        // Keine Exception, kein Save fuer ein nicht existierendes Profil.
        assertTrue(profileService.getSaveCallCount() == 0);
    }
}
