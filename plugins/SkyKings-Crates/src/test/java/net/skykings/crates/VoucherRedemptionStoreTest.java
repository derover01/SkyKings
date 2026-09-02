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

public class VoucherRedemptionStoreTest {
    private final List<VoucherRedemptionStore> stores = new ArrayList<VoucherRedemptionStore>();

    @After
    public void tearDown() {
        for (VoucherRedemptionStore store : stores) store.shutdown();
        stores.clear();
    }

    @Test
    public void sameSerialCanOnlyBeReservedOnceEvenWhenClickedRapidly() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-concurrency").toFile();
        File file = new File(dir, "redeemed-vouchers.txt");
        VoucherRedemptionStore store = create(file);
        store.initialize().get(5, TimeUnit.SECONDS);

        UUID serial = UUID.randomUUID();
        List<CompletableFuture<Boolean>> attempts = new ArrayList<CompletableFuture<Boolean>>();
        for (int i = 0; i < 32; i++) attempts.add(store.redeem(serial));

        int success = 0;
        for (CompletableFuture<Boolean> attempt : attempts) if (attempt.get(5, TimeUnit.SECONDS)) success++;
        assertEquals(1, success);
    }

    @Test
    public void synchronousLiveClaimPersistsBeforeReturning() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-sync").toFile();
        File file = new File(dir, "redeemed-vouchers.txt");
        UUID serial = UUID.randomUUID();

        VoucherRedemptionStore first = create(file);
        first.initialize().get(5, TimeUnit.SECONDS);
        assertTrue(first.redeemSync(serial, 2));
        assertEquals(1, first.getRedeemedClaims(serial));
        assertEquals(1, Files.readAllLines(file.toPath()).size());
        first.shutdown();
        stores.remove(first);

        VoucherRedemptionStore restarted = create(file);
        restarted.initialize().get(5, TimeUnit.SECONDS);
        assertEquals(1, restarted.getRedeemedClaims(serial));
        assertTrue(restarted.redeemSync(serial, 2));
        assertFalse(restarted.redeemSync(serial, 2));
    }

    @Test
    public void synchronousLiveClaimFailsClosedBeforeInitialization() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-sync-ready").toFile();
        VoucherRedemptionStore store = create(new File(dir, "redeemed-vouchers.txt"));
        assertFalse(store.redeemSync(UUID.randomUUID(), 1));
    }

    @Test
    public void stackSerialAllowsExactlyIssuedClaimCount() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-stack").toFile();
        File file = new File(dir, "redeemed-vouchers.txt");
        VoucherRedemptionStore store = create(file);
        store.initialize().get(5, TimeUnit.SECONDS);

        UUID serial = UUID.randomUUID();
        List<CompletableFuture<Boolean>> attempts = new ArrayList<CompletableFuture<Boolean>>();
        for (int i = 0; i < 12; i++) attempts.add(store.redeem(serial, 5));

        int success = 0;
        for (CompletableFuture<Boolean> attempt : attempts) if (attempt.get(5, TimeUnit.SECONDS)) success++;
        assertEquals(5, success);
        assertEquals(5, store.getRedeemedClaims(serial));
        assertFalse(store.redeem(serial, 5).get(5, TimeUnit.SECONDS));
    }

    @Test
    public void redeemedSerialStaysBlockedAfterRestart() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-restart").toFile();
        File file = new File(dir, "redeemed-vouchers.txt");
        UUID serial = UUID.randomUUID();

        VoucherRedemptionStore first = create(file);
        first.initialize().get(5, TimeUnit.SECONDS);
        assertTrue(first.redeem(serial).get(5, TimeUnit.SECONDS));
        first.shutdown();
        stores.remove(first);

        VoucherRedemptionStore restarted = create(file);
        restarted.initialize().get(5, TimeUnit.SECONDS);
        assertFalse(restarted.redeem(serial).get(5, TimeUnit.SECONDS));
    }

    @Test
    public void stackClaimCountSurvivesRestart() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-stack-restart").toFile();
        File file = new File(dir, "redeemed-vouchers.txt");
        UUID serial = UUID.randomUUID();

        VoucherRedemptionStore first = create(file);
        first.initialize().get(5, TimeUnit.SECONDS);
        assertTrue(first.redeem(serial, 3).get(5, TimeUnit.SECONDS));
        assertTrue(first.redeem(serial, 3).get(5, TimeUnit.SECONDS));
        first.shutdown();
        stores.remove(first);

        VoucherRedemptionStore restarted = create(file);
        restarted.initialize().get(5, TimeUnit.SECONDS);
        assertEquals(2, restarted.getRedeemedClaims(serial));
        assertTrue(restarted.redeem(serial, 3).get(5, TimeUnit.SECONDS));
        assertFalse(restarted.redeem(serial, 3).get(5, TimeUnit.SECONDS));
    }

    @Test
    public void storeFailsClosedUntilInitializationCompleted() throws Exception {
        File dir = Files.createTempDirectory("skykings-voucher-ready").toFile();
        File file = new File(dir, "redeemed-vouchers.txt");
        VoucherRedemptionStore store = create(file);

        assertFalse(store.redeem(UUID.randomUUID()).get(5, TimeUnit.SECONDS));
        store.initialize().get(5, TimeUnit.SECONDS);
        assertTrue(store.isReady());
    }

    private VoucherRedemptionStore create(File file) {
        VoucherRedemptionStore store = new VoucherRedemptionStore(file, Logger.getLogger("VoucherRedemptionStoreTest"));
        stores.add(store);
        return store;
    }
}
