package net.skykings.combat.weapon;

import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** StatTrack lebt direkt auf dem Item und bleibt daher bei Trade/Drop erhalten. */
public final class StatTrackItemService implements Listener {
    private static final String WEAPON_MARKER = ChatColor.BLACK + "skykings:stattrack:";
    private static final String MODULE_MARKER = ChatColor.BLACK + "skykings:stattrack-module";

    public ItemStack createModule(int amount) {
        ItemStack module = new ItemStack(Material.NAME_TAG, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = module.getItemMeta();
        meta.setDisplayName(UiTheme.PRIMARY + "StatTrack Module");
        List<String> lore = new ArrayList<String>();
        lore.add(UiTheme.MUTED + "Speichert legitime PvP-Kills auf einer Waffe.");
        lore.add("");
        lore.add(UiTheme.TEXT + "Die Geschichte bleibt beim Trading erhalten.");
        lore.add(UiTheme.WARNING + "/stattrack apply" + UiTheme.MUTED + " mit Waffe in der Hand");
        lore.add(MODULE_MARKER);
        meta.setLore(lore);
        module.setItemMeta(meta);
        return module;
    }

    public boolean isModule(ItemStack stack) {
        if (stack == null || stack.getType() != Material.NAME_TAG || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        for (String line : meta.getLore()) if (MODULE_MARKER.equals(line)) return true;
        return false;
    }

    public boolean isTrackableWeapon(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() != 1) return false;
        String name = stack.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || stack.getType() == Material.BOW;
    }

    public boolean hasStatTrack(ItemStack stack) {
        return decode(stack) != null;
    }

    public boolean apply(Player player, ItemStack weapon) {
        if (!isTrackableWeapon(weapon) || hasStatTrack(weapon)) return false;
        if (!consumeModule(player)) return false;
        StatData data = new StatData(UUID.randomUUID(), player.getUniqueId(), player.getName(), 0L);
        write(weapon, data);
        player.updateInventory();
        SoundFeedback.success(player);
        return true;
    }

    @EventHandler
    public void onKill(SkyKingsPlayerKillEvent event) {
        // Komplett abgefarmte Kills duerfen Sammler-Items nicht aufblasen.
        if (event.getAntiFarmMultiplier() <= 0D) return;
        Player killer = Bukkit.getPlayer(event.getKillerUuid());
        if (killer == null) return;
        ItemStack weapon = killer.getItemInHand();
        StatData data = decode(weapon);
        if (data == null) return;
        data.kills++;
        data.currentOwner = killer.getUniqueId();
        data.currentOwnerName = killer.getName();
        write(weapon, data);
        killer.updateInventory();
        if (data.kills == 100L || data.kills == 500L || data.kills == 1000L || data.kills == 5000L) {
            killer.sendMessage(UiTheme.PRIMARY + "Weapon History");
            killer.sendMessage(UiTheme.TEXT + UiFormat.number(data.kills) + UiTheme.MUTED + " legitime Kills auf dieser Waffe.");
            SoundFeedback.reward(killer);
        }
    }

    public StatData decode(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(WEAPON_MARKER)) continue;
            try {
                String[] parts = line.substring(WEAPON_MARKER.length()).split("\\|");
                UUID id = UUID.fromString(parts[0]);
                UUID owner = UUID.fromString(parts[1]);
                long kills = Long.parseLong(parts[2]);
                String ownerName = parts.length >= 4 ? parts[3] : owner.toString().substring(0, 8);
                return new StatData(id, owner, ownerName, kills);
            } catch (RuntimeException ignored) { return null; }
        }
        return null;
    }

    private void write(ItemStack stack, StatData data) {
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        removeOldBlock(lore);
        if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) lore.add("");
        lore.add(UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(data.kills));
        lore.add(UiTheme.MUTED + "Owner " + UiTheme.TEXT + data.currentOwnerName);
        lore.add(UiTheme.MUTED + "Weapon ID " + UiTheme.DISABLED + data.id.toString().substring(0, 8));
        lore.add(tier(data.kills));
        lore.add(WEAPON_MARKER + data.id + "|" + data.currentOwner + "|" + data.kills + "|" + data.currentOwnerName);
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    private void removeOldBlock(List<String> lore) {
        int marker = -1;
        for (int i = 0; i < lore.size(); i++) if (lore.get(i) != null && lore.get(i).startsWith(WEAPON_MARKER)) { marker = i; break; }
        if (marker < 0) return;
        int start = Math.max(0, marker - 4);
        for (int i = marker; i >= start; i--) lore.remove(i);
        if (!lore.isEmpty() && lore.get(lore.size() - 1).isEmpty()) lore.remove(lore.size() - 1);
    }

    private String tier(long kills) {
        if (kills >= 5000L) return UiTheme.MYTHIC + "MYTHIC HISTORY";
        if (kills >= 1000L) return UiTheme.LEGENDARY + "LEGENDARY HISTORY";
        if (kills >= 500L) return UiTheme.PRIMARY + "ELITE HISTORY";
        if (kills >= 100L) return UiTheme.SUCCESS + "VETERAN HISTORY";
        return UiTheme.DISABLED + "STATTRACK";
    }

    private boolean consumeModule(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isModule(stack)) continue;
            if (stack.getAmount() <= 1) player.getInventory().setItem(slot, null);
            else stack.setAmount(stack.getAmount() - 1);
            return true;
        }
        return false;
    }

    public static final class StatData {
        private final UUID id;
        private UUID currentOwner;
        private String currentOwnerName;
        private long kills;
        StatData(UUID id, UUID currentOwner, String currentOwnerName, long kills) {
            this.id = id; this.currentOwner = currentOwner; this.currentOwnerName = currentOwnerName; this.kills = kills;
        }
        public UUID getId() { return id; }
        public UUID getCurrentOwner() { return currentOwner; }
        public String getCurrentOwnerName() { return currentOwnerName; }
        public long getKills() { return kills; }
    }
}
