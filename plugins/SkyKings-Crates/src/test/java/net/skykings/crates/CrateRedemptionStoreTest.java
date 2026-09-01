package net.skykings.crates;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrateRedemptionStoreTest {
    private final List<CrateRedemptionStore> stores = new ArrayList<CrateRedemptionStore>();

    @After
    public void tearDown() {
        for (CrateRedemptionStore store : stores) store.shutdown();
        stores.clear();
    }

    @Test
    public void rapidClaimsNeverExceedBatchLimit() throws Exception {
        File dir = Files.createTempDirectory("skykings-crate-concurrency").toFile();
        CrateRedemptionStore store = create(new File(dir, "redeemed-crates.txt"));
        store.initialize().get(5, TimeUnit.SECONDS);

        UUID serial = UUID.randomUUID();
        int maxClaims = 5;
        List<CompletableFuture<Boolean>> attempts = new ArrayList<CompletableFuture<Boolean>>();
        for (int i = 0; i < 40; i++) attempts.add(store.redeem(serial, maxClaims));

        int success = 0;
        for (CompletableFuture<Boolean> attempt : attempts) {
            if (attempt.get(5, TimeUnit.SECONDS)) success++;
        }
        assertEquals(maxClaims, success);
    }

    @Test
    public void consumedBatchRemainsConsumedAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("skykings-crate-restart").toFile();
        File file = new File(dir, "redeemed-crates.txt");
        UUID serial = UUID.randomUUID();

        CrateRedemptionStore first = create(file);
        first.initialize().get(5, TimeUnit.SECONDS);
        assertTrue(first.redeem(serial, 2).get(5, TimeUnit.SECONDS));
        assertTrue(first.redeem(serial, 2).get(5, TimeUnit.SECONDS));
        assertFalse(first.redeem(serial, 2).get(5, TimeUnit.SECONDS));
        first.shutdown();
        stores.remove(first);

        CrateRedemptionStore restarted = create(file);
        restarted.initialize().get(5, TimeUnit.SECONDS);
        assertFalse(restarted.redeem(serial, 2).get(5, TimeUnit.SECONDS));
    }

    @Test
    public void storeFailsClosedBeforeInitialization() throws Exception {
        File dir = Files.createTempDirectory("skykings-crate-ready").toFile();
        CrateRedemptionStore store = create(new File(dir, "redeemed-crates.txt"));
        assertFalse(store.redeem(UUID.randomUUID(), 1).get(5, TimeUnit.SECONDS));
        store.initialize().get(5, TimeUnit.SECONDS);
        assertTrue(store.isReady());
    }

    private CrateRedemptionStore create(File file) {
        CrateRedemptionStore store = new CrateRedemptionStore(file, Logger.getLogger("CrateRedemptionStoreTest"));
        stores.add(store);
        return store;
    }
}
