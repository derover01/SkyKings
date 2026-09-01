package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.model.Rank;
import net.skykings.core.shop.ShopCurrency;
import net.skykings.core.shop.ShopOffer;
import net.skykings.core.shop.ShopPurchaseResult;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
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

/** Premium Crate Center mit Rang-Rewards und einem getrennten, teuren Crate Market. */
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
                ChatColor.GRAY + "Rang-Rewards und Crate Market.",
                ChatColor.DARK_GRAY + "READY • COOLDOWN • LOCKED"));

        int[] rail = {10, 12, 14, 16, 28, 30};
        for (int i = 0; i < TIERS.length; i++) gui.setItem(rail[i], railIcon(player, TIERS[i]));

        int[] cards = {19, 21, 23, 25, 37, 39};
        for (int i = 0; i < TIERS.length; i++) {
            final Tier tier = TIERS[i];
            gui.setItem(cards[i], claimCard(player, tier), (p,e,s) -> claim(p, tier));
        }

        gui.setItem(42, UiItems.item(Material.EMERALD_BLOCK,
                ChatColor.GREEN.toString() + ChatColor.BOLD + "CRATE MARKET",
                ChatColor.GRAY + "Build, Fight, Money, Utility & mehr.",
                ChatColor.YELLOW + "High-End Crates bleiben teuer.",
                UiItems.action("Market oeffnen")), (p,e,s) -> openMarket(p));

        gui.setItem(44, UiItems.item(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "CRATE PREVIEW",
                ChatColor.GRAY + "Crate in der Hand:",
                ChatColor.WHITE + "Linksklick = Rewards",
                ChatColor.WHITE + "Rechtsklick = Oeffnen"));

        gui.setItem(49, UiItems.item(Material.GOLD_NUGGET,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "DEIN STATUS",
                ChatColor.GRAY + "Rang: " + ChatColor.WHITE + display(core.getRankService().getRank(player.getUniqueId())),
                ChatColor.GRAY + "Freigeschaltet: " + ChatColor.WHITE + unlockedCount(player) + "/" + TIERS.length,
                ChatColor.GRAY + "Coins: " + ChatColor.WHITE + UiFormat.number(core.getShopTransactionService().getCoinBalance(player.getUniqueId()))));

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openMarket(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Crate Market", 54);
        gui.setItem(4, UiItems.item(Material.ENDER_CHEST,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "CRATE MARKET",
                ChatColor.GRAY + "Normale Crates sind erreichbar.",
                ChatColor.YELLOW + "Seltene Crates sind massive Coin-Sinks.",
                ChatColor.DARK_PURPLE + "Royal & King: nicht normal kaeuflich."));

        market(gui, player, 10, "build", 4, 650000L, Material.BRICK, ChatColor.GREEN);
        market(gui, player, 12, "fight", 4, 1200000L, Material.DIAMOND_SWORD, ChatColor.RED);
        market(gui, player, 14, "money", 4, 1500000L, Material.GOLD_INGOT, ChatColor.GOLD);
        market(gui, player, 16, "utility", 4, 1200000L, Material.ENDER_PEARL, ChatColor.AQUA);
        market(gui, player, 28, "event", 2, 2500000L, Material.FIREWORK, ChatColor.LIGHT_PURPLE);
        market(gui, player, 30, "common", 1, 2000000L, Material.CHEST, ChatColor.WHITE);
        market(gui, player, 32, "rare", 1, 8000000L, Material.LAPIS_BLOCK, ChatColor.BLUE);
        market(gui, player, 34, "epic", 1, 25000000L, Material.OBSIDIAN, ChatColor.DARK_PURPLE);
        market(gui, player, 40, "legendary", 1, 75000000L, Material.GOLD_BLOCK, ChatColor.GOLD);

        gui.setItem(43, UiItems.item(Material.BEDROCK,
                ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "ROYAL / KING",
                ChatColor.GRAY + "Nicht im normalen Shop.",
                ChatColor.GRAY + "Events, Rang-Rewards und besondere Systeme",
                ChatColor.YELLOW + "sollen diese Crates wertvoll halten."));
        gui.setItem(45, UiItems.item(Material.ARROW, ChatColor.WHITE + "Zurueck", UiItems.action("Crate Center")), (p,e,s) -> open(p));
        gui.setItem(49, UiItems.item(Material.GOLD_NUGGET,
                ChatColor.GOLD + "Deine Coins",
                ChatColor.WHITE + UiFormat.number(core.getShopTransactionService().getCoinBalance(player.getUniqueId()))));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void market(GuiSession gui, Player player, int slot, final String crateId, final int amount,
                        final long price, Material iconMaterial, ChatColor color) {
        CrateRegistry.CrateDefinition definition = registry.get(crateId);
        if (definition == null) return;
        long balance = core.getShopTransactionService().getCoinBalance(player.getUniqueId());
        String state = balance >= price ? UiItems.action("Klicken zum Kaufen")
                : ChatColor.RED + "Fehlen: " + UiFormat.number(price - balance) + " Coins";
        gui.setItem(slot, UiItems.item(iconMaterial,
                color.toString() + ChatColor.BOLD + amount + "x " + prettyCrate(crateId),
                ChatColor.GRAY + "Preis: " + ChatColor.WHITE + UiFormat.number(price) + " Coins",
                ChatColor.GRAY + "Guthaben: " + ChatColor.WHITE + UiFormat.number(balance),
                state), (p,e,s) -> buyCrate(p, crateId, amount, price));
    }

    private void buyCrate(Player player, String crateId, int amount, long price) {
        if (core.getShopTransactionService().getCoinBalance(player.getUniqueId()) < price) {
            player.sendMessage(ChatColor.RED + "Nicht genug Coins fuer diesen Crate-Kauf.");
            SoundFeedback.error(player);
            return;
        }
        CrateRegistry.CrateDefinition crate = registry.get(crateId);
        if (crate == null) {
            player.sendMessage(ChatColor.RED + "Dieser Crate-Typ ist gerade nicht verfuegbar.");
            SoundFeedback.error(player);
            return;
        }
        ItemStack stack;
        try { stack = codec.create(crate, amount); }
        catch (RuntimeException ex) {
            player.sendMessage(ChatColor.RED + "Crate konnte nicht sicher registriert werden.");
            SoundFeedback.error(player);
            return;
        }
        ShopOffer offer = new ShopOffer("crate-market-" + crateId + "-" + amount,
                stack, ShopCurrency.COINS, price);
        ShopPurchaseResult result = core.getShopTransactionService().purchase(player, offer, "CRATE_MARKET");
        if (result == ShopPurchaseResult.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Gekauft: " + ChatColor.WHITE + amount + "x " + prettyCrate(crateId));
            SoundFeedback.reward(player);
            openMarket(player);
        } else if (result == ShopPurchaseResult.INVENTORY_FULL) {
            player.sendMessage(ChatColor.RED + "Inventar voll.");
            SoundFeedback.error(player);
        } else if (result == ShopPurchaseResult.NOT_ENOUGH_MONEY) {
            player.sendMessage(ChatColor.RED + "Nicht genug Coins.");
            SoundFeedback.error(player);
        } else {
            player.sendMessage(ChatColor.RED + "Crate-Kauf konnte nicht sicher abgeschlossen werden.");
            SoundFeedback.error(player);
        }
    }

    private ItemStack railIcon(Player player, Tier tier) {
        boolean access = canAccess(player, tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key(tier));
        String state = !access ? ChatColor.RED + "LOCKED"
                : remaining > 0L ? ChatColor.YELLOW + "COOLDOWN"
                : ChatColor.GREEN + "READY";
        return UiItems.item(tier.material,
                tier.color + ChatColor.BOLD.toString() + display(tier.rank),
                ChatColor.GRAY.toString() + tier.amount + "x " + prettyCrate(tier.crateId), state);
    }

    private ItemStack claimCard(Player player, Tier tier) {
        boolean access = canAccess(player, tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key(tier));
        if (!access) return UiItems.item(Material.INK_SACK, (short) 8,
                ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + display(tier.rank) + " REWARD",
                ChatColor.GRAY.toString() + tier.amount + "x " + prettyCrate(tier.crateId),
                ChatColor.RED + "LOCKED", ChatColor.DARK_GRAY + "Benoetigt mindestens " + display(tier.rank));
        if (remaining > 0L) return UiItems.item(Material.WATCH,
                tier.color + ChatColor.BOLD.toString() + display(tier.rank) + " REWARD",
                ChatColor.GRAY.toString() + tier.amount + "x " + prettyCrate(tier.crateId),
                ChatColor.YELLOW + "COOLDOWN • " + formatDuration(remaining), ChatColor.DARK_GRAY + "Noch nicht verfuegbar");
        return UiItems.item(Material.ENDER_CHEST,
                tier.color + ChatColor.BOLD.toString() + display(tier.rank) + " REWARD",
                ChatColor.GRAY.toString() + tier.amount + "x " + prettyCrate(tier.crateId),
                ChatColor.GRAY + "Cooldown danach: " + ChatColor.WHITE + tier.hours + "h",
                ChatColor.GREEN.toString() + ChatColor.BOLD + "READY", UiItems.action("Klicken zum Abholen"));
    }

    private boolean canAccess(Player player, Tier tier) {
        return player.hasPermission(PERMISSION) || core.getRankService().hasAtLeast(player.getUniqueId(), tier.rank);
    }

    private void claim(Player player, Tier tier) {
        if (!canAccess(player, tier)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens " + display(tier.rank) + " oder das Crate-Rewards-Recht.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return;
        }
        String key = key(tier);
        long remaining = core.getCooldownService().getRemainingMillis(player.getUniqueId(), key);
        if (remaining > 0L) {
            player.sendMessage(ChatColor.RED + "Dieser Reward ist noch " + formatDuration(remaining) + " im Cooldown.");
            SoundFeedback.error(player); return;
        }
        CrateRegistry.CrateDefinition crate = registry.get(tier.crateId);
        if (crate == null) { player.sendMessage(ChatColor.RED + "Crate-Typ nicht verfuegbar."); return; }
        ItemStack stack = codec.create(crate, tier.amount);
        if (!hasSpace(player, stack)) {
            player.sendMessage(ChatColor.RED + "Du brauchst einen freien Inventarplatz."); SoundFeedback.error(player); return;
        }
        long duration = tier.hours * 60L * 60L * 1000L;
        core.getCooldownService().set(player.getUniqueId(), key, duration);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            core.getCooldownService().remove(player.getUniqueId(), key);
            player.sendMessage(ChatColor.RED + "Reward konnte nicht sicher ins Inventar gelegt werden. Cooldown wurde zurueckgesetzt."); return;
        }
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Crate-Reward abgeholt: " + ChatColor.WHITE + tier.amount + "x "
                + ChatColor.translateAlternateColorCodes('&', crate.getDisplayName()));
        SoundFeedback.reward(player); open(player);
    }

    private boolean hasSpace(Player player, ItemStack item) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(item.clone()).isEmpty();
    }

    private int unlockedCount(Player player) { int count = 0; for (Tier tier : TIERS) if (canAccess(player, tier)) count++; return count; }
    private String prettyCrate(String id) { if (id == null || id.isEmpty()) return "Crate"; String raw=id.toLowerCase(Locale.ROOT); return Character.toUpperCase(raw.charAt(0))+raw.substring(1)+" Crate"; }
    private String key(Tier tier) { return "craterewards:" + tier.rank.name().toLowerCase(Locale.ROOT); }
    private String display(Rank rank) { String raw=rank.name().toLowerCase(Locale.ROOT); return Character.toUpperCase(raw.charAt(0))+raw.substring(1); }
    private String formatDuration(long millis) { long seconds=Math.max(1L,(millis+999L)/1000L); long hours=seconds/3600L; long minutes=(seconds%3600L)/60L; return hours>0L?hours+"h "+minutes+"m":minutes+"m"; }

    private static final class Tier {
        final Rank rank; final long hours; final String crateId; final int amount; final Material material; final ChatColor color;
        Tier(Rank rank,long hours,String crateId,int amount,Material material,ChatColor color) {
            this.rank=rank; this.hours=hours; this.crateId=crateId; this.amount=amount; this.material=material; this.color=color;
        }
    }
}
