package net.skykings.combat.loot;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LootPickupListenerTest {

    private LootProtectionService lootProtectionService;
    private LootPickupListener listener;

    @Before
    public void setUp() {
        lootProtectionService = mock(LootProtectionService.class);
        listener = new LootPickupListener(lootProtectionService);
    }

    @Test
    public void pickupIsCancelledWhenNotAllowed() {
        Item item = mock(Item.class);
        Player player = mock(Player.class);
        when(lootProtectionService.canPickup(item, player)).thenReturn(false);

        PlayerPickupItemEvent event = mock(PlayerPickupItemEvent.class);
        when(event.getItem()).thenReturn(item);
        when(event.getPlayer()).thenReturn(player);

        listener.onPickup(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void pickupIsAllowedWhenPermitted() {
        Item item = mock(Item.class);
        Player player = mock(Player.class);
        when(lootProtectionService.canPickup(item, player)).thenReturn(true);

        PlayerPickupItemEvent event = mock(PlayerPickupItemEvent.class);
        when(event.getItem()).thenReturn(item);
        when(event.getPlayer()).thenReturn(player);

        listener.onPickup(event);

        verify(event, org.mockito.Mockito.never()).setCancelled(true);
    }

    @Test
    public void despawnForgetsTheItem() {
        Item item = mock(Item.class);
        ItemDespawnEvent event = mock(ItemDespawnEvent.class);
        when(event.getEntity()).thenReturn(item);

        listener.onDespawn(event);

        verify(lootProtectionService).forget(item);
    }
}
