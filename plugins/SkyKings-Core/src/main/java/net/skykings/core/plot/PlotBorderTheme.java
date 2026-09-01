package net.skykings.core.plot;

import org.bukkit.Material;

/** Kaufbare Bodenrand-Cosmetics fuer den aeussersten Blockring eines Plots. */
public enum PlotBorderTheme {
    CLASSIC("classic", "Klassisch", Material.GRASS, 0L),
    STONE("stone", "Stone Brick", Material.SMOOTH_BRICK, 25000L),
    SANDSTONE("sandstone", "Sandstein", Material.SANDSTONE, 50000L),
    NETHER("nether", "Nether Brick", Material.NETHER_BRICK, 75000L),
    QUARTZ("quartz", "Quarz", Material.QUARTZ_BLOCK, 100000L),
    PRISMARINE("prismarine", "Prismarin", Material.PRISMARINE, 150000L),
    OBSIDIAN("obsidian", "Obsidian", Material.OBSIDIAN, 250000L),
    GOLD("gold", "Gold", Material.GOLD_BLOCK, 500000L),
    DIAMOND("diamond", "Diamant", Material.DIAMOND_BLOCK, 750000L);

    private final String id;
    private final String displayName;
    private final Material material;
    private final long price;

    PlotBorderTheme(String id, String displayName, Material material, long price) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.price = price;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public long getPrice() { return price; }

    public static PlotBorderTheme byId(String raw) {
        if (raw == null) return CLASSIC;
        for (PlotBorderTheme theme : values()) if (theme.id.equalsIgnoreCase(raw)) return theme;
        return CLASSIC;
    }
}
