package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Stackbare Crates mit Batch-ID, Anti-Dupe-Meta und optionalen Custom-Head-Textures. */
public final class CrateItemCodec {
    private static final String LEGACY_MARKER = ChatColor.BLACK + "skykings:crate:";
    private static final String META_MARKER = ChatColor.BLACK + "#sc";
    private static final String TEXTURE_PREFIX = "texture:";
    private static final int META_CHUNK = 12;
    private final IssuedItemStore issuedStore;

    public CrateItemCodec() { this(IssuedItemStore.active()); }

    public CrateItemCodec(IssuedItemStore issuedStore) {
        this.issuedStore = issuedStore;
    }

    public ItemStack create(CrateRegistry.CrateDefinition crate) { return create(crate, 1); }

    public ItemStack create(CrateRegistry.CrateDefinition crate, int amount) {
        int safeAmount = Math.max(1, Math.min(64, amount));
        UUID serial = UUID.randomUUID();
        if (issuedStore != null && !issuedStore.issueCrate(serial, crate.getId(), safeAmount)) {
            throw new IllegalStateException("Crate-Batch konnte nicht sicher registriert werden");
        }
        ItemStack item = new ItemStack(Material.SKULL_ITEM, safeAmount, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        applyHead(meta, crate.getHeadOwner());
        meta.setDisplayName(color(crate.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Linksklick: " + ChatColor.WHITE + "Rewards ansehen");
        lore.add(ChatColor.GRAY + "Rechtsklick: " + ChatColor.GREEN + "Crate oeffnen");
        lore.add(ChatColor.GRAY + "Shift + Rechtsklick:");
        lore.add(ChatColor.GOLD + "Alle oeffnen " + ChatColor.DARK_GRAY + "(Exile+)");
        addMeta(lore, crate.getId().toLowerCase(Locale.ROOT) + "|" + serial + "|" + safeAmount);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Bukkit 1.8 exposes only player owners. Custom texture values are therefore injected via
     * the CraftMetaSkull GameProfile field without compile-time authlib dependency.
     */
    private void applyHead(SkullMeta meta, String configured) {
        if (meta == null) return;
        String value = configured == null ? "" : configured.trim();
        if (!value.startsWith(TEXTURE_PREFIX)) {
            meta.setOwner(value.isEmpty() ? "MHF_Chest" : value);
            return;
        }
        String texture = value.substring(TEXTURE_PREFIX.length()).trim();
        if (texture.isEmpty() || !applyTexture(meta, texture)) meta.setOwner("MHF_Chest");
    }

    private boolean applyTexture(SkullMeta meta, String textureValue) {
        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Constructor<?> profileCtor = profileClass.getConstructor(UUID.class, String.class);
            Object profile = profileCtor.newInstance(UUID.randomUUID(), "SkyKingsCrate");

            Method getProperties = profileClass.getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> propertyCtor = propertyClass.getConstructor(String.class, String.class);
            Object property = propertyCtor.newInstance("textures", textureValue);
            Method put = properties.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(properties, "textures", property);

            Field profileField = findField(meta.getClass(), "profile");
            if (profileField == null) return false;
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Field findField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null) {
            try { return cursor.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { cursor = cursor.getSuperclass(); }
        }
        return null;
    }

    public DecodedCrate decode(ItemStack item) {
        if (item == null || item.getType() != Material.SKULL_ITEM || item.getDurability() != 3) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        StringBuilder payload = new StringBuilder();
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(META_MARKER)) payload.append(line.substring(META_MARKER.length()));
        }
        if (payload.length() > 0) return decodePayload(payload.toString());

        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(LEGACY_MARKER)) continue;
            String[] parts = line.substring(LEGACY_MARKER.length()).split(":");
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
                return validate(crateId, serial, maxClaims);
            } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

    private DecodedCrate decodePayload(String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) return null;
        try {
            String crateId = parts[0].toLowerCase(Locale.ROOT);
            UUID serial = UUID.fromString(parts[1]);
            int maxClaims = Math.max(1, Math.min(64, Integer.parseInt(parts[2])));
            return validate(crateId, serial, maxClaims);
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private DecodedCrate validate(String crateId, UUID serial, int maxClaims) {
        if (issuedStore != null && !issuedStore.isIssuedCrate(serial, crateId, maxClaims)) return null;
        return new DecodedCrate(crateId, serial, maxClaims);
    }

    private void addMeta(List<String> lore, String payload) {
        for (int start = 0; start < payload.length(); start += META_CHUNK) {
            int end = Math.min(payload.length(), start + META_CHUNK);
            lore.add(META_MARKER + payload.substring(start, end));
        }
    }

    private String color(String raw) { return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw); }

    public static final class DecodedCrate {
        private final String crateId;
        private final UUID serial;
        private final int maxClaims;
        DecodedCrate(String crateId, UUID serial, int maxClaims) { this.crateId=crateId; this.serial=serial; this.maxClaims=maxClaims; }
        public String getCrateId() { return crateId; }
        public UUID getSerial() { return serial; }
        public int getMaxClaims() { return maxClaims; }
    }
}
