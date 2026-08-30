package net.skykings.combat.starterkit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeathStarterKitServiceTest {

    private Player player;
    private PlayerInventory inventory;
    private DeathStarterKit kit;

    @Before
    public void setUp() {
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        kit = DeathStarterKits.createDefault(8);
    }

    @Test
    public void applyClearsInventoryAndEquipsArmorDirectly() {
        DeathStarterKitService service = new DeathStarterKitService(kit, true);

        service.apply(player);

        verify(inventory).clear();
        verify(inventory).setHelmet(argThatType(Material.IRON_HELMET));
        verify(inventory).setChestplate(argThatType(Material.IRON_CHESTPLATE));
        verify(inventory).setLeggings(argThatType(Material.IRON_LEGGINGS));
        verify(inventory).setBoots(argThatType(Material.IRON_BOOTS));
    }

    @Test
    public void applyAddsSwordAndGoldenApplesToInventory() {
        DeathStarterKitService service = new DeathStarterKitService(kit, true);

        service.apply(player);

        verify(inventory, times(2)).addItem(any(ItemStack.class));
    }

    @Test
    public void applyDoesNothingWhenDisabled() {
        DeathStarterKitService service = new DeathStarterKitService(kit, false);

        service.apply(player);

        verify(inventory, never()).clear();
        verify(inventory, never()).setHelmet(any());
        verify(inventory, never()).addItem(any(ItemStack.class));
    }

    private ItemStack argThatType(Material material) {
        return org.mockito.ArgumentMatchers.argThat(item -> item != null && item.getType() == material);
    }
}
