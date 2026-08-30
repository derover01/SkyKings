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
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LootProtectionServiceImplTest {

    private LootProtectionServiceImpl service;
    private Location deathLocation;
    private UUID killerUuid;

    @Before
    public void setUp() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        // Fuehrt den "naechsten Tick"-Task synchron sofort aus, damit der Test deterministisch ist.
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        });

        World world = mock(World.class);
        deathLocation = mock(Location.class);
        when(deathLocation.getWorld()).thenReturn(world);
        killerUuid = UUID.randomUUID();

        service = new LootProtectionServiceImpl(plugin, 150L);
    }

    private Item mockDroppedItem(World world, Location location) {
        Item item = mock(Item.class);
        when(item.getUniqueId()).thenReturn(UUID.randomUUID());
        when(world.getNearbyEntities(location, 2.0, 2.0, 2.0)).thenReturn((java.util.Collection) Arrays.asList(item));
        return item;
    }

    @Test
    public void killerCanPickUpProtectedDrop() {
        Item item = mockDroppedItem(deathLocation.getWorld(), deathLocation);
        service.protectDeathDrops(deathLocation, killerUuid);

        Player killer = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(killerUuid);

        assertTrue(service.canPickup(item, killer));
    }

    @Test
    public void strangerCannotPickUpBeforeExpiry() {
        Item item = mockDroppedItem(deathLocation.getWorld(), deathLocation);
        service.protectDeathDrops(deathLocation, killerUuid);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());

        assertFalse(service.canPickup(item, stranger));
    }

    @Test
    public void strangerCanPickUpAfterExpiry() throws InterruptedException {
        Item item = mockDroppedItem(deathLocation.getWorld(), deathLocation);
        service.protectDeathDrops(deathLocation, killerUuid);
        Thread.sleep(200L);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(service.canPickup(item, stranger));
    }

    @Test
    public void unprotectedItemCanAlwaysBePickedUp() {
        Item neverProtected = mock(Item.class);
        when(neverProtected.getUniqueId()).thenReturn(UUID.randomUUID());
        Player anyone = mock(Player.class);
        when(anyone.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(service.canPickup(neverProtected, anyone));
    }

    @Test
    public void onlyItemEntitiesAreProtectedNotOtherNearbyEntities() {
        World world = deathLocation.getWorld();
        Item item = mock(Item.class);
        when(item.getUniqueId()).thenReturn(UUID.randomUUID());
        Entity zombie = mock(Zombie.class);
        when(world.getNearbyEntities(deathLocation, 2.0, 2.0, 2.0))
                .thenReturn((java.util.Collection) Arrays.asList(item, zombie));

        service.protectDeathDrops(deathLocation, killerUuid);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertFalse("Das echte Drop-Item muss geschuetzt sein", service.canPickup(item, stranger));
        // Der Zombie ist kein Item und wird schlicht nie ueber canPickup(Item,...) angefragt -
        // dieser Test dokumentiert nur, dass das Filtern in protectDeathDrops nicht crasht.
    }

    @Test
    public void forgetRemovesTrackingSoItemBecomesFreelyPickupable() {
        Item item = mockDroppedItem(deathLocation.getWorld(), deathLocation);
        service.protectDeathDrops(deathLocation, killerUuid);

        service.forget(item);

        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertTrue(service.canPickup(item, stranger));
    }
}
