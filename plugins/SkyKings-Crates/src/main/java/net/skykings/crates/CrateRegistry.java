package net.skykings.crates;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Laedt Crate- und Reward-Tabellen aus crates.yml und berechnet deren Expected Value. */
public final class CrateRegistry {

    public enum RewardType { COINS, NETHERSTARS, ITEM }

    public static final class RewardDefinition {
        private final String id;
        private final RewardType type;
        private final long amount;
        private final int weight;
        private final long evValue;
        private final Material material;
        private final short data;

        RewardDefinition(String id, RewardType type, long amount, int weight, long evValue,
                         Material material, short data) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.weight = weight;
            this.evValue = evValue;
            this.material = material;
            this.data = data;
        }

        public String getId() { return id; }
        public RewardType getType() { return type; }
        public long getAmount() { return amount; }
        public int getWeight() { return weight; }
        public long getEvValue() { return evValue; }
        public Material getMaterial() { return material; }
        public short getData() { return data; }
    }

    public static final class CrateDefinition {
        private final String id;
        private final String displayName;
        private final String headOwner;
        private final List<RewardDefinition> rewards;
        private final int totalWeight;

        CrateDefinition(String id, String displayName, String headOwner, List<RewardDefinition> rewards) {
            this.id = id;
            this.displayName = displayName;
            this.headOwner = headOwner;
            this.rewards = Collections.unmodifiableList(new ArrayList<RewardDefinition>(rewards));
            int total = 0;
            for (RewardDefinition reward : rewards) total += reward.weight;
            this.totalWeight = total;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getHeadOwner() { return headOwner; }
        public List<RewardDefinition> getRewards() { return rewards; }

        public double getExpectedValue() {
            if (totalWeight <= 0) return 0D;
            double value = 0D;
            for (RewardDefinition reward : rewards) {
                value += ((double) reward.weight / (double) totalWeight) * reward.evValue;
            }
            return value;
        }

        RewardDefinition draw(Random random) {
            if (rewards.isEmpty() || totalWeight <= 0) return null;
            int roll = random.nextInt(totalWeight) + 1;
            int cursor = 0;
            for (RewardDefinition reward : rewards) {
                cursor += reward.weight;
                if (roll <= cursor) return reward;
            }
            return rewards.get(rewards.size() - 1);
        }
    }

    private final Map<String, CrateDefinition> crates = new LinkedHashMap<String, CrateDefinition>();
    private final Random random = new Random();

    public CrateRegistry(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "crates.yml");
        if (!file.exists()) plugin.saveResource("crates.yml", false);
        load(YamlConfiguration.loadConfiguration(file), plugin);
    }

    private void load(YamlConfiguration yaml, JavaPlugin plugin) {
        ConfigurationSection root = yaml.getConfigurationSection("crates");
        if (root == null) return;
        for (String crateId : root.getKeys(false)) {
            ConfigurationSection crateSection = root.getConfigurationSection(crateId);
            if (crateSection == null) continue;
            List<RewardDefinition> rewards = new ArrayList<RewardDefinition>();
            ConfigurationSection rewardSection = crateSection.getConfigurationSection("rewards");
            if (rewardSection != null) {
                for (String rewardId : rewardSection.getKeys(false)) {
                    ConfigurationSection section = rewardSection.getConfigurationSection(rewardId);
                    if (section == null) continue;
                    try {
                        RewardType type = RewardType.valueOf(section.getString("type", "ITEM").toUpperCase(Locale.ROOT));
                        long amount = Math.max(1L, section.getLong("amount", 1L));
                        int weight = Math.max(1, section.getInt("weight", 1));
                        long evValue = Math.max(0L, section.getLong("ev-value", 0L));
                        Material material = null;
                        short data = (short) section.getInt("data", 0);
                        if (type == RewardType.ITEM) {
                            material = Material.matchMaterial(section.getString("material", "STONE"));
                            if (material == null) throw new IllegalArgumentException("Unbekanntes Material");
                        }
                        rewards.add(new RewardDefinition(rewardId, type, amount, weight, evValue, material, data));
                    } catch (RuntimeException ex) {
                        plugin.getLogger().warning("Ungueltiger Reward " + crateId + "." + rewardId + ": " + ex.getMessage());
                    }
                }
            }
            crates.put(crateId.toLowerCase(Locale.ROOT), new CrateDefinition(
                    crateId.toLowerCase(Locale.ROOT), crateSection.getString("display-name", crateId),
                    crateSection.getString("head-owner", "MHF_Chest"), rewards));
        }
        plugin.getLogger().info("Crates registriert: " + crates.size());
    }

    public CrateDefinition get(String id) {
        return id == null ? null : crates.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<CrateDefinition> getAll() {
        return Collections.unmodifiableCollection(crates.values());
    }

    public RewardDefinition draw(CrateDefinition crate) {
        return crate == null ? null : crate.draw(random);
    }
}
