import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Splits the committed SkyKings UI atlas into Minecraft 1.8.x legacy item textures.
 * Uses only Java 8/JRE classes so the same build works on Windows and GitHub Actions.
 */
public final class ResourcePackAtlasBuilder {
    private static final int TILE = 32;
    private static final int COLS = 5;
    private static final int ROWS = 4;

    // Row-major order. Entry 19 is used as pack.png instead of an item texture.
    private static final String[] ITEM_TEXTURES = {
            "minecart_normal.png",       // HOME
            "minecart_furnace.png",      // BACK
            "minecart_hopper.png",       // NEXT
            "barrier.png",               // LOCKED
            "slimeball.png",             // READY
            "fireworks.png",             // COMPLETED
            "ender_eye.png",             // PREMIUM
            "gold_nugget.png",           // COINS
            "nether_star.png",           // STAR
            "map_empty.png",             // BATTLE PASS
            "book_writable.png",         // QUESTS
            "minecart_chest.png",        // KITS
            "minecart_command_block.png",// CRATES
            "repeater.png",              // JACKPOT
            "hopper.png",                // SHOP
            "name_tag.png",              // TRADE
            "book_written.png",          // CLAN
            "shears.png",                // DUEL
            "magma_cream.png"             // EVENT
    };

    private ResourcePackAtlasBuilder() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ResourcePackAtlasBuilder <atlas.png> <stage-root>");
        }

        File atlasFile = new File(args[0]);
        File stageRoot = new File(args[1]);
        BufferedImage atlas = ImageIO.read(atlasFile);
        if (atlas == null) throw new IOException("Atlas is not a readable PNG: " + atlasFile);
        if (atlas.getWidth() != TILE * COLS || atlas.getHeight() != TILE * ROWS) {
            throw new IOException("Unexpected atlas size " + atlas.getWidth() + "x" + atlas.getHeight()
                    + "; expected " + (TILE * COLS) + "x" + (TILE * ROWS));
        }

        File itemDir = new File(stageRoot, "assets/minecraft/textures/items");
        if (!itemDir.exists() && !itemDir.mkdirs()) {
            throw new IOException("Could not create item texture directory: " + itemDir);
        }

        for (int i = 0; i < ITEM_TEXTURES.length; i++) {
            BufferedImage tile = crop(atlas, i);
            File output = new File(itemDir, ITEM_TEXTURES[i]);
            if (!ImageIO.write(tile, "png", output)) {
                throw new IOException("Could not write PNG: " + output);
            }
        }

        // Last tile is the SkyKings crest. Keep it larger than an inventory icon for the pack selector.
        BufferedImage logo = crop(atlas, 19);
        BufferedImage packIcon = resizeNearest(logo, 128, 128);
        File packPng = new File(stageRoot, "pack.png");
        if (!ImageIO.write(packIcon, "png", packPng)) {
            throw new IOException("Could not write pack.png");
        }

        System.out.println("[OK] Generated " + ITEM_TEXTURES.length + " SkyKings item textures + pack.png");
    }

    private static BufferedImage crop(BufferedImage atlas, int index) {
        int x = (index % COLS) * TILE;
        int y = (index / COLS) * TILE;
        BufferedImage out = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setComposite(java.awt.AlphaComposite.Src);
            g.drawImage(atlas, 0, 0, TILE, TILE, x, y, x + TILE, y + TILE, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static BufferedImage resizeNearest(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
