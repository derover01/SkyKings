package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.kit.KitDefinition;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.model.Rank;
import net.skykings.core.permission.VoucherPermission;
import net.skykings.core.ui.UiFormat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Owner/Admin-GUI zum sicheren Erzeugen eindeutig serialisierter Gutscheine. */
public final class VoucherAdminGui {
    private static final long[] COIN_VALUES = {25_000L, 50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L, 2_500_000L, 5_000_000L};
    private static final long[] GIVEALL_VALUES = {10_000L, 25_000L, 50_000L, 100_000L, 250_000L, 500_000L};

    private final GuiManager guiManager;
    private final SkyKingsCoreAPI core;
    private final VoucherItemCodec codec;

    public VoucherAdminGui(GuiManager guiManager, SkyKingsCoreAPI core, VoucherItemCodec codec) {
        this.guiManager = guiManager;
        this.core = core;
        this.codec = codec;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Gutscheine", 27);
        gui.setItem(10, icon(Material.DIAMOND, ChatColor.AQUA + "Ranggutscheine", "Free- und Paid-Raenge"), (p,e,s) -> openRanks(p));
        gui.setItem(11, icon(Material.CHEST, ChatColor.GREEN + "Kitgutscheine", "Einmalige Kit-Ausgabe"), (p,e,s) -> openKits(p));
        gui.setItem(12, icon(Material.PAPER, ChatColor.LIGHT_PURPLE + "Rechtegutscheine", "Freigegebene Feature-Rechte"), (p,e,s) -> openPermissions(p));
        gui.setItem(14, icon(Material.NAME_TAG, ChatColor.YELLOW + "Prefixgutscheine", "Kosmetische Prefixe"), (p,e,s) -> openPrefixes(p));
        gui.setItem(15, icon(Material.GOLD_INGOT, ChatColor.GOLD + "Coin-Gutscheine", "Coins fuer den Einloeser"), (p,e,s) -> openCoins(p));
        gui.setItem(16, icon(Material.GOLD_BLOCK, ChatColor.YELLOW + "GiveAll Coin-Gutscheine", "Coins fuer alle Online-Spieler"), (p,e,s) -> openGiveAllCoins(p));
        guiManager.open(gui);
    }

    private void openRanks(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutscheine | Raenge", 27);
        int slot = 0;
        for (Rank rank : Rank.values()) {
            final Rank selected = rank;
            gui.setItem(slot++, icon(Material.DIAMOND, ChatColor.AQUA + display(rank), "Klicken: Gutschein erzeugen"),
                    (p,e,s) -> generate(p, VoucherItemCodec.VoucherType.RANK,
                            selected.name().toLowerCase(), display(selected).toUpperCase()));
        }
        back(gui);
        guiManager.open(gui);
    }

    private void openKits(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutscheine | Kits", 27);
        int slot = 0;
        for (KitDefinition kit : core.getKitRegistry().getAll()) {
            if (slot >= 18) break;
            final KitDefinition selected = kit;
            String pretty = translate(selected.getDisplayName());
            gui.setItem(slot++, icon(Material.CHEST, pretty, "Klicken: einmaligen Kitgutschein erzeugen"),
                    (p,e,s) -> generate(p, VoucherItemCodec.VoucherType.KIT,
                            selected.getId(), ChatColor.stripColor(pretty)));
        }
        back(gui);
        guiManager.open(gui);
    }

    private void openPermissions(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutscheine | Rechte", 27);
        int slot = 0;
        for (VoucherPermission permission : core.getVoucherPermissionService().getAll()) {
            final VoucherPermission selected = permission;
            gui.setItem(slot++, icon(Material.PAPER, selected.getDisplayName(), "Klicken: Rechtegutschein erzeugen"),
                    (p,e,s) -> generate(p, VoucherItemCodec.VoucherType.PERMISSION,
                            selected.getId(), ChatColor.stripColor(selected.getDisplayName())));
        }
        back(gui);
        guiManager.open(gui);
    }

    private void openPrefixes(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutscheine | Prefixe", 27);
        addPrefix(gui, 10, "fighter", "Fighter", ChatColor.RED);
        addPrefix(gui, 11, "hunter", "Hunter", ChatColor.DARK_AQUA);
        addPrefix(gui, 12, "lucky", "Lucky", ChatColor.GREEN);
        addPrefix(gui, 14, "royal", "Royal", ChatColor.LIGHT_PURPLE);
        addPrefix(gui, 15, "legend", "Legend", ChatColor.GOLD);
        addPrefix(gui, 16, "kingkiller", "KingKiller", ChatColor.DARK_RED);
        back(gui);
        guiManager.open(gui);
    }

    private void openCoins(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutscheine | Coins", 27);
        for (int i = 0; i < COIN_VALUES.length; i++) {
            final long amount = COIN_VALUES[i];
            gui.setItem(i, icon(Material.GOLD_INGOT, ChatColor.GOLD + UiFormat.coins(amount),
                    "Nur der Einloeser erhaelt die Coins.", "Klicken: Gutschein erzeugen"),
                    (p,e,s) -> generate(p, VoucherItemCodec.VoucherType.COINS,
                            Long.toString(amount), UiFormat.coins(amount)));
        }
        back(gui);
        guiManager.open(gui);
    }

    private void openGiveAllCoins(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Gutscheine | GiveAll", 27);
        for (int i = 0; i < GIVEALL_VALUES.length; i++) {
            final long amount = GIVEALL_VALUES[i];
            gui.setItem(i, icon(Material.GOLD_BLOCK, ChatColor.YELLOW + "GiveAll " + UiFormat.coins(amount),
                    "Jeder aktuell Online-Spieler erhaelt den Betrag.", "Klicken: Gutschein erzeugen"),
                    (p,e,s) -> generate(p, VoucherItemCodec.VoucherType.GIVEALL_COINS,
                            Long.toString(amount), UiFormat.coins(amount)));
        }
        back(gui);
        guiManager.open(gui);
    }

    private void addPrefix(GuiSession gui, int slot, String id, String name, ChatColor color) {
        gui.setItem(slot, icon(Material.NAME_TAG, color + name, "Klicken: Prefixgutschein erzeugen"),
                (p,e,s) -> generate(p, VoucherItemCodec.VoucherType.PREFIX, id, name));
    }

    private void generate(Player player, VoucherItemCodec.VoucherType type, String target, String display) {
        ItemStack voucher = codec.create(type, target, display);
        VoucherItemCodec.DecodedVoucher decoded = codec.decode(voucher);
        if (!player.getInventory().addItem(voucher).isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du brauchst einen freien Inventarplatz.");
            return;
        }
        if (decoded != null) {
            core.getLoggingService().log(new AuditEvent(AuditEventType.VOUCHER_GENERATED,
                    player.getUniqueId(), player.getName(), null,
                    "serial=" + decoded.getSerial() + ", type=" + type + ", target=" + target));
        }
        player.sendMessage(ChatColor.GREEN + "Gutschein erzeugt: " + ChatColor.WHITE + display);
    }

    private void back(GuiSession gui) {
        gui.setItem(22, icon(Material.ARROW, ChatColor.YELLOW + "Zurueck", "Zur Gutschein-Uebersicht"), (p,e,s) -> open(p));
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String translate(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private String display(Rank rank) {
        String raw = rank.name().toLowerCase();
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
