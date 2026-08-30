package net.skykings.core.kit;

import net.skykings.core.model.Rank;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KitRegistryImplTest {

    private KitRegistry registry;

    @Before
    public void setUp() {
        registry = new KitRegistryImpl();
    }

    private KitDefinition sampleKit(String id) {
        return KitDefinition.builder(id)
                .displayName("Test-Kit " + id)
                .requiredRank(Rank.SPIELER)
                .cooldownMillis(60_000L)
                .itemFactory(() -> new ArrayList<>(Arrays.asList(new ItemStack(Material.STONE, 1))))
                .build();
    }

    @Test
    public void registerThenGetReturnsSameDefinition() {
        KitDefinition kit = sampleKit("placeholder-1");
        registry.register(kit);

        Optional<KitDefinition> loaded = registry.get("placeholder-1");
        assertTrue(loaded.isPresent());
        assertEquals("Test-Kit placeholder-1", loaded.get().getDisplayName());
        assertEquals(Rank.SPIELER, loaded.get().getRequiredRank());
        assertEquals(60_000L, loaded.get().getCooldownMillis());
    }

    @Test
    public void getForUnknownIdReturnsEmpty() {
        assertFalse(registry.get("does-not-exist").isPresent());
        assertFalse(registry.contains("does-not-exist"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registeringDuplicateIdThrows() {
        registry.register(sampleKit("duplicate"));
        registry.register(sampleKit("duplicate"));
    }

    @Test
    public void duplicateRegistrationAttemptDoesNotReplaceExisting() {
        registry.register(KitDefinition.builder("duplicate").displayName("Original").build());
        try {
            registry.register(KitDefinition.builder("duplicate").displayName("Ueberschreiber").build());
        } catch (IllegalArgumentException expected) {
            // erwartet
        }
        assertEquals("Original", registry.get("duplicate").get().getDisplayName());
    }

    @Test
    public void getAllReturnsAllRegisteredKits() {
        registry.register(sampleKit("a"));
        registry.register(sampleKit("b"));

        assertEquals(2, registry.getAll().size());
    }

    @Test
    public void unregisterRemovesKit() {
        registry.register(sampleKit("removable"));
        registry.unregister("removable");

        assertFalse(registry.contains("removable"));
    }

    @Test
    public void unregisterUnknownIdIsSafeNoOp() {
        registry.unregister("never-registered");
    }

    @Test
    public void createItemsReturnsIndependentCopiesPerCall() {
        KitDefinition kit = sampleKit("items");
        registry.register(kit);

        List<ItemStack> firstGrant = registry.get("items").get().createItems();
        List<ItemStack> secondGrant = registry.get("items").get().createItems();

        assertEquals(1, firstGrant.size());
        firstGrant.get(0).setAmount(64);
        // Die zweite Vergabe darf von der Mutation der ersten nicht betroffen sein.
        assertEquals(1, secondGrant.get(0).getAmount());
    }
}
