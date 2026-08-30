package net.skykings.core.cooldown;

import net.skykings.core.storage.sqlite.SQLiteDataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownServiceImplTest {

    private File databaseFile;
    private SQLiteDataStore dataStore;
    private CooldownServiceImpl cooldownService;

    @Before
    public void setUp() throws IOException {
        databaseFile = File.createTempFile("skykings-core-cooldown-test", ".db");
        databaseFile.delete();
        dataStore = new SQLiteDataStore(databaseFile, Logger.getLogger("CooldownServiceImplTest"));
        dataStore.initialize();
        cooldownService = new CooldownServiceImpl(dataStore, new SameThreadExecutorService(), Logger.getLogger("CooldownServiceImplTest"));
    }

    @After
    public void tearDown() {
        dataStore.close();
        databaseFile.delete();
    }

    @Test
    public void newCooldownIsNotActive() {
        UUID uuid = UUID.randomUUID();
        assertFalse(cooldownService.isActive(uuid, "pearl"));
    }

    @Test
    public void setMakesCooldownActiveAndPersists() {
        UUID uuid = UUID.randomUUID();
        cooldownService.set(uuid, "pearl", 5000L);

        assertTrue(cooldownService.isActive(uuid, "pearl"));
        assertTrue(cooldownService.getRemainingMillis(uuid, "pearl") > 0);
        assertTrue(dataStore.loadCooldown(uuid, "pearl").isPresent());
    }

    @Test
    public void removeClearsCacheAndPersistence() {
        UUID uuid = UUID.randomUUID();
        cooldownService.set(uuid, "pearl", 5000L);
        cooldownService.remove(uuid, "pearl");

        assertFalse(cooldownService.isActive(uuid, "pearl"));
        assertFalse(dataStore.loadCooldown(uuid, "pearl").isPresent());
    }

    @Test
    public void cooldownSurvivesSimulatedRestart() {
        UUID uuid = UUID.randomUUID();
        cooldownService.set(uuid, "pearl", 60000L);

        // Neuer Service mit frischem Cache, aber demselben DataStore simuliert einen Serverneustart.
        CooldownServiceImpl reloaded = new CooldownServiceImpl(
                dataStore, new SameThreadExecutorService(), Logger.getLogger("CooldownServiceImplTest"));
        assertTrue(reloaded.isActive(uuid, "pearl"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRejectsNonPositiveDuration() {
        cooldownService.set(UUID.randomUUID(), "pearl", 0L);
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
