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

    public enum VoucherType { RANK, KIT, PERMISSION, PREFIX }

    private static final String MARKER = ChatColor.BLACK + "skykings:voucher:";

    public ItemStack create(VoucherType type, String target, String displayTarget) {
        String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                displayTarget == null ? target : displayTarget));
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorFor(type) + ChatColor.BOLD.toString() + nameFor(type) + " " + cleanTarget);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Belohnung: " + ChatColor.WHITE + cleanTarget);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Rechtsklick zum Einlösen");
        lore.add(ChatColor.DARK_GRAY + "Einmalig • SkyKings Gutschein");
        lore.add(MARKER + type.name().toLowerCase(Locale.ROOT) + ":" + sanitize(target) + ":" + UUID.randomUUID());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public DecodedVoucher decode(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(MARKER)) continue;
            String[] parts = line.substring(MARKER.length()).split(":", 3);
            if (parts.length != 3) return null;
            try {
                VoucherType type = VoucherType.valueOf(parts[0].toUpperCase(Locale.ROOT));
                return new DecodedVoucher(type, parts[1], UUID.fromString(parts[2]));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
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
            default: return "Gutschein";
        }
    }

    private ChatColor colorFor(VoucherType type) {
        switch (type) {
            case RANK: return ChatColor.AQUA;
            case KIT: return ChatColor.GREEN;
            case PERMISSION: return ChatColor.LIGHT_PURPLE;
            case PREFIX: return ChatColor.YELLOW;
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
