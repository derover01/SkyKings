package net.skykings.core.ui;

import org.bukkit.Material;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourcePackIconTest {

    @Test
    public void coreAtlasHasExactlyNineteenUniqueReservedMaterials() {
        ResourcePackIcon[] icons = ResourcePackIcon.values();
        assertEquals(19, icons.length);

        Set<Material> materials = new HashSet<Material>();
        for (ResourcePackIcon icon : icons) {
            assertNotNull(icon.material());
            assertTrue("Duplicate resource-pack material: " + icon.material(), materials.add(icon.material()));
        }
    }

    @Test
    public void coinAndStarMaterialsStayDistinct() {
        assertEquals(Material.GOLD_NUGGET, ResourcePackIcon.COINS.material());
        assertEquals(Material.NETHER_STAR, ResourcePackIcon.STAR.material());
        assertFalse(ResourcePackIcon.COINS.material() == ResourcePackIcon.STAR.material());
    }

    @Test
    public void reservedIconsNeverReplaceProtectedPvpItems() {
        Set<Material> protectedPvp = new HashSet<Material>(Arrays.asList(
                Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLD_SWORD, Material.DIAMOND_SWORD,
                Material.BOW, Material.FISHING_ROD, Material.ENDER_PEARL, Material.GOLDEN_APPLE,
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_LEGGINGS, Material.GOLD_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
        ));

        for (ResourcePackIcon icon : ResourcePackIcon.values()) {
            assertFalse("Protected PvP material must not be overwritten: " + icon.material(), protectedPvp.contains(icon.material()));
        }
    }
}
