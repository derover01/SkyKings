package net.skykings.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Wird fuer einen registrierten Slot aufgerufen, wenn ein Spieler ihn anklickt. */
public interface GuiClickHandler {

    void onClick(Player player, InventoryClickEvent event, int slot);
}
