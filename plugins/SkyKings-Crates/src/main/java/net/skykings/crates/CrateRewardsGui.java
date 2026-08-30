package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /craterewards GUI mit eigenem Cooldown pro Paid-Rang-Tier. */
public final class CrateRewardsGui {

    public static final String PERMISSION = "skykings.perk.craterewards";

    private static final Tier[] TIERS = new Tier[] {
            new Tier(Rank.KNIGHT, 2L, "common", 1, Material.IRON_SWORD),
            new Tier(Rank.PHOENIX, 4L, "epic", 1, Material.BLAZE_POWDER),
            new Tier(Rank.ETERNAL, 8L, "epic", 2, Material.ENDER_PEARL),
            new Tier(Rank.EXILE, 12L, "legendary", 1, Material.OBSIDIAN),
            new Tier(Rank.ENDLING, 18L, "royal", 1, Material.ENDER_STONE),
            new Tier(Rank.KING, 24L, "king", 1, Material.GOLD_BLOCK)
    };

    private final GuiManager guiManager;
    private final SkyKingsCoreAPI core;
    private final CrateRegistry registry;
    private final CrateItemCodec codec;

    public CrateRewardsGui(GuiManager guiManager, SkyKingsCoreAPI core, CrateRegistry registry, CrateItemCodec codec) {
        this.guiManager = guiManager;
        this.core = core;
        this.registry = registry;
        this.codec = codec;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Crate Rewards", 27);
        int[] slots = {10, 11, 12, 14, 15, 16};
        for (int i = 0; i < TIERS.length; i++) {
            final Tier tier = TIERS[i];
            gui.setItem(slots[i], icon(player, tier), (p,e,s) -> claim(p, tier));
        }
        gui.setItem(22, info());
        guiManager.open(gui);
    }

    private boolean canAccess(Player player, Tier tier) {
        return player.hasPermission(PERMISSION) || core.getRankService().hasAtLeast(player.getUniqueId(), tier.rank);
    }

    private void claim(Player player, Tier tier) {
        if (!canAccess(player, tier)) {
            player.sendMessage(ChatColor.RED + "Du benötigst mindestens " + display(tier.rank) + " oder das Crate-Rewards-Recht.");
            return;
        }
        String key = key(tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key);
        if (remaining > 0L) {
            player.sendMessage(ChatColor.RED + "Dieser Reward ist noch " + formatDuration(remaining) + " im Cooldown.");
            return;
        }
        CrateRegistry.CrateDefinition crate = registry.get(tier.crateId);
        if (crate == null) {
            player.sendMessage(ChatColor.RED + "Der konfigurierte Crate-Typ ist aktuell nicht verfügbar.");
            return;
        }
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (int i = 0; i < tier.amount; i++) items.add(codec.create(crate));
        if (!hasSpaceForAll(player, items)) {
            player.sendMessage(ChatColor.RED + "Du brauchst mehr freien Inventarplatz.");
            return;
        }
        for (ItemStack item : items) player.getInventory().addItem(item);
        core.getCooldownService().set(player.getUniqueId(), key, tier.hours * 60L * 60L * 1000L);
        player.sendMessage(ChatColor.GREEN + "Crate-Reward für " + display(tier.rank) + ChatColor.GREEN
                + " abgeholt: " + tier.amount + "x " + ChatColor.translateAlternateColorCodes('&', crate.getDisplayName()));
        open(player);
    }

    private boolean hasSpaceForAll(Player player, List<ItemStack> items) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        for (ItemStack item : items) if (!temp.addItem(item.clone()).isEmpty()) return false;
        return true;
    }

    private ItemStack icon(Player player, Tier tier) {
        ItemStack item = new ItemStack(tier.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + display(tier.rank) + ChatColor.YELLOW + " Reward");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Reward: " + ChatColor.WHITE + tier.amount + "x " + tier.crateId + " Crate");
        lore.add(ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + tier.hours + " Stunden");
        boolean access = canAccess(player, tier);
        if (!access) lore.add(ChatColor.RED + "Nicht freigeschaltet");
        else {
            long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key(tier));
            lore.add(remaining > 0L ? ChatColor.RED + "Bereit in: " + formatDuration(remaining)
                    : ChatColor.GREEN + "Bereit • klicken zum Abholen");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack info() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Crate Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Jeder Paid-Rang hat einen eigenen Reward.",
                ChatColor.GRAY + "Höhere Gameplay-Ränge können niedrigere Rewards claimen.",
                ChatColor.DARK_GRAY + "Teamränge geben keinen automatischen Zugriff."));
        item.setItemMeta(meta);
        return item;
    }

    private String key(Tier tier) { return "craterewards:" + tier.rank.name().toLowerCase(Locale.ROOT); }
    private String display(Rank rank) {
        String raw = rank.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
    private String formatDuration(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        return hours > 0L ? hours + "h " + minutes + "m" : minutes + "m";
    }

    private static final class Tier {
        final Rank rank; final long hours; final String crateId; final int amount; final Material material;
        Tier(Rank rank, long hours, String crateId, int amount, Material material) {
            this.rank = rank; this.hours = hours; this.crateId = crateId; this.amount = amount; this.material = material;
        }
    }
}
