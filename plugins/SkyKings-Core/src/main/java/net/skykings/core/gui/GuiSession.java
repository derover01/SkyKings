package net.skykings.core.gui;

import net.skykings.core.ui.UiTheme;
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
 * <p>Enthaelt bewusst KEINE konkrete GUI - nur das generische Framework.
 */
public final class GuiSession {

    /** Erzeugt eine neue Session inkl. Bukkit-Inventory (Produktions-Verwendung). */
    public static GuiSession create(Player player, String title, int size) {
        Inventory inventory = Bukkit.createInventory(player, size, title);
        return new GuiSession(player, inventory);
    }

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, GuiClickHandler> handlers = new HashMap<Integer, GuiClickHandler>();
    private Runnable onClose;

    public GuiSession(Player player, Inventory inventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public Player getPlayer() { return player; }
    public Inventory getInventory() { return inventory; }

    public GuiSession setItem(int slot, ItemStack item) {
        inventory.setItem(resolveSlot(slot), item);
        return this;
    }

    public GuiSession setItem(int slot, ItemStack item, GuiClickHandler handler) {
        int resolved = resolveSlot(slot);
        inventory.setItem(resolved, item);
        if (handler != null) handlers.put(resolved, handler);
        else handlers.remove(resolved);
        return this;
    }

    /**
     * UiTheme-Navigation ist fuer 54 Slots definiert. Kleinere Menues bekommen dieselben relativen
     * Positionen automatisch in ihre letzte Reihe gemappt. Dadurch kann ein NAV_BACK nie wieder
     * ein 27er/45er Inventar mit ArrayIndexOutOfBounds crashen.
     */
    private int resolveSlot(int requested) {
        int size = inventory.getSize();
        if (requested >= 0 && requested < size) return requested;
        if (requested == UiTheme.NAV_BACK) return size - 9;
        if (requested == UiTheme.NAV_HOME) return size - 5;
        if (requested == UiTheme.NAV_NEXT) return size - 1;
        throw new IllegalArgumentException("GUI-Slot " + requested + " liegt ausserhalb von " + size + " Slots");
    }

    /** Optionaler Cleanup-Hook, der beim Schliessen der Session aufgerufen wird. */
    public GuiSession onClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public void open() { player.openInventory(inventory); }

    void handleClick(Player clicker, InventoryClickEvent event, int slot) {
        GuiClickHandler handler = handlers.get(slot);
        if (handler != null) handler.onClick(clicker, event, slot);
    }

    void handleClose() {
        if (onClose != null) onClose.run();
    }
}
