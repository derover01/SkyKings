package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.model.Rank;
import net.skykings.core.ui.UiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

/** Premium Crate Center mit Rank-Rail, Claim-Cards und klaren Statuszustaenden. */
public final class CrateRewardsGui {

    public static final String PERMISSION = "skykings.perk.craterewards";

    private static final Tier[] TIERS = new Tier[] {
            new Tier(Rank.KNIGHT, 2L, "common", 1, Material.IRON_SWORD, ChatColor.WHITE),
            new Tier(Rank.PHOENIX, 4L, "epic", 1, Material.BLAZE_POWDER, ChatColor.LIGHT_PURPLE),
            new Tier(Rank.ETERNAL, 8L, "epic", 2, Material.ENDER_PEARL, ChatColor.AQUA),
            new Tier(Rank.EXILE, 12L, "legendary", 1, Material.OBSIDIAN, ChatColor.GOLD),
            new Tier(Rank.ENDLING, 18L, "royal", 1, Material.ENDER_STONE, ChatColor.DARK_PURPLE),
            new Tier(Rank.KING, 24L, "king", 1, Material.GOLD_BLOCK, ChatColor.YELLOW)
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
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Crate Center", 54);

        gui.setItem(4, UiItems.head(player.getName(),
                ChatColor.GOLD.toString() + ChatColor.BOLD + "CRATE CENTER",
                ChatColor.GRAY + "Deine Rang-Rewards auf einen Blick.",
                ChatColor.DARK_GRAY + "READY • COOLDOWN • LOCKED"));

        // obere Rail: Progression der Rangstufen
        int[] rail = {10, 12, 14, 16, 28, 30};
        for (int i = 0; i < TIERS.length; i++) {
            Tier tier = TIERS[i];
            gui.setItem(rail[i], railIcon(player, tier));
        }

        // grosse Claim-Cards im Content-Bereich
        int[] cards = {19, 21, 23, 25, 37, 39};
        for (int i = 0; i < TIERS.length; i++) {
            final Tier tier = TIERS[i];
            gui.setItem(cards[i], claimCard(player, tier), (p,e,s) -> claim(p, tier));
        }

        gui.setItem(42, UiItems.item(Material.CHEST,
                ChatColor.AQUA.toString() + ChatColor.BOLD + "SO FUNKTIONIERT'S",
                ChatColor.GRAY + "Jeder Rang besitzt einen eigenen Reward.",
                ChatColor.GRAY + "Einmal abholen, dann läuft der Cooldown.",
                ChatColor.DARK_GRAY + "Höhere Ränge schalten weitere Tiers frei."));

        gui.setItem(44, UiItems.item(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "CRATE PREVIEW",
                ChatColor.GRAY + "Crate in der Hand:",
                ChatColor.WHITE + "Linksklick = Rewards",
                ChatColor.WHITE + "Rechtsklick = Öffnen"));

        gui.setItem(49, UiItems.item(Material.GOLD_NUGGET,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "DEIN STATUS",
                ChatColor.GRAY + "Rang: " + ChatColor.WHITE + display(core.getRankService().getRank(player.getUniqueId())),
                ChatColor.GRAY + "Freigeschaltet: " + ChatColor.WHITE + unlockedCount(player) + "/" + TIERS.length));

        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.25F);
    }

    private ItemStack railIcon(Player player, Tier tier) {
        boolean access = canAccess(player, tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key(tier));
        String state = !access ? ChatColor.RED + "LOCKED"
                : remaining > 0L ? ChatColor.YELLOW + "COOLDOWN"
                : ChatColor.GREEN + "READY";

        return UiItems.item(tier.material,
                tier.color + ChatColor.BOLD.toString() + display(tier.rank),
                ChatColor.GRAY + tier.amount + "x " + prettyCrate(tier.crateId),
                state);
    }

    private ItemStack claimCard(Player player, Tier tier) {
        boolean access = canAccess(player, tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key(tier));

        if (!access) {
            return UiItems.item(Material.INK_SACK, (short) 8,
                    ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + display(tier.rank) + " REWARD",
                    ChatColor.GRAY + tier.amount + "x " + prettyCrate(tier.crateId),
                    ChatColor.RED + "LOCKED",
                    ChatColor.DARK_GRAY + "Benötigt mindestens " + display(tier.rank));
        }

        if (remaining > 0L) {
            return UiItems.item(Material.WATCH,
                    tier.color + ChatColor.BOLD.toString() + display(tier.rank) + " REWARD",
                    ChatColor.GRAY + tier.amount + "x " + prettyCrate(tier.crateId),
                    ChatColor.YELLOW + "COOLDOWN • " + formatDuration(remaining),
                    ChatColor.DARK_GRAY + "Noch nicht verfügbar");
        }

        return UiItems.item(Material.ENDER_CHEST,
                tier.color + ChatColor.BOLD.toString() + display(tier.rank) + " REWARD",
                ChatColor.GRAY + tier.amount + "x " + prettyCrate(tier.crateId),
                ChatColor.GRAY + "Cooldown danach: " + ChatColor.WHITE + tier.hours + "h",
                ChatColor.GREEN.toString() + ChatColor.BOLD + "READY",
                UiItems.action("Klicken zum Abholen"));
    }

    private boolean canAccess(Player player, Tier tier) {
        return player.hasPermission(PERMISSION) || core.getRankService().hasAtLeast(player.getUniqueId(), tier.rank);
    }

    private void claim(Player player, Tier tier) {
        if (!canAccess(player, tier)) {
            player.sendMessage(ChatColor.RED + "Du benötigst mindestens " + display(tier.rank) + " oder das Crate-Rewards-Recht.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }
        String key = key(tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key);
        if (remaining > 0L) {
            player.sendMessage(ChatColor.RED + "Dieser Reward ist noch " + formatDuration(remaining) + " im Cooldown.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.6F, 0.8F);
            return;
        }
        CrateRegistry.CrateDefinition crate = registry.get(tier.crateId);
        if (crate == null) {
            player.sendMessage(ChatColor.RED + "Der konfigurierte Crate-Typ ist aktuell nicht verfügbar.");
            return;
        }
        ItemStack stack = codec.create(crate, tier.amount);
        if (!hasSpace(player, stack)) {
            player.sendMessage(ChatColor.RED + "Du brauchst einen freien Inventarplatz.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }

        long duration = tier.hours * 60L * 60L * 1000L;
        core.getCooldownService().set(player.getUniqueId(), key, duration);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            core.getCooldownService().remove(player.getUniqueId(), key);
            player.sendMessage(ChatColor.RED + "Reward konnte nicht sicher ins Inventar gelegt werden. Cooldown wurde zurückgesetzt.");
            return;
        }

        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Crate-Reward abgeholt: " + ChatColor.WHITE + tier.amount + "x "
                + ChatColor.translateAlternateColorCodes('&', crate.getDisplayName()));
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.4F);
        open(player);
    }

    private boolean hasSpace(Player player, ItemStack item) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(item.clone()).isEmpty();
    }

    private int unlockedCount(Player player) {
        int count = 0;
        for (Tier tier : TIERS) if (canAccess(player, tier)) count++;
        return count;
    }

    private String prettyCrate(String id) {
        if (id == null || id.isEmpty()) return "Crate";
        String raw = id.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1) + " Crate";
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
        final Rank rank;
        final long hours;
        final String crateId;
        final int amount;
        final Material material;
        final ChatColor color;

        Tier(Rank rank, long hours, String crateId, int amount, Material material, ChatColor color) {
            this.rank = rank;
            this.hours = hours;
            this.crateId = crateId;
            this.amount = amount;
            this.material = material;
            this.color = color;
        }
    }
}
