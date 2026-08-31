package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Kodiert Gutschein-Typ, Ziel und eindeutige Seriennummer persistent in Lore (1.8-kompatibel). */
public final class VoucherItemCodec {

    public enum VoucherType { RANK, KIT, PERMISSION, PREFIX, COINS, GIVEALL_COINS }

    private static final String MARKER = ChatColor.BLACK + "skykings:voucher:";
    private final IssuedItemStore issuedStore;

    /** Nutzt im laufenden Plugin automatisch das aktive Issued-Registry; in isolierten Tests null. */
    public VoucherItemCodec() { this(IssuedItemStore.active()); }

    public VoucherItemCodec(IssuedItemStore issuedStore) {
        this.issuedStore = issuedStore;
    }

    public ItemStack create(VoucherType type, String target, String displayTarget) {
        String cleanTarget = cleanDisplay(target, displayTarget);
        UUID serial = UUID.randomUUID();
        if (issuedStore != null && !issuedStore.issueVoucher(serial, type, sanitize(target))) {
            throw new IllegalStateException("Voucher-Serial konnte nicht sicher registriert werden");
        }
        ItemStack item = baseItem(type, cleanTarget, true);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<String>(meta.getLore());
        lore.add(MARKER + type.name().toLowerCase(Locale.ROOT) + ":" + sanitize(target) + ":" + serial);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Nur fuer GUI-Preview/Inventar-Simulation. Erzeugt absichtlich keine gueltige Serial. */
    public ItemStack preview(VoucherType type, String displayTarget) {
        String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                displayTarget == null ? "" : displayTarget));
        return baseItem(type, cleanTarget, false);
    }

    private ItemStack baseItem(VoucherType type, String cleanTarget, boolean redeemable) {
        ItemStack item = new ItemStack(materialFor(type), 1, dataFor(type));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorFor(type) + ChatColor.BOLD.toString() + nameFor(type));
        List<String> lore = new ArrayList<String>();
        if (type == VoucherType.COINS) {
            lore.add(ChatColor.GRAY + "Wert: " + ChatColor.GOLD + cleanTarget);
            lore.add(ChatColor.DARK_GRAY + "Nur fuer dich.");
        } else if (type == VoucherType.GIVEALL_COINS) {
            lore.add(ChatColor.GRAY + "Pro Spieler: " + ChatColor.GOLD + cleanTarget);
            lore.add(ChatColor.YELLOW + "Fuer alle aktuell Online-Spieler.");
        } else {
            lore.add(ChatColor.GRAY + "Belohnung: " + ChatColor.WHITE + cleanTarget);
        }
        lore.add("");
        lore.add(redeemable ? ChatColor.YELLOW + "Rechtsklick: einloesen" : ChatColor.GRAY + "Crate-Reward Preview");
        if (redeemable) lore.add(ChatColor.DARK_GRAY + "Einmalig • SkyKings");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public DecodedVoucher decode(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(MARKER)) continue;
            String[] parts = line.substring(MARKER.length()).split(":", 3);
            if (parts.length != 3) return null;
            try {
                VoucherType type = VoucherType.valueOf(parts[0].toUpperCase(Locale.ROOT));
                String target = parts[1];
                UUID serial = UUID.fromString(parts[2]);
                if (issuedStore != null && !issuedStore.isIssuedVoucher(serial, type, target)) return null;
                return new DecodedVoucher(type, target, serial);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String cleanDisplay(String target, String displayTarget) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                displayTarget == null ? target : displayTarget));
    }

    private Material materialFor(VoucherType type) {
        switch (type) {
            case RANK:
            case PERMISSION:
                return Material.BOOK;
            case KIT:
                return Material.PAPER;
            case PREFIX:
                return Material.NAME_TAG;
            case COINS:
            case GIVEALL_COINS:
                return Material.DOUBLE_PLANT;
            default:
                return Material.PAPER;
        }
    }

    private short dataFor(VoucherType type) {
        // DOUBLE_PLANT:0 ist in 1.8 das Sonnenblumen-Item.
        return 0;
    }

    private String sanitize(String target) {
        return target == null ? "" : target.trim().toLowerCase(Locale.ROOT).replace(":", "");
    }

    private String nameFor(VoucherType type) {
        switch (type) {
            case RANK: return "Ranggutschein";
            case KIT: return "Kitgutschein";
            case PERMISSION: return "Rechtegutschein";
            case PREFIX: return "Prefixgutschein";
            case COINS: return "Coin-Gutschein";
            case GIVEALL_COINS: return "GiveAll Coin-Gutschein";
            default: return "Gutschein";
        }
    }

    private ChatColor colorFor(VoucherType type) {
        switch (type) {
            case RANK: return ChatColor.AQUA;
            case KIT: return ChatColor.GREEN;
            case PERMISSION: return ChatColor.LIGHT_PURPLE;
            case PREFIX: return ChatColor.YELLOW;
            case COINS: return ChatColor.GOLD;
            case GIVEALL_COINS: return ChatColor.YELLOW;
            default: return ChatColor.WHITE;
        }
    }

    public static final class DecodedVoucher {
        private final VoucherType type;
        private final String target;
        private final UUID serial;

        DecodedVoucher(VoucherType type, String target, UUID serial) {
            this.type = type;
            this.target = target;
            this.serial = serial;
        }

        public VoucherType getType() { return type; }
        public String getTarget() { return target; }
        public UUID getSerial() { return serial; }
    }
}
