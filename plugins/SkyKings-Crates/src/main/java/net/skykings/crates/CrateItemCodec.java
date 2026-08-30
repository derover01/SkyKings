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

public final class CrateItemCodec {
    private static final String MARKER_PREFIX = ChatColor.BLACK + "skykings:crate:";

    public ItemStack create(CrateRegistry.CrateDefinition crate) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(crate.getHeadOwner());
        meta.setDisplayName(color(crate.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Linksklick: " + ChatColor.WHITE + "Rewards ansehen");
        lore.add(ChatColor.GRAY + "Rechtsklick: " + ChatColor.GREEN + "Crate oeffnen");
        lore.add(ChatColor.GRAY + "Shift + Rechtsklick: " + ChatColor.GOLD + "Alle oeffnen " + ChatColor.DARK_GRAY + "(Exile+)");
        lore.add(encode(crate.getId(), UUID.randomUUID()));
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
            String[] parts = line.substring(MARKER_PREFIX.length()).split(":", 2);
            if (parts.length != 2) return null;
            try {
                return new DecodedCrate(parts[0].toLowerCase(Locale.ROOT), UUID.fromString(parts[1]));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String encode(String crateId, UUID serial) {
        return MARKER_PREFIX + crateId.toLowerCase(Locale.ROOT) + ":" + serial.toString();
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    public static final class DecodedCrate {
        private final String crateId;
        private final UUID serial;

        DecodedCrate(String crateId, UUID serial) {
            this.crateId = crateId;
            this.serial = serial;
        }

        public String getCrateId() { return crateId; }
        public UUID getSerial() { return serial; }
    }
}
