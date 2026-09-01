package net.skykings.core.plot;

import net.skykings.core.economy.EconomyService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Persistiert gekaufte Plot-Raender und wendet sie nur innerhalb der Claim-Grenze an. */
public final class PlotBorderService {
    private final JavaPlugin plugin;
    private final PlotService plots;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration yaml;

    public PlotBorderService(JavaPlugin plugin, PlotService plots, EconomyService economy) {
        this.plugin = plugin;
        this.plots = plots;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "plot-borders.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasPlot(UUID owner) {
        return plots.hasPlot(owner);
    }

    public PlotBorderTheme selected(UUID owner) {
        return PlotBorderTheme.byId(yaml.getString(path(owner, "selected"), PlotBorderTheme.CLASSIC.getId()));
    }

    public boolean owns(UUID owner, PlotBorderTheme theme) {
        if (theme == PlotBorderTheme.CLASSIC) return true;
        return yaml.getStringList(path(owner, "owned")).contains(theme.getId());
    }

    public boolean purchaseAndSelect(Player player, PlotBorderTheme theme) {
        UUID owner = player.getUniqueId();
        if (!plots.hasPlot(owner)) return false;
        if (!owns(owner, theme)) {
            if (!economy.withdraw(owner, theme.getPrice(), player.getName(), "plot-border:" + theme.getId())) return false;
            Set<String> owned = new HashSet<String>(yaml.getStringList(path(owner, "owned")));
            owned.add(theme.getId());
            yaml.set(path(owner, "owned"), new java.util.ArrayList<String>(owned));
        }
        yaml.set(path(owner, "selected"), theme.getId());
        save();
        apply(owner, theme);
        return true;
    }

    public boolean selectOwned(Player player, PlotBorderTheme theme) {
        UUID owner = player.getUniqueId();
        if (!plots.hasPlot(owner) || !owns(owner, theme)) return false;
        yaml.set(path(owner, "selected"), theme.getId());
        save();
        apply(owner, theme);
        return true;
    }

    @SuppressWarnings("deprecation")
    public void apply(UUID owner, PlotBorderTheme theme) {
        PlotService.PlotData plot = plots.get(owner);
        if (plot == null) return;
        World world = plots.ensureWorld();
        if (world == null) return;
        int minX = plot.getMinX();
        int maxX = plot.getMaxX();
        int minZ = plot.getMinZ();
        int maxZ = plot.getMaxZ();
        Material material = theme.getMaterial();

        // Nur der aeusserste Blockring DER 65x65-Claimflaeche wird veraendert.
        // Die 7 Block breite Stone-Brick-Strasse liegt ausserhalb dieser Grenzen und bleibt neutral.
        for (int x = minX; x <= maxX; x++) {
            world.getBlockAt(x, PlotService.Y - 1, minZ).setType(material, false);
            world.getBlockAt(x, PlotService.Y - 1, maxZ).setType(material, false);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            world.getBlockAt(minX, PlotService.Y - 1, z).setType(material, false);
            world.getBlockAt(maxX, PlotService.Y - 1, z).setType(material, false);
        }
    }

    public long balance(UUID owner) {
        return economy.getBalance(owner);
    }

    private String path(UUID owner, String key) {
        return "players." + owner.toString() + "." + key;
    }

    private void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "plot-borders.yml konnte nicht gespeichert werden.", ex);
        }
    }
}
