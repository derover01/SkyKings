package net.skykings.core.item;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Kleine, null-sichere Kopierhilfen fuer {@link ItemStack}s (siehe Auftrag Phase 1B). */
public final class ItemUtil {

    private ItemUtil() {
    }

    /** Null-sichere Kopie: liefert {@code null} fuer {@code null}, sonst {@link ItemStack#clone()}. */
    public static ItemStack safeCopy(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }

    /** Erzeugt eine neue Liste mit unabhaengigen Kopien aller enthaltenen ItemStacks. */
    public static List<ItemStack> safeCopyAll(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(safeCopy(stack));
        }
        return copies;
    }
}
