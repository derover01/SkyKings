package net.skykings.core.shop.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection-only 1.8.x bridge for the real vanilla villager trade screen.
 * Keeps SkyKings compiling against spigot-api while runtime uses CraftBukkit/NMS v1_8_R3.
 */
final class LegacyVillagerTradeBridge {
    private static final String OFFER_MARKER = ChatColor.DARK_GRAY + "SK-Angebot #";

    boolean configureAndOpen(Player player, Villager villager, PlayerShop shop) {
        if (player == null || villager == null || shop == null) return false;
        String stage = "handles";
        try {
            Object playerHandle = getHandle(player);
            Object villagerHandle = getHandle(villager);

            stage = "villager offers";
            Object offers = getOffers(villagerHandle, playerHandle);
            if (!(offers instanceof List)) return fail(stage, "getOffers lieferte keine MerchantRecipe-Liste", null);

            @SuppressWarnings("unchecked")
            List<Object> recipes = (List<Object>) offers;
            recipes.clear();

            stage = "NMS recipe classes";
            Class<?> nmsItemStackClass = Class.forName("net.minecraft.server.v1_8_R3.ItemStack");
            Class<?> merchantRecipeClass = Class.forName("net.minecraft.server.v1_8_R3.MerchantRecipe");
            Constructor<?> recipeConstructor = findRecipeConstructor(merchantRecipeClass, nmsItemStackClass);
            if (recipeConstructor == null) return fail(stage, "passender MerchantRecipe(ItemStack, ItemStack)-Konstruktor fehlt", null);

            stage = "recipe conversion";
            for (int index = 0; index < PlayerShop.MAX_OFFERS; index++) {
                PlayerShopOffer offer = shop.getOffer(index);
                if (offer == null || !offer.isConfigured()) continue;
                ItemStack token = virtualCoinToken();
                ItemStack preview = previewItem(offer, index);
                Object nmsToken = asNmsCopy(token);
                Object nmsPreview = asNmsCopy(preview);
                recipes.add(recipeConstructor.newInstance(nmsToken, nmsPreview));
            }

            if (recipes.isEmpty()) return fail("recipe conversion", "keine konfigurierten Angebote nach NMS-Konvertierung", null);

            stage = "openTrade(IMerchant)";
            Method openTrade = findOpenTrade(playerHandle.getClass(), villagerHandle.getClass());
            if (openTrade == null) return fail(stage, "EntityHuman#openTrade(IMerchant) konnte nicht gefunden werden", null);
            openTrade.setAccessible(true);
            openTrade.invoke(playerHandle, villagerHandle);
            PlayerShopTradeSnapshotRegistry.open(player.getUniqueId(), shop);
            return true;
        } catch (Exception ex) {
            PlayerShopTradeSnapshotRegistry.close(player.getUniqueId());
            return fail(stage, "Reflection-Aufruf fehlgeschlagen fuer Spieler " + player.getName(), ex);
        }
    }

    private boolean fail(String stage, String message, Throwable error) {
        Logger logger = logger();
        String full = "PlayerShop Legacy-Merchant-Bridge [" + stage + "]: " + message
                + ". Erwartete Runtime: CraftBukkit/Spigot v1_8_R3.";
        if (error == null) logger.severe(full);
        else logger.log(Level.SEVERE, full, error);
        return false;
    }

    private Logger logger() {
        try {
            return JavaPlugin.getProvidingPlugin(LegacyVillagerTradeBridge.class).getLogger();
        } catch (Throwable ignored) {
            return Logger.getLogger("SkyKings-Core");
        }
    }

    ItemStack virtualCoinToken() {
        ItemStack token = new ItemStack(Material.GOLD_NUGGET, 1);
        ItemMeta meta = token.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "SkyKings Coins");
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Virtuelle Preisfreigabe");
            lore.add(ChatColor.DARK_GRAY + "Nicht entnehmbar");
            meta.setLore(lore);
            token.setItemMeta(meta);
        }
        return token;
    }

    int offerIndex(ItemStack preview) {
        if (preview == null || preview.getType() == Material.AIR || !preview.hasItemMeta()) return -1;
        ItemMeta meta = preview.getItemMeta();
        if (meta == null || !meta.hasLore()) return -1;
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(OFFER_MARKER)) {
                try {
                    return Integer.parseInt(line.substring(OFFER_MARKER.length()).trim()) - 1;
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private ItemStack previewItem(PlayerShopOffer offer, int index) {
        ItemStack preview = offer.topStack();
        if (preview == null) preview = new ItemStack(offer.getMaterial(), 1, offer.getData());
        preview = preview.clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
            if (offer.getAmountMiddle() > 0) {
                lore.add(ChatColor.GRAY + "+ weiterer Stack: " + ChatColor.WHITE + offer.getAmountMiddle() + "x " + pretty(offer.getMaterial()));
            }
            lore.add(ChatColor.GOLD + "Preis: " + ChatColor.WHITE + format(offer.getPriceCoins()) + " Coins");
            lore.add(ChatColor.GREEN + "Klicken zum Kaufen");
            lore.add(OFFER_MARKER + (index + 1));
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    private Object getHandle(Object craftEntity) throws Exception {
        Method getHandle = craftEntity.getClass().getMethod("getHandle");
        return getHandle.invoke(craftEntity);
    }

    private Object getOffers(Object villagerHandle, Object playerHandle) throws Exception {
        for (Method method : villagerHandle.getClass().getMethods()) {
            if (!"getOffers".equals(method.getName()) || method.getParameterTypes().length != 1) continue;
            if (method.getParameterTypes()[0].isAssignableFrom(playerHandle.getClass())) return method.invoke(villagerHandle, playerHandle);
        }
        for (Method method : villagerHandle.getClass().getDeclaredMethods()) {
            if (!"getOffers".equals(method.getName()) || method.getParameterTypes().length != 1) continue;
            method.setAccessible(true);
            return method.invoke(villagerHandle, playerHandle);
        }
        return null;
    }

    private Object asNmsCopy(ItemStack item) throws Exception {
        Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack");
        Method method = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
        return method.invoke(null, item);
    }

    private Constructor<?> findRecipeConstructor(Class<?> recipeClass, Class<?> nmsItemStackClass) {
        for (Constructor<?> constructor : recipeClass.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 2 && types[0] == nmsItemStackClass && types[1] == nmsItemStackClass) return constructor;
        }
        return null;
    }

    private Method findOpenTrade(Class<?> playerClass, Class<?> villagerClass) {
        Class<?> type = playerClass;
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (!"openTrade".equals(method.getName()) || method.getParameterTypes().length != 1) continue;
                if (method.getParameterTypes()[0].isAssignableFrom(villagerClass)) return method;
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }

    private String pretty(Material material) {
        return material == null ? "Item" : material.name().toLowerCase().replace('_', ' ');
    }
}
