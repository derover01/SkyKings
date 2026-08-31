package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Stackbare Crates mit Batch-ID und maximal erlaubter Einlösungsanzahl. */
public final class CrateItemCodec {
    private static final String MARKER_PREFIX = ChatColor.BLACK + "skykings:crate:";
    private final IssuedItemStore issuedStore;

    /** Nutzt im laufenden Plugin automatisch das aktive Issued-Registry; in isolierten Tests null. */
    public CrateItemCodec() { this(IssuedItemStore.active()); }

    public CrateItemCodec(IssuedItemStore issuedStore) {
        this.issuedStore = issuedStore;
    }

    public ItemStack create(CrateRegistry.CrateDefinition crate) {
        return create(crate, 1);
    }

    public ItemStack create(CrateRegistry.CrateDefinition crate, int amount) {
        int safeAmount = Math.max(1, Math.min(64, amount));
        UUID serial = UUID.randomUUID();
        if (issuedStore != null && !issuedStore.issueCrate(serial, crate.getId(), safeAmount)) {
            throw new IllegalStateException("Crate-Batch konnte nicht sicher registriert werden");
        }
        ItemStack item = new ItemStack(Material.SKULL_ITEM, safeAmount, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(crate.getHeadOwner());
        meta.setDisplayName(color(crate.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Linksklick: " + ChatColor.WHITE + "Rewards ansehen");
        lore.add(ChatColor.GRAY + "Rechtsklick: " + ChatColor.GREEN + "Crate öffnen");
        lore.add(ChatColor.GRAY + "Shift + Rechtsklick: " + ChatColor.GOLD + "Alle öffnen " + ChatColor.DARK_GRAY + "(Exile+)");
        lore.add(encode(crate.getId(), serial, safeAmount));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public DecodedCrate decode(ItemStack item) {
        if (item == null || item.getType() != Material.SKULL_ITEM || item.getDurability() != 3) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(MARKER_PREFIX)) continue;
            String[] parts = line.substring(MARKER_PREFIX.length()).split(":");
            try {
                final String crateId;
                final UUID serial;
                final int maxClaims;
                if (parts.length == 2) {
                    crateId = parts[0].toLowerCase(Locale.ROOT);
                    serial = UUID.fromString(parts[1]);
                    maxClaims = 1;
                } else if (parts.length == 3) {
                    crateId = parts[0].toLowerCase(Locale.ROOT);
                    serial = UUID.fromString(parts[1]);
                    maxClaims = Math.max(1, Math.min(64, Integer.parseInt(parts[2])));
                } else return null;
                if (issuedStore != null && !issuedStore.isIssuedCrate(serial, crateId, maxClaims)) return null;
                return new DecodedCrate(crateId, serial, maxClaims);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String encode(String crateId, UUID serial, int maxClaims) {
        return MARKER_PREFIX + crateId.toLowerCase(Locale.ROOT) + ":" + serial + ":" + maxClaims;
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    public static final class DecodedCrate {
        private final String crateId;
        private final UUID serial;
        private final int maxClaims;

        DecodedCrate(String crateId, UUID serial, int maxClaims) {
            this.crateId = crateId;
            this.serial = serial;
            this.maxClaims = maxClaims;
        }

        public String getCrateId() { return crateId; }
        public UUID getSerial() { return serial; }
        public int getMaxClaims() { return maxClaims; }
    }
}
