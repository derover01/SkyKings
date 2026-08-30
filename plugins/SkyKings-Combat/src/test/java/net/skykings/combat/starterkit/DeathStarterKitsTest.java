package net.skykings.combat.starterkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class DeathStarterKitsTest {

    @Test
    public void defaultKitContainsFullIronArmor() {
        DeathStarterKit kit = DeathStarterKits.createDefault(8);

        assertEquals(Material.IRON_HELMET, kit.getHelmet().getType());
        assertEquals(Material.IRON_CHESTPLATE, kit.getChestplate().getType());
        assertEquals(Material.IRON_LEGGINGS, kit.getLeggings().getType());
        assertEquals(Material.IRON_BOOTS, kit.getBoots().getType());
    }

    @Test
    public void defaultKitContainsIronSwordAndConfiguredAmountOfNormalGoldenApples() {
        DeathStarterKit kit = DeathStarterKits.createDefault(8);

        boolean foundSword = false;
        boolean foundApples = false;
        for (ItemStack item : kit.getOtherItems()) {
            if (item.getType() == Material.IRON_SWORD) {
                foundSword = true;
            }
            if (item.getType() == Material.GOLDEN_APPLE) {
                foundApples = true;
                assertEquals("Muss ein NORMALER Golden Apple sein (data=0), kein Enchanted/Notch-Apple",
                        0, item.getDurability());
                assertEquals(8, item.getAmount());
            }
        }
        org.junit.Assert.assertTrue("Kit sollte ein Eisenschwert enthalten", foundSword);
        org.junit.Assert.assertTrue("Kit sollte Golden Apples enthalten", foundApples);
    }

    @Test
    public void goldenAppleAmountIsConfigurable() {
        DeathStarterKit kit = DeathStarterKits.createDefault(3);
        ItemStack apples = findByMaterial(kit, Material.GOLDEN_APPLE);
        assertEquals(3, apples.getAmount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeGoldenAppleAmountIsRejected() {
        DeathStarterKits.createDefault(-1);
    }

    @Test
    public void getOtherItemsReturnsIndependentCopiesPerCall() {
        DeathStarterKit kit = DeathStarterKits.createDefault(8);

        ItemStack firstCallApples = findByMaterial(kit, Material.GOLDEN_APPLE);
        ItemStack secondCallApples = findByMaterial(kit, Material.GOLDEN_APPLE);

        assertNotSame(firstCallApples, secondCallApples);
        firstCallApples.setAmount(1);
        assertEquals("Mutation der ersten Kopie darf die zweite nicht beeinflussen", 8, secondCallApples.getAmount());
    }

    @Test
    public void getHelmetReturnsIndependentCopyEachCall() {
        DeathStarterKit kit = DeathStarterKits.createDefault(8);
        ItemStack first = kit.getHelmet();
        ItemStack second = kit.getHelmet();

        assertNotSame(first, second);
    }

    private ItemStack findByMaterial(DeathStarterKit kit, Material material) {
        for (ItemStack item : kit.getOtherItems()) {
            if (item.getType() == material) {
                return item;
            }
        }
        throw new AssertionError("Kein Item mit Material " + material + " gefunden");
    }
}
