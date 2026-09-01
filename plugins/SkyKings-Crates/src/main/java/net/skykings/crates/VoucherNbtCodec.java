package net.skykings.crates;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Speichert technische Gutschein-Metadaten in echtem Item-NBT, ohne sie Spielern im Tooltip zu zeigen.
 * Nutzt Reflection, damit das Modul weiterhin nur gegen spigot-api kompiliert und keine feste v1_8_R3-
 * Compile-Abhaengigkeit braucht.
 */
final class VoucherNbtCodec {

    private static final String KEY = "SkyKingsVoucher";

    private VoucherNbtCodec() { }

    static ItemStack write(ItemStack source, String payload) {
        if (source == null || payload == null || payload.isEmpty()) return null;
        try {
            Reflection reflection = Reflection.resolve();
            Object nmsItem = reflection.asNmsCopy.invoke(null, source);
            Object tag = reflection.getTag.invoke(nmsItem);
            if (tag == null) tag = reflection.tagConstructor.newInstance();
            reflection.setString.invoke(tag, KEY, payload);
            reflection.setTag.invoke(nmsItem, tag);
            return (ItemStack) reflection.asBukkitCopy.invoke(null, nmsItem);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String read(ItemStack source) {
        if (source == null) return null;
        try {
            Reflection reflection = Reflection.resolve();
            Object nmsItem = reflection.asNmsCopy.invoke(null, source);
            Object tag = reflection.getTag.invoke(nmsItem);
            if (tag == null) return null;
            String payload = (String) reflection.getString.invoke(tag, KEY);
            return payload == null || payload.isEmpty() ? null : payload;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class Reflection {
        private static Reflection cached;

        private final Method asNmsCopy;
        private final Method asBukkitCopy;
        private final Method getTag;
        private final Method setTag;
        private final Constructor<?> tagConstructor;
        private final Method setString;
        private final Method getString;

        private Reflection(Method asNmsCopy, Method asBukkitCopy, Method getTag, Method setTag,
                           Constructor<?> tagConstructor, Method setString, Method getString) {
            this.asNmsCopy = asNmsCopy;
            this.asBukkitCopy = asBukkitCopy;
            this.getTag = getTag;
            this.setTag = setTag;
            this.tagConstructor = tagConstructor;
            this.setString = setString;
            this.getString = getString;
        }

        static synchronized Reflection resolve() throws Exception {
            if (cached != null) return cached;
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
            Class<?> nbtTagCompound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");

            Method asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Method asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack);
            Method getTag = nmsItemStack.getMethod("getTag");
            Method setTag = nmsItemStack.getMethod("setTag", nbtTagCompound);
            Constructor<?> tagConstructor = nbtTagCompound.getConstructor();
            Method setString = nbtTagCompound.getMethod("setString", String.class, String.class);
            Method getString = nbtTagCompound.getMethod("getString", String.class);

            cached = new Reflection(asNmsCopy, asBukkitCopy, getTag, setTag,
                    tagConstructor, setString, getString);
            return cached;
        }
    }
}
