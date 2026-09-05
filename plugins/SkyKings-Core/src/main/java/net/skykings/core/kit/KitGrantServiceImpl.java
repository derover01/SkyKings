package net.skykings.core.kit;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class KitGrantServiceImpl implements KitGrantService {

    private static final String COOLDOWN_PREFIX = "kit:";

    private final KitRegistry kitRegistry;
    private final PlayerProfileService profileService;
    private final CooldownService cooldownService;
    private final GameplaySettlementJournal settlementJournal;

    public KitGrantServiceImpl(KitRegistry kitRegistry, PlayerProfileService profileService,
                               CooldownService cooldownService) {
        this(kitRegistry, profileService, cooldownService, resolveJournal());
    }

    KitGrantServiceImpl(KitRegistry kitRegistry, PlayerProfileService profileService,
                        CooldownService cooldownService, GameplaySettlementJournal settlementJournal) {
        this.kitRegistry = kitRegistry;
        this.profileService = profileService;
        this.cooldownService = cooldownService;
        this.settlementJournal = settlementJournal;
    }

    private static GameplaySettlementJournal resolveJournal() {
        try {
            GameplaySettlementJournal existing = GameplaySettlementJournal.active();
            if (existing != null) return existing;
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(KitGrantServiceImpl.class);
            return plugin == null ? null : new GameplaySettlementJournal(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public synchronized KitGrantResult grant(Player player, String kitId) {
        if (player == null || kitId == null) {
            return KitGrantResult.of(KitGrantResult.Status.NOT_FOUND, null);
        }

        KitDefinition kit = kitRegistry.get(normalize(kitId)).orElse(null);
        if (kit == null) {
            return KitGrantResult.of(KitGrantResult.Status.NOT_FOUND, null);
        }

        UUID playerId = player.getUniqueId();
        if (settlementJournal != null && settlementJournal.hasPendingFor(playerId)) {
            return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
        }

        PlayerProfile profile = profileService.getCached(playerId);
        if (profile == null) {
            return KitGrantResult.of(KitGrantResult.Status.PROFILE_NOT_LOADED, kit);
        }
        if (!profile.getRank().isAtLeast(kit.getRequiredRank())) {
            return KitGrantResult.of(KitGrantResult.Status.NO_PERMISSION, kit);
        }

        String cooldownKey = COOLDOWN_PREFIX + normalize(kit.getId());
        long remaining = cooldownService.getRemainingMillis(playerId, cooldownKey);
        if (remaining > 0L) {
            return KitGrantResult.cooldown(kit, remaining);
        }

        List<ItemStack> items = kit.createItems();
        if (!hasEnoughEmptySlots(player.getInventory(), items.size())) {
            return KitGrantResult.of(KitGrantResult.Status.INVENTORY_FULL, kit);
        }

        UUID transaction = settlementJournal == null ? null : settlementJournal.begin(
                playerId, "KIT_GRANT", normalize(kit.getId()),
                "cooldown-ms=" + kit.getCooldownMillis() + ", items=" + items.size());
        if (settlementJournal != null && transaction == null) {
            return KitGrantResult.of(KitGrantResult.Status.FAILED, kit);
        }

        Map<Integer, ItemStack> inventoryBefore = snapshot(player.getInventory());
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(new ItemStack[items.size()]));
        if (leftovers != null && !leftovers.isEmpty()) {
            restore(player.getInventory(), inventoryBefore);
            if (settlementJournal != null) {
                if (!savePlayerData(player)) {
                    note(transaction, "PARTIAL_GRANT_ROLLBACK_PLAYERDATA_COMMIT_FAILED");
                    return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
                }
                if (!close(transaction, "PARTIAL_GRANT_ROLLBACK_JOURNAL_CLOSE_FAILED")) {
                    return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
                }
            }
            return KitGrantResult.of(KitGrantResult.Status.INVENTORY_FULL, kit);
        }

        if (kit.getCooldownMillis() > 0L) {
            if (!cooldownService.setNow(playerId, cooldownKey, kit.getCooldownMillis())) {
                restore(player.getInventory(), inventoryBefore);
                if (settlementJournal != null) {
                    if (!savePlayerData(player)) {
                        note(transaction, "COOLDOWN_COMMIT_FAILED_AND_INVENTORY_ROLLBACK_COMMIT_FAILED");
                        return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
                    }
                    if (!close(transaction, "COOLDOWN_REJECTED_ROLLBACK_JOURNAL_CLOSE_FAILED")) {
                        return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
                    }
                }
                return KitGrantResult.of(KitGrantResult.Status.FAILED, kit);
            }
        }

        if (settlementJournal != null) {
            if (!savePlayerData(player)) {
                note(transaction, "KIT_ITEMS_PLAYERDATA_COMMIT_FAILED_AFTER_COOLDOWN_COMMIT");
                return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
            }
            if (!close(transaction, "KIT_COMMITTED_BUT_JOURNAL_CLOSE_FAILED")) {
                return KitGrantResult.of(KitGrantResult.Status.REVIEW_REQUIRED, kit);
            }
        }

        for (PotionEffect effect : kit.getPotionEffects()) {
            player.addPotionEffect(effect, true);
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

    private Map<Integer, ItemStack> snapshot(PlayerInventory inventory) {
        Map<Integer, ItemStack> snapshot = new HashMap<Integer, ItemStack>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) snapshot.put(slot, item.clone());
        }
        return snapshot;
    }

    private void restore(PlayerInventory inventory, Map<Integer, ItemStack> snapshot) {
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, null);
        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
    }

    private boolean savePlayerData(Player player) {
        try {
            player.updateInventory();
            player.saveData();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean close(UUID transaction, String reason) {
        if (settlementJournal == null || transaction == null) return true;
        if (settlementJournal.complete(transaction)) return true;
        settlementJournal.noteFailure(transaction, reason);
        return false;
    }

    private void note(UUID transaction, String reason) {
        if (settlementJournal != null && transaction != null) settlementJournal.noteFailure(transaction, reason);
    }

    private String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
