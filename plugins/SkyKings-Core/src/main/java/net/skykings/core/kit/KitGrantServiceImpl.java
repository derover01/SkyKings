package net.skykings.core.kit;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KitGrantServiceImpl implements KitGrantService {

    private static final String COOLDOWN_PREFIX = "kit:";

    private final KitRegistry kitRegistry;
    private final PlayerProfileService profileService;
    private final CooldownService cooldownService;

    public KitGrantServiceImpl(KitRegistry kitRegistry, PlayerProfileService profileService,
                               CooldownService cooldownService) {
        this.kitRegistry = kitRegistry;
        this.profileService = profileService;
        this.cooldownService = cooldownService;
    }

    @Override
    public KitGrantResult grant(Player player, String kitId) {
        if (player == null || kitId == null) {
            return KitGrantResult.of(KitGrantResult.Status.NOT_FOUND, null);
        }

        KitDefinition kit = kitRegistry.get(normalize(kitId)).orElse(null);
        if (kit == null) {
            return KitGrantResult.of(KitGrantResult.Status.NOT_FOUND, null);
        }

        PlayerProfile profile = profileService.getCached(player.getUniqueId());
        if (profile == null) {
            return KitGrantResult.of(KitGrantResult.Status.PROFILE_NOT_LOADED, kit);
        }
        if (!profile.getRank().isAtLeast(kit.getRequiredRank())) {
            return KitGrantResult.of(KitGrantResult.Status.NO_PERMISSION, kit);
        }

        String cooldownKey = COOLDOWN_PREFIX + normalize(kit.getId());
        long remaining = cooldownService.getRemainingMillis(player.getUniqueId(), cooldownKey);
        if (remaining > 0L) {
            return KitGrantResult.cooldown(kit, remaining);
        }

        List<ItemStack> items = kit.createItems();
        if (!hasEnoughEmptySlots(player.getInventory(), items.size())) {
            return KitGrantResult.of(KitGrantResult.Status.INVENTORY_FULL, kit);
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(new ItemStack[items.size()]));
        if (leftovers != null && !leftovers.isEmpty()) {
            // Sollte wegen der Vorabpruefung nicht auftreten. Fail-safe: nichts auf den Boden werfen,
            // damit eine Kit-Vergabe nie unkontrolliert Items erzeugt.
            for (ItemStack leftover : leftovers.values()) {
                player.getInventory().removeItem(leftover);
            }
            return KitGrantResult.of(KitGrantResult.Status.INVENTORY_FULL, kit);
        }

        for (PotionEffect effect : kit.getPotionEffects()) {
            player.addPotionEffect(effect, true);
        }

        if (kit.getCooldownMillis() > 0L) {
            cooldownService.set(player.getUniqueId(), cooldownKey, kit.getCooldownMillis());
        }
        return KitGrantResult.of(KitGrantResult.Status.SUCCESS, kit);
    }

    @Override
    public Collection<KitDefinition> getAccessibleKits(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        PlayerProfile profile = profileService.getCached(player.getUniqueId());
        if (profile == null) {
            return Collections.emptyList();
        }
        List<KitDefinition> accessible = new ArrayList<KitDefinition>();
        for (KitDefinition kit : kitRegistry.getAll()) {
            if (profile.getRank().isAtLeast(kit.getRequiredRank())) {
                accessible.add(kit);
            }
        }
        accessible.sort(Comparator.comparingInt(kit -> kit.getRequiredRank().getTier()));
        return Collections.unmodifiableList(accessible);
    }

    @Override
    public Collection<KitDefinition> getAllKits() {
        List<KitDefinition> all = new ArrayList<KitDefinition>(kitRegistry.getAll());
        all.sort(Comparator.comparingInt(kit -> kit.getRequiredRank().getTier()));
        return Collections.unmodifiableList(all);
    }

    private boolean hasEnoughEmptySlots(PlayerInventory inventory, int requiredSlots) {
        if (requiredSlots <= 0) {
            return true;
        }
        int empty = 0;
        ItemStack[] contents = inventory.getContents();
        if (contents != null) {
            for (ItemStack content : contents) {
                if (content == null || content.getType() == Material.AIR) {
                    empty++;
                }
            }
        }
        return empty >= requiredSlots;
    }

    private String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
