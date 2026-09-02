package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Normale Crates sind identisch pro Typ und damit voll stackbar; alte Batch-Crates bleiben lesbar. */
public final class CrateItemCodec {
    private static final String STACK_MARKER = ChatColor.BLACK + "skykings:crate-stack:";
    private static final String LEGACY_MARKER = ChatColor.BLACK + "skykings:crate:";
    private static final String META_MARKER = ChatColor.BLACK + "#sc";
    private static final String TEXTURE_PREFIX = "texture:";
    private final IssuedItemStore issuedStore;

    public CrateItemCodec() { this(IssuedItemStore.active()); }

    public CrateItemCodec(IssuedItemStore issuedStore) {
        this.issuedStore = issuedStore;
    }

    public ItemStack create(CrateRegistry.CrateDefinition crate) { return create(crate, 1); }

    public ItemStack create(CrateRegistry.CrateDefinition crate, int amount) {
        int safeAmount = Math.max(1, Math.min(64, amount));
        ItemStack item = new ItemStack(Material.SKULL_ITEM, safeAmount, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        applyHead(meta, resolvedHead(crate));
        meta.setDisplayName(color(crate.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Linksklick: " + ChatColor.WHITE + "Rewards ansehen");
        lore.add(ChatColor.GRAY + "Rechtsklick: " + ChatColor.GREEN + "Crate oeffnen");
        lore.add(ChatColor.GRAY + "Shift + Rechtsklick:");
        lore.add(ChatColor.GOLD + "Alle oeffnen " + ChatColor.DARK_GRAY + "(Exile+)");
        lore.add(STACK_MARKER + crate.getId().toLowerCase(Locale.ROOT));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Alte Configs hatten fuer Vote/Common/Rare/Epic/Legendary/Royal/King alle MHF_Chest.
     * Solange dort noch der Default steht, erzwingen wir pro Tier einen eigenen stabilen Kopf.
     * Explizite texture:-Werte (z. B. Build/Fight/Money/Utility/Event) bleiben unangetastet.
     */
    private String resolvedHead(CrateRegistry.CrateDefinition crate) {
        String configured = crate.getHeadOwner() == null ? "" : crate.getHeadOwner().trim();
        if (!configured.isEmpty() && !"MHF_Chest".equalsIgnoreCase(configured)) return configured;
        String id = crate.getId() == null ? "" : crate.getId().toLowerCase(Locale.ROOT);
        if ("vote".equals(id)) return "MHF_Question";
        if ("common".equals(id)) return "MHF_Chicken";
        if ("rare".equals(id)) return "MHF_Enderman";
        if ("epic".equals(id)) return "MHF_Blaze";
        if ("legendary".equals(id)) return "MHF_Golem";
        if ("royal".equals(id)) return "MHF_Wither";
        if ("king".equals(id)) return "MHF_Dragon";
        return configured.isEmpty() ? "MHF_Chest" : configured;
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
            UUID profileId = UUID.nameUUIDFromBytes(("SkyKingsCrate:" + textureValue).getBytes(StandardCharsets.UTF_8));
            Object profile = profileCtor.newInstance(profileId, "SkyKingsCrate");

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

        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(STACK_MARKER)) {
                String crateId = line.substring(STACK_MARKER.length()).trim().toLowerCase(Locale.ROOT);
                if (crateId.isEmpty()) return null;
                return new DecodedCrate(crateId, null, 1);
            }
        }

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
                return validateLegacy(crateId, serial, maxClaims);
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
            return validateLegacy(crateId, serial, maxClaims);
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private DecodedCrate validateLegacy(String crateId, UUID serial, int maxClaims) {
        if (issuedStore != null && !issuedStore.isIssuedCrate(serial, crateId, maxClaims)) return null;
        return new DecodedCrate(crateId, serial, maxClaims);
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
        public boolean isLegacySerial() { return serial != null; }
    }
}
