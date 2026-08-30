package net.skykings.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testet das generische GUI-Framework (Session-Tracking, Klick-Cancel, Cleanup) mit Mockito-
 * Mocks fuer Player/Inventory/Events - kein laufender Server oder MockBukkit noetig, da
 * {@code InventoryClickEvent}/{@code InventoryCloseEvent} normale (nicht-finale) Klassen der
 * Bukkit-API sind.
 */
public class GuiManagerTest {

    private GuiManager manager;
    private Player player;
    private Inventory inventory;
    private GuiSession session;

    @Before
    public void setUp() {
        manager = new GuiManager();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        inventory = mock(Inventory.class);
        session = new GuiSession(player, inventory);
    }

    @Test
    public void openTracksSessionAndOpensInventory() {
        manager.open(session);

        assertSame(session, manager.getOpenSession(player.getUniqueId()));
        verify(player).openInventory(inventory);
    }

    @Test
    public void clickInTrackedInventoryIsCancelledAndDispatchedToHandler() {
        GuiClickHandler handler = mock(GuiClickHandler.class);
        session.setItem(2, mock(org.bukkit.inventory.ItemStack.class), handler);
        manager.open(session);

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClickedInventory()).thenReturn(inventory);
        when(event.getRawSlot()).thenReturn(2);

        manager.onClick(event);

        verify(event).setCancelled(true);
        verify(handler).onClick(player, event, 2);
    }

    @Test
    public void clickInPlayersOwnInventoryIsCancelledButNotDispatched() {
        GuiClickHandler handler = mock(GuiClickHandler.class);
        session.setItem(2, mock(org.bukkit.inventory.ItemStack.class), handler);
        manager.open(session);

        Inventory bottomInventory = mock(Inventory.class);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClickedInventory()).thenReturn(bottomInventory);

        manager.onClick(event);

        verify(event).setCancelled(true);
        verify(handler, never()).onClick(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void clickIsIgnoredWhenPlayerHasNoOpenSession() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);

        manager.onClick(event);

        verify(event, never()).setCancelled(org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    public void closingTrackedInventoryRemovesSessionAndRunsCleanup() {
        Runnable cleanup = mock(Runnable.class);
        session.onClose(cleanup);
        manager.open(session);

        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);

        manager.onClose(event);

        assertNull(manager.getOpenSession(player.getUniqueId()));
        verify(cleanup).run();
    }

    @Test
    public void closingUnrelatedInventoryDoesNotAffectTrackedSession() {
        manager.open(session);

        Inventory otherInventory = mock(Inventory.class);
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getInventory()).thenReturn(otherInventory);

        manager.onClose(event);

        assertSame(session, manager.getOpenSession(player.getUniqueId()));
    }

    @Test
    public void quitRemovesSessionEvenIfCloseNeverFired() {
        manager.open(session);

        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        manager.onQuit(event);

        assertNull("Quit muss die Session als Memory-Leak-Schutz entfernen", manager.getOpenSession(player.getUniqueId()));
    }
}
