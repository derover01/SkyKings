package net.skykings.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Player-spezifische GUI-Session: eine offene Inventar-Instanz plus Slot-Klick-Handler.
 *
 * <p>Der Konstruktor nimmt ein bereits erzeugtes {@link Inventory} entgegen (Dependency
 * Injection statt eines internen {@code Bukkit.createInventory}-Aufrufs), damit diese Klasse
 * ohne laufenden Server mit einem Test-Double fuer {@code Inventory}/{@code Player} instanziiert
 * werden kann. {@link #create(Player, String, int)} ist die normale Produktions-Abkuerzung.
 *
 * <p>Enthaelt bewusst KEINE konkrete GUI (kein /kit, kein /raenge) - nur das generische Framework.
 */
public final class GuiSession {

    /** Erzeugt eine neue Session inkl. Bukkit-Inventory (Produktions-Verwendung). */
    public static GuiSession create(Player player, String title, int size) {
        Inventory inventory = Bukkit.createInventory(player, size, title);
        return new GuiSession(player, inventory);
    }

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, GuiClickHandler> handlers = new HashMap<>();
    private Runnable onClose;

    public GuiSession(Player player, Inventory inventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public Player getPlayer() {
        return player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public GuiSession setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
        return this;
    }

    public GuiSession setItem(int slot, ItemStack item, GuiClickHandler handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            handlers.put(slot, handler);
        } else {
            handlers.remove(slot);
        }
        return this;
    }

    /** Optionaler Cleanup-Hook, der beim Schliessen der Session aufgerufen wird (siehe {@link GuiManager}). */
    public GuiSession onClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public void open() {
        player.openInventory(inventory);
    }

    void handleClick(Player clicker, InventoryClickEvent event, int slot) {
        GuiClickHandler handler = handlers.get(slot);
        if (handler != null) {
            handler.onClick(clicker, event, slot);
        }
    }

    void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
