package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Kodiert Gutschein-Typ, Ziel und eindeutige Seriennummer persistent und spielerunsichtbar im Item. */
public final class VoucherItemCodec {

    public enum VoucherType { RANK, KIT, PERMISSION, PREFIX, COINS, GIVEALL_COINS }

    /** Legacy-Marker bleibt lesbar, damit bereits ausgegebene Testgutscheine dekodiert werden koennen. */
    private static final String LEGACY_MARKER = ChatColor.BLACK + "skykings:voucher:";
    /** Fallback fuer Umgebungen ohne CraftBukkit-NBT (z. B. isolierte Unit-Tests). */
    private static final String META_MARKER = ChatColor.BLACK + "#sv";
    private static final int META_CHUNK = 12;

    private final IssuedItemStore issuedStore;

    /** Nutzt im laufenden Plugin automatisch das aktive Issued-Registry; in isolierten Tests null. */
    public VoucherItemCodec() { this(IssuedItemStore.active()); }

    public VoucherItemCodec(IssuedItemStore issuedStore) {
        this.issuedStore = issuedStore;
    }

    public ItemStack create(VoucherType type, String target, String displayTarget) {
        String cleanTarget = cleanDisplay(target, displayTarget);
        UUID serial = UUID.randomUUID();
        String sanitizedTarget = sanitize(target);
        if (issuedStore != null && !issuedStore.issueVoucher(serial, type, sanitizedTarget)) {
            throw new IllegalStateException("Voucher-Serial konnte nicht sicher registriert werden");
        }
        ItemStack item = baseItem(type, cleanTarget, true);
        String payload = type.name().toLowerCase(Locale.ROOT) + "|" + sanitizedTarget + "|" + serial;

        // Auf dem echten Spigot-Server: technische Daten ausschliesslich im NBT, nicht im Tooltip.
        ItemStack nbtItem = VoucherNbtCodec.write(item, payload);
        if (nbtItem != null) return nbtItem;

        // Nur Fallback fuer Test-Doubles/ungewoehnliche Server ohne CraftBukkit-NBT-Zugriff.
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<String>(meta.getLore());
        addMeta(lore, payload);
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
        meta.setDisplayName(colorFor(type) + ChatColor.BOLD.toString() + nameFor(type)
                + ChatColor.DARK_GRAY + " • " + colorFor(type) + shortTarget(type, cleanTarget));
        List<String> lore = new ArrayList<String>();
        if (type == VoucherType.COINS) {
            lore.add(ChatColor.GRAY + "Wert: " + ChatColor.GOLD + cleanTarget);
            lore.add(ChatColor.DARK_GRAY + "Nur fuer dich.");
        } else if (type == VoucherType.GIVEALL_COINS) {
            lore.add(ChatColor.GRAY + "Pro Spieler: " + ChatColor.GOLD + cleanTarget);
            lore.add(ChatColor.YELLOW + "Fuer alle Online-Spieler.");
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

        // Neue Gutscheine: echte interne Item-Daten, komplett ausserhalb der sichtbaren Lore.
        String nbtPayload = VoucherNbtCodec.read(item);
        if (nbtPayload != null) return decodePayload(nbtPayload);

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        // Rueckwaertskompatibilitaet fuer den kurzen Lore-Marker der vorherigen Version.
        StringBuilder payload = new StringBuilder();
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(META_MARKER)) payload.append(line.substring(META_MARKER.length()));
        }
        if (payload.length() > 0) return decodePayload(payload.toString());

        // Rueckwaertskompatibilitaet fuer ganz alte Testitems.
        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(LEGACY_MARKER)) continue;
            String[] parts = line.substring(LEGACY_MARKER.length()).split(":", 3);
            if (parts.length != 3) return null;
            return decoded(parts[0], parts[1], parts[2]);
        }
        return null;
    }

    private DecodedVoucher decodePayload(String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) return null;
        return decoded(parts[0], parts[1], parts[2]);
    }

    private DecodedVoucher decoded(String rawType, String target, String rawSerial) {
        try {
            VoucherType type = VoucherType.valueOf(rawType.toUpperCase(Locale.ROOT));
            UUID serial = UUID.fromString(rawSerial);
            if (issuedStore != null && !issuedStore.isIssuedVoucher(serial, type, target)) return null;
            return new DecodedVoucher(type, target, serial);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void addMeta(List<String> lore, String payload) {
        for (int start = 0; start < payload.length(); start += META_CHUNK) {
            int end = Math.min(payload.length(), start + META_CHUNK);
            lore.add(META_MARKER + payload.substring(start, end));
        }
    }

    private String cleanDisplay(String target, String displayTarget) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                displayTarget == null ? target : displayTarget));
    }

    private String shortTarget(VoucherType type, String cleanTarget) {
        String value = cleanTarget == null ? "" : cleanTarget.trim();
        if (type == VoucherType.RANK) value = stripSuffix(value, " Rang");
        if (type == VoucherType.KIT) value = stripSuffix(value, " Kit");
        if (type == VoucherType.PERMISSION) value = stripSuffix(value, " Recht");
        if (type == VoucherType.PREFIX) value = stripSuffix(value, " Prefix");
        if (type == VoucherType.COINS || type == VoucherType.GIVEALL_COINS) value = stripSuffix(value, " Coins");
        if (value.length() > 22) value = value.substring(0, 22);
        return value;
    }

    private String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
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
            case GIVEALL_COINS: return "GiveAll-Gutschein";
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
