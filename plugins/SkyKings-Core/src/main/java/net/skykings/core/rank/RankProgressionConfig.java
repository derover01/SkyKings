package net.skykings.core.rank;

import net.skykings.core.model.Rank;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/** Laedt die Coin-Kosten fuer die vier kaufbaren Free-Rank-Aufstiege. */
public final class RankProgressionConfig {

    private final Map<Rank, Long> costsByTargetRank = new EnumMap<>(Rank.class);

    public RankProgressionConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "rank-progression.yml");
        if (!file.exists()) {
            plugin.saveResource("rank-progression.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadCost(config, Rank.IRON);
        loadCost(config, Rank.GOLD);
        loadCost(config, Rank.EPIC);
        loadCost(config, Rank.DIAMOND);
    }

    private void loadCost(YamlConfiguration config, Rank target) {
        String path = "free-rank-costs." + target.name().toLowerCase();
        long cost = config.getLong(path, -1L);
        if (cost <= 0L) {
            throw new IllegalStateException("Ungueltiger Rankup-Preis in rank-progression.yml: " + path + "=" + cost);
        }
        costsByTargetRank.put(target, cost);
    }

    public long getCost(Rank targetRank) {
        Long cost = costsByTargetRank.get(targetRank);
        if (cost == null) {
            throw new IllegalArgumentException("Kein Free-Rankup-Preis fuer " + targetRank);
        }
        return cost;
    }
}
