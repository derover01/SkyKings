package net.skykings.combat.loot;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LootProtectionServiceImplTest {

    private LootProtectionServiceImpl service;
    private Location deathLocation;
    private World world;
    private UUID killerUuid;

    @Before
    public void setUp() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        });

        world = mock(World.class);
        deathLocation = mock(Location.class);
        when(deathLocation.getWorld()).thenReturn(world);
        killerUuid = UUID.randomUUID();

        service = new LootProtectionServiceImpl(plugin, 150L);
    }

    private Item item() {
        Item item = mock(Item.class);
        when(item.getUniqueId()).thenReturn(UUID.randomUUID());
        return item;
    }

    private void nearbyBeforeAndAfter(java.util.Collection<? extends Entity> before,
                                      java.util.Collection<? extends Entity> after) {
        AtomicInteger calls = new AtomicInteger();
        when(world.getNearbyEntities(deathLocation, 2.0, 2.0, 2.0)).thenAnswer(invocation ->
                calls.getAndIncrement() == 0 ? (java.util.Collection) before : (java.util.Collection) after);
    }

    @Test
    public void killerCanPickUpProtectedDrop() {
        Item drop = item();
        nearbyBeforeAndAfter(Collections.emptyList(), Collections.singletonList(drop));
        service.protectDeathDrops(deathLocation, killerUuid);

        Player killer = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(killerUuid);
        assertTrue(service.canPickup(drop, killer));
    }

    @Test
    public void ownerPickupCheckDoesNotReleaseProtectionBeforePickupActuallySucceeds() {
        Item drop = item();
        nearbyBeforeAndAfter(Collections.emptyList(), Collections.singletonList(drop));
        service.protectDeathDrops(deathLocation, killerUuid);

        Player killer = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(killerUuid);
        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(service.canPickup(drop, killer));
        assertFalse("Nur das Pruefen des Besitzer-Pickups darf den Schutz noch nicht entfernen",
                service.canPickup(drop, stranger));

        service.forget(drop);
        assertTrue(service.canPickup(drop, stranger));
    }

    @Test
    public void strangerCannotPickUpBeforeExpiry() {
        Item drop = item();
        nearbyBeforeAndAfter(Collections.emptyList(), Collections.singletonList(drop));
        service.protectDeathDrops(deathLocation, killerUuid);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertFalse(service.canPickup(drop, stranger));
    }

    @Test
    public void strangerCanPickUpAfterExpiry() throws InterruptedException {
        Item drop = item();
        nearbyBeforeAndAfter(Collections.emptyList(), Collections.singletonList(drop));
        service.protectDeathDrops(deathLocation, killerUuid);
        Thread.sleep(200L);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertTrue(service.canPickup(drop, stranger));
    }

    @Test
    public void preExistingGroundItemIsNeverClaimedAsDeathDrop() {
        Item oldGroundItem = item();
        Item newDeathDrop = item();
        nearbyBeforeAndAfter(Collections.singletonList(oldGroundItem), Arrays.asList(oldGroundItem, newDeathDrop));

        service.protectDeathDrops(deathLocation, killerUuid);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertTrue("Schon vor dem Tod herumliegendes Item muss frei bleiben", service.canPickup(oldGroundItem, stranger));
        assertFalse("Neu entstandener Death-Drop muss fuer den Killer geschuetzt sein", service.canPickup(newDeathDrop, stranger));
    }

    @Test
    public void unprotectedItemCanAlwaysBePickedUp() {
        Item neverProtected = item();
        Player anyone = mock(Player.class);
        when(anyone.getUniqueId()).thenReturn(UUID.randomUUID());
        assertTrue(service.canPickup(neverProtected, anyone));
    }

    @Test
    public void onlyNewItemEntitiesAreProtectedNotOtherNearbyEntities() {
        Item drop = item();
        Entity zombie = mock(Zombie.class);
        nearbyBeforeAndAfter(Collections.emptyList(), Arrays.asList(drop, zombie));

        service.protectDeathDrops(deathLocation, killerUuid);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertFalse("Das echte neue Drop-Item muss geschuetzt sein", service.canPickup(drop, stranger));
    }

    @Test
    public void forgetRemovesTrackingSoItemBecomesFreelyPickupable() {
        Item drop = item();
        nearbyBeforeAndAfter(Collections.emptyList(), Collections.singletonList(drop));
        service.protectDeathDrops(deathLocation, killerUuid);

        service.forget(drop);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertTrue(service.canPickup(drop, stranger));
    }
}
