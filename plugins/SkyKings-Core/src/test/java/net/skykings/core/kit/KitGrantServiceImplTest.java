package net.skykings.core.kit;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KitGrantServiceImplTest {

    private KitRegistry registry;
    private PlayerProfileService profileService;
    private CooldownService cooldownService;
    private Player player;
    private PlayerInventory inventory;
    private UUID uuid;

    @Before
    public void setUp() {
        registry = new KitRegistryImpl();
        profileService = mock(PlayerProfileService.class);
        cooldownService = mock(CooldownService.class);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(new ItemStack[36]);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<Integer, ItemStack>());
        when(cooldownService.setNow(any(UUID.class), anyString(), anyLong())).thenReturn(true);
    }

    private KitDefinition kit(String id, Rank rank, long cooldownMillis) {
        return KitDefinition.builder(id)
                .displayName(id)
                .requiredRank(rank)
                .cooldownMillis(cooldownMillis)
                .itemFactory(() -> Collections.singletonList(new ItemStack(Material.IRON_SWORD)))
                .addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1))
                .build();
    }

    private void profile(Rank rank) {
        when(profileService.getCached(uuid)).thenReturn(
                new PlayerProfile(uuid, "Tester", rank, 0L, 0L, 1L, 1L));
    }

    @Test
    public void higherRankCanClaimLowerRankKit() {
        registry.register(kit("spieler", Rank.SPIELER, 60_000L));
        profile(Rank.KING);

        KitGrantServiceImpl service = new KitGrantServiceImpl(registry, profileService, cooldownService);
        KitGrantResult result = service.grant(player, "SPIELER");

        assertEquals(KitGrantResult.Status.SUCCESS, result.getStatus());
        verify(cooldownService).setNow(uuid, "kit:spieler", 60_000L);
        verify(player).addPotionEffect(any(PotionEffect.class), eq(true));
    }

    @Test
    public void lowerRankCannotClaimHigherRankKit() {
        registry.register(kit("king", Rank.KING, 60_000L));
        profile(Rank.SPIELER);

        KitGrantServiceImpl service = new KitGrantServiceImpl(registry, profileService, cooldownService);
        KitGrantResult result = service.grant(player, "king");

        assertEquals(KitGrantResult.Status.NO_PERMISSION, result.getStatus());
        verify(cooldownService, never()).setNow(any(UUID.class), anyString(), anyLong());
    }

    @Test
    public void activeCooldownBlocksGrant() {
        registry.register(kit("gold", Rank.GOLD, 60_000L));
        profile(Rank.GOLD);
        when(cooldownService.getRemainingMillis(uuid, "kit:gold")).thenReturn(12_345L);

        KitGrantServiceImpl service = new KitGrantServiceImpl(registry, profileService, cooldownService);
        KitGrantResult result = service.grant(player, "gold");

        assertEquals(KitGrantResult.Status.COOLDOWN, result.getStatus());
        assertEquals(12_345L, result.getRemainingMillis());
        verify(inventory, never()).addItem(any(ItemStack[].class));
        verify(cooldownService, never()).setNow(any(UUID.class), anyString(), anyLong());
    }

    @Test
    public void fullInventoryDoesNotStartCooldown() {
        registry.register(kit("iron", Rank.IRON, 60_000L));
        profile(Rank.IRON);
        ItemStack[] full = new ItemStack[36];
        for (int i = 0; i < full.length; i++) {
            full[i] = new ItemStack(Material.STONE);
        }
        when(inventory.getContents()).thenReturn(full);

        KitGrantServiceImpl service = new KitGrantServiceImpl(registry, profileService, cooldownService);
        KitGrantResult result = service.grant(player, "iron");

        assertEquals(KitGrantResult.Status.INVENTORY_FULL, result.getStatus());
        verify(cooldownService, never()).setNow(any(UUID.class), anyString(), anyLong());
    }

    @Test
    public void accessibleKitsIncludeOwnAndLowerOnly() {
        registry.register(kit("spieler", Rank.SPIELER, 1L));
        registry.register(kit("gold", Rank.GOLD, 1L));
        registry.register(kit("king", Rank.KING, 1L));
        profile(Rank.GOLD);

        KitGrantServiceImpl service = new KitGrantServiceImpl(registry, profileService, cooldownService);

        assertEquals(2, service.getAccessibleKits(player).size());
        assertTrue(service.getAccessibleKits(player).stream().anyMatch(k -> k.getId().equals("spieler")));
        assertTrue(service.getAccessibleKits(player).stream().anyMatch(k -> k.getId().equals("gold")));
    }
}
