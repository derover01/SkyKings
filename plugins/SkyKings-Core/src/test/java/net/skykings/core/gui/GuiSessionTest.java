package net.skykings.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GuiSessionTest {

    private Player player;
    private Inventory inventory;
    private GuiSession session;

    @Before
    public void setUp() {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        inventory = mock(Inventory.class);
        session = new GuiSession(player, inventory);
    }

    @Test
    public void getPlayerAndInventoryReturnConstructorValues() {
        assertSame(player, session.getPlayer());
        assertSame(inventory, session.getInventory());
    }

    @Test
    public void setItemForwardsToInventory() {
        ItemStack stack = mock(ItemStack.class);
        session.setItem(3, stack);
        verify(inventory).setItem(3, stack);
    }

    @Test
    public void openCallsOpenInventoryOnPlayer() {
        session.open();
        verify(player).openInventory(inventory);
    }

    @Test
    public void handleClickInvokesRegisteredHandlerForThatSlot() {
        GuiClickHandler handler = mock(GuiClickHandler.class);
        session.setItem(5, mock(ItemStack.class), handler);

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        session.handleClick(player, event, 5);

        verify(handler).onClick(player, event, 5);
    }

    @Test
    public void handleClickDoesNothingForSlotWithoutHandler() {
        GuiClickHandler handler = mock(GuiClickHandler.class);
        session.setItem(5, mock(ItemStack.class), handler);

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        session.handleClick(player, event, 99);

        verify(handler, never()).onClick(any(Player.class), any(InventoryClickEvent.class), anyInt());
    }

    @Test
    public void settingNullHandlerRemovesPreviousHandler() {
        GuiClickHandler handler = mock(GuiClickHandler.class);
        session.setItem(5, mock(ItemStack.class), handler);
        session.setItem(5, mock(ItemStack.class), null);

        session.handleClick(player, mock(InventoryClickEvent.class), 5);

        verify(handler, never()).onClick(any(Player.class), any(InventoryClickEvent.class), anyInt());
    }

    @Test
    public void handleCloseInvokesOnCloseCallbackExactlyOnce() {
        Runnable cleanup = mock(Runnable.class);
        session.onClose(cleanup);

        session.handleClose();

        verify(cleanup, times(1)).run();
    }

    @Test
    public void handleCloseWithoutCallbackDoesNotThrow() {
        session.handleClose();
    }
}
