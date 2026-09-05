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
    public void legacyAtlasBindingsStayStable() {
        assertEquals(Material.MINECART, ResourcePackIcon.HOME.material());
        assertEquals(Material.POWERED_MINECART, ResourcePackIcon.BACK.material());
        assertEquals(Material.HOPPER_MINECART, ResourcePackIcon.NEXT.material());
        assertEquals(Material.BARRIER, ResourcePackIcon.LOCKED.material());
        assertEquals(Material.SLIME_BALL, ResourcePackIcon.READY.material());
        assertEquals(Material.GHAST_TEAR, ResourcePackIcon.COMPLETED.material());
        assertEquals(Material.PRISMARINE_CRYSTALS, ResourcePackIcon.PREMIUM.material());
        assertEquals(Material.GOLD_NUGGET, ResourcePackIcon.COINS.material());
        assertEquals(Material.NETHER_STAR, ResourcePackIcon.STAR.material());
        assertEquals(Material.EMPTY_MAP, ResourcePackIcon.BATTLE_PASS.material());
        assertEquals(Material.PRISMARINE_SHARD, ResourcePackIcon.QUESTS.material());
        assertEquals(Material.STORAGE_MINECART, ResourcePackIcon.KITS.material());
        assertEquals(Material.COMMAND_MINECART, ResourcePackIcon.CRATES.material());
        assertEquals(Material.DIODE, ResourcePackIcon.JACKPOT.material());
        assertEquals(Material.CARROT_STICK, ResourcePackIcon.SHOP.material());
        assertEquals(Material.FIREWORK_CHARGE, ResourcePackIcon.TRADE.material());
        assertEquals(Material.WRITTEN_BOOK, ResourcePackIcon.CLAN.material());
        assertEquals(Material.SHEARS, ResourcePackIcon.DUEL.material());
        assertEquals(Material.MAGMA_CREAM, ResourcePackIcon.EVENT.material());
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

    @Test
    public void reservedIconsAvoidKnownSharedServerMaterials() {
        Set<Material> shared = new HashSet<Material>(Arrays.asList(
                Material.FIREWORK,
                Material.EYE_OF_ENDER,
                Material.BOOK_AND_QUILL,
                Material.HOPPER,
                Material.NAME_TAG
        ));
        for (ResourcePackIcon icon : ResourcePackIcon.values()) {
            assertFalse("Shared server material must not be reserved by the pack: " + icon.material(), shared.contains(icon.material()));
        }
    }
}
