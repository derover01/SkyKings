package net.skykings.core.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Deckt nur die Teile von {@link ItemBuilder} ab, die ohne laufenden Server testbar sind
 * (siehe Auftrag Phase 1B, "Item Utility soweit ohne echten Server testbar"). Methoden, die
 * {@code ItemStack#getItemMeta()} verwenden (name/lore/enchant/flag), rufen intern
 * {@code Bukkit.getServer().getItemFactory()} auf und benoetigen daher einen echten Server
 * oder MockBukkit - das ist ausserhalb des Umfangs dieser Unit-Tests (manuell auf dem
 * Testserver ueberprueft, siehe Abschlussbericht).
 */
public class ItemBuilderTest {

    @Test
    public void constructorSetsMaterialAndDefaultAmount() {
        ItemStack built = new ItemBuilder(Material.DIAMOND_SWORD).build();
        assertEquals(Material.DIAMOND_SWORD, built.getType());
        assertEquals(1, built.getAmount());
    }

    @Test
    public void constructorWithAmountSetsBothFields() {
        ItemStack built = new ItemBuilder(Material.ARROW, 16).build();
        assertEquals(Material.ARROW, built.getType());
        assertEquals(16, built.getAmount());
    }

    @Test
    public void amountCanBeChangedFluently() {
        ItemStack built = new ItemBuilder(Material.STONE).amount(32).build();
        assertEquals(32, built.getAmount());
    }

    @Test
    public void durabilitySetsLegacyDataValue() {
        ItemStack built = new ItemBuilder(Material.WOOL).durability((short) 14).build();
        assertEquals(14, built.getDurability());
    }

    @Test
    public void buildReturnsIndependentCopyEachTime() {
        ItemBuilder builder = new ItemBuilder(Material.GOLD_INGOT, 5);
        ItemStack first = builder.build();
        ItemStack second = builder.build();

        assertNotSame(first, second);
        first.setAmount(64);
        assertEquals(5, second.getAmount());
    }

    @Test
    public void copyConstructorDoesNotMutateOriginalStack() {
        ItemStack original = new ItemStack(Material.APPLE, 3);
        ItemStack built = new ItemBuilder(original).amount(10).build();

        assertEquals(3, original.getAmount());
        assertEquals(10, built.getAmount());
    }

    @Test
    public void safeCopyHandlesNullWithoutThrowing() {
        org.junit.Assert.assertNull(ItemUtil.safeCopy(null));
    }

    @Test
    public void safeCopyReturnsIndependentClone() {
        ItemStack original = new ItemStack(Material.BOW, 1);
        ItemStack copy = ItemUtil.safeCopy(original);

        assertNotSame(original, copy);
        assertEquals(original.getType(), copy.getType());
        copy.setAmount(2);
        assertEquals(1, original.getAmount());
    }
}
