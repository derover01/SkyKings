package net.skykings.core.kit;

import net.skykings.core.item.ItemBuilder;
import net.skykings.core.model.Rank;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Laedt die 11 Rank-Kits aus rank-kits.yml und registriert sie in der zentralen KitRegistry. */
public final class RankKitLoader {

    private final JavaPlugin plugin;
    private final KitRegistry registry;

    public RankKitLoader(JavaPlugin plugin, KitRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void loadAndRegister() {
        File file = new File(plugin.getDataFolder(), "rank-kits.yml");
        if (!file.exists()) plugin.saveResource("rank-kits.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int registered = 0;
        for (Rank rank : Rank.values()) {
            String id = rank.name().toLowerCase(Locale.ROOT);
            ConfigurationSection section = config.getConfigurationSection("kits." + id);
            if (section == null) {
                plugin.getLogger().warning("Rank-Kit fehlt in rank-kits.yml: " + id);
                continue;
            }
            try {
                registry.register(buildKit(id, rank, section));
                registered++;
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Rank-Kit konnte nicht geladen werden: " + id, e);
            }
        }
        plugin.getLogger().info("Rank-Kits registriert: " + registered + "/" + Rank.values().length);
    }

    private KitDefinition buildKit(String id, Rank rank, ConfigurationSection section) {
        final KitSpec spec = new KitSpec();
        spec.armorPrefix = section.getString("gear.armor", "IRON").toUpperCase(Locale.ROOT);
        spec.protection = section.getInt("gear.protection", 0);
        spec.unbreaking = section.getInt("gear.unbreaking", 0);
        spec.swordMaterial = material(section.getString("gear.sword", "IRON_SWORD"));
        spec.sharpness = section.getInt("gear.sharpness", 0);
        spec.fireAspect = section.getInt("gear.fire-aspect", 0);
        spec.bowPower = nonNegative(section.getInt("gear.bow-power", 1), "bow-power");
        spec.bowPunch = nonNegative(section.getInt("gear.bow-punch", 0), "bow-punch");
        spec.bowFlame = nonNegative(section.getInt("gear.bow-flame", 0), "bow-flame");
        spec.goldenApples = nonNegative(section.getInt("consumables.golden-apples", 0), "golden-apples");
        spec.opApples = nonNegative(section.getInt("consumables.op-apples", 0), "op-apples");
        spec.enderPearls = nonNegative(section.getInt("consumables.enderpearls", 0), "enderpearls");
        spec.arrows = nonNegative(section.getInt("consumables.arrows", 32), "arrows");

        long cooldownMinutes = Math.max(0L, section.getLong("cooldown-minutes", 0L));
        int strengthTicks = effectTicks(section.getLong("effects.strength-seconds", 0L));
        int speedTicks = effectTicks(section.getLong("effects.speed-seconds", 0L));

        KitDefinition.Builder builder = KitDefinition.builder(id)
                .displayName(section.getString("display-name", rank.name()))
                .requiredRank(rank)
                .cooldownMillis(TimeUnit.MINUTES.toMillis(cooldownMinutes))
                .itemFactory(() -> createItems(spec));
        if (strengthTicks > 0) builder.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, strengthTicks, 1));
        if (speedTicks > 0) builder.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedTicks, 1));
        return builder.build();
    }

    private List<ItemStack> createItems(KitSpec spec) {
        List<ItemStack> items = new ArrayList<>();
        items.add(armor(spec.armorPrefix + "_HELMET", spec.protection, spec.unbreaking));
        items.add(armor(spec.armorPrefix + "_CHESTPLATE", spec.protection, spec.unbreaking));
        items.add(armor(spec.armorPrefix + "_LEGGINGS", spec.protection, spec.unbreaking));
        items.add(armor(spec.armorPrefix + "_BOOTS", spec.protection, spec.unbreaking));

        ItemBuilder sword = new ItemBuilder(spec.swordMaterial);
        if (spec.sharpness > 0) sword.enchant(Enchantment.DAMAGE_ALL, spec.sharpness);
        if (spec.fireAspect > 0) sword.enchant(Enchantment.FIRE_ASPECT, spec.fireAspect);
        if (spec.unbreaking > 0) sword.enchant(Enchantment.DURABILITY, spec.unbreaking);
        items.add(sword.build());

        ItemBuilder bow = new ItemBuilder(Material.BOW);
        if (spec.bowPower > 0) bow.enchant(Enchantment.ARROW_DAMAGE, spec.bowPower);
        if (spec.bowPunch > 0) bow.enchant(Enchantment.ARROW_KNOCKBACK, spec.bowPunch);
        if (spec.bowFlame > 0) bow.enchant(Enchantment.ARROW_FIRE, spec.bowFlame);
        if (spec.unbreaking > 0) bow.enchant(Enchantment.DURABILITY, spec.unbreaking);
        items.add(bow.build());

        addStacked(items, Material.ARROW, spec.arrows, (short) 0);
        addStacked(items, Material.GOLDEN_APPLE, spec.goldenApples, (short) 0);
        addStacked(items, Material.GOLDEN_APPLE, spec.opApples, (short) 1);
        addStacked(items, Material.ENDER_PEARL, spec.enderPearls, (short) 0);
        return items;
    }

    private ItemStack armor(String materialName, int protection, int unbreaking) {
        ItemBuilder builder = new ItemBuilder(material(materialName));
        if (protection > 0) builder.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, protection);
        if (unbreaking > 0) builder.enchant(Enchantment.DURABILITY, unbreaking);
        return builder.build();
    }

    private void addStacked(List<ItemStack> items, Material material, int totalAmount, short durability) {
        int remaining = totalAmount;
        int maxStack = Math.max(1, material.getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(maxStack, remaining);
            items.add(new ItemBuilder(material, amount).durability(durability).build());
            remaining -= amount;
        }
    }

    private Material material(String name) {
        try { return Material.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unbekanntes 1.8.8-Material in rank-kits.yml: " + name, e);
        }
    }

    private int effectTicks(long seconds) {
        if (seconds <= 0L) return 0;
        return (int) Math.min(Integer.MAX_VALUE, seconds * 20L);
    }

    private int nonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " darf nicht negativ sein: " + value);
        return value;
    }

    private static final class KitSpec {
        private String armorPrefix;
        private int protection;
        private int unbreaking;
        private Material swordMaterial;
        private int sharpness;
        private int fireAspect;
        private int bowPower;
        private int bowPunch;
        private int bowFlame;
        private int arrows;
        private int goldenApples;
        private int opApples;
        private int enderPearls;
    }
}
