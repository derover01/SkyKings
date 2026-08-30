package net.skykings.core.cooldown;

import net.skykings.core.logging.AuditEvent;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.storage.DataStore;
import net.skykings.core.storage.sqlite.SQLiteDataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownServiceImplTest {

    private File databaseFile;
    private SQLiteDataStore realDataStore;
    private CountingDataStore countingDataStore;
    private CooldownServiceImpl cooldownService;

    @Before
    public void setUp() throws IOException {
        databaseFile = File.createTempFile("skykings-core-cooldown-test", ".db");
        databaseFile.delete();
        realDataStore = new SQLiteDataStore(databaseFile, Logger.getLogger("CooldownServiceImplTest"));
        realDataStore.initialize();
        countingDataStore = new CountingDataStore(realDataStore);
        cooldownService = new CooldownServiceImpl(countingDataStore, new SameThreadExecutorService(), Logger.getLogger("CooldownServiceImplTest"));
    }

    @After
    public void tearDown() {
        realDataStore.close();
        databaseFile.delete();
    }

    @Test
    public void unknownCooldownWithoutLoadIsNotActive() {
        UUID uuid = UUID.randomUUID();
        assertFalse(cooldownService.isActive(uuid, "pearl"));
    }

    @Test
    public void setMakesCooldownActiveAndPersists() {
        UUID uuid = UUID.randomUUID();
        cooldownService.loadForPlayer(uuid);
        cooldownService.set(uuid, "pearl", 5000L);

        assertTrue(cooldownService.isActive(uuid, "pearl"));
        assertTrue(cooldownService.getRemainingMillis(uuid, "pearl") > 0);
        assertTrue(realDataStore.loadCooldown(uuid, "pearl").isPresent());
    }

    @Test
    public void removeClearsCacheAndPersistence() {
        UUID uuid = UUID.randomUUID();
        cooldownService.loadForPlayer(uuid);
        cooldownService.set(uuid, "pearl", 5000L);
        cooldownService.remove(uuid, "pearl");

        assertFalse(cooldownService.isActive(uuid, "pearl"));
        assertFalse(realDataStore.loadCooldown(uuid, "pearl").isPresent());
    }

    @Test
    public void multipleCooldownKeysSurviveSimulatedRestartAfterExplicitLoad() {
        UUID uuid = UUID.randomUUID();
        cooldownService.loadForPlayer(uuid);
        cooldownService.set(uuid, "pearl", 5000L);
        cooldownService.set(uuid, "kit-spieler", 60000L);
        cooldownService.set(uuid, "craterewards-knight", 3600000L);

        // Neuer Service mit frischem Cache, aber demselben DataStore simuliert einen Serverneustart.
        CooldownServiceImpl restarted = new CooldownServiceImpl(
                countingDataStore, new SameThreadExecutorService(), Logger.getLogger("test"));
        restarted.loadForPlayer(uuid);

        assertTrue(restarted.isActive(uuid, "pearl"));
        assertTrue(restarted.isActive(uuid, "kit-spieler"));
        assertTrue(restarted.isActive(uuid, "craterewards-knight"));
    }

    @Test
    public void cooldownIsNotVisibleAfterRestartWithoutExplicitLoad() {
        UUID uuid = UUID.randomUUID();
        cooldownService.loadForPlayer(uuid);
        cooldownService.set(uuid, "pearl", 60000L);

        CooldownServiceImpl restarted = new CooldownServiceImpl(
                countingDataStore, new SameThreadExecutorService(), Logger.getLogger("test"));
        // Ohne loadForPlayer() darf NICHTS aus der DB nachgeladen werden (kein synchroner
        // Read im Gameplay-Pfad) - der Cooldown ist bewusst (noch) nicht sichtbar.
        assertFalse(restarted.isActive(uuid, "pearl"));

        restarted.loadForPlayer(uuid);
        assertTrue(restarted.isActive(uuid, "pearl"));
    }

    @Test
    public void expiredCooldownsAreExcludedFromCacheAndCleanedFromDatabaseOnLoad() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        cooldownService.loadForPlayer(uuid);
        cooldownService.set(uuid, "pearl", 1L);
        Thread.sleep(20L);

        CooldownServiceImpl restarted = new CooldownServiceImpl(
                countingDataStore, new SameThreadExecutorService(), Logger.getLogger("test"));
        restarted.loadForPlayer(uuid);

        assertFalse(restarted.isActive(uuid, "pearl"));
        assertFalse("Abgelaufener Cooldown sollte beim Laden aus der DB bereinigt werden",
                realDataStore.loadCooldown(uuid, "pearl").isPresent());
    }

    @Test
    public void isActiveAndGetRemainingMillisNeverReadFromDataStoreAfterLoad() {
        UUID uuid = UUID.randomUUID();
        cooldownService.loadForPlayer(uuid);
        cooldownService.set(uuid, "pearl", 5000L);

        int readsBefore = countingDataStore.getReadCallCount();
        for (int i = 0; i < 20; i++) {
            cooldownService.isActive(uuid, "pearl");
            cooldownService.getRemainingMillis(uuid, "pearl");
            cooldownService.isActive(uuid, "never-set-key");
        }
        assertEquals("isActive/getRemainingMillis duerfen nach dem Laden keine DataStore-Reads ausloesen",
                readsBefore, countingDataStore.getReadCallCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRejectsNonPositiveDuration() {
        cooldownService.set(UUID.randomUUID(), "pearl", 0L);
    }

    /** Zaehlt Lesezugriffe auf den DataStore, um zu belegen, dass der Gameplay-Pfad ihn nicht mehr aufruft. */
    private static final class CountingDataStore implements DataStore {
        private final DataStore delegate;
        private final AtomicInteger readCallCount = new AtomicInteger();

        CountingDataStore(DataStore delegate) {
            this.delegate = delegate;
        }

        int getReadCallCount() {
            return readCallCount.get();
        }

        @Override
        public void initialize() {
            delegate.initialize();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public Optional<PlayerProfile> loadProfile(UUID uuid) {
            return delegate.loadProfile(uuid);
        }

        @Override
        public void saveProfile(PlayerProfile profile) {
            delegate.saveProfile(profile);
        }

        @Override
        public Optional<Long> loadCooldown(UUID uuid, String key) {
            readCallCount.incrementAndGet();
            return delegate.loadCooldown(uuid, key);
        }

        @Override
        public Map<String, Long> loadCooldowns(UUID uuid) {
            readCallCount.incrementAndGet();
            return delegate.loadCooldowns(uuid);
        }

        @Override
        public void saveCooldown(UUID uuid, String key, long expiresAtMillis) {
            delegate.saveCooldown(uuid, key, expiresAtMillis);
        }

        @Override
        public void deleteCooldown(UUID uuid, String key) {
            delegate.deleteCooldown(uuid, key);
        }

        @Override
        public void appendAuditEvent(AuditEvent event) {
            delegate.appendAuditEvent(event);
        }
    }

    /** Fuehrt Tasks synchron auf dem aufrufenden Thread aus, damit DB-Schreibvorgaenge in Tests deterministisch sind. */
    private static final class SameThreadExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
