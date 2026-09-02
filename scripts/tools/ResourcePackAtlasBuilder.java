import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Builds Minecraft 1.8.x item textures from the committed SkyKings RGBA atlas source.
 * The source is gzip-compressed raw RGBA bytes. Java 8 only has to inflate raw pixels
 * and writes the final PNGs itself, avoiding decoder differences in designer-export PNGs.
 */
public final class ResourcePackAtlasBuilder {
    private static final int TILE = 32;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int WIDTH = TILE * COLS;
    private static final int HEIGHT = TILE * ROWS;
    private static final int RAW_BYTES = WIDTH * HEIGHT * 4;

    // Row-major order. Entry 19 is used as pack.png instead of an item texture.
    private static final String[] ITEM_TEXTURES = {
            "minecart_normal.png",        // HOME
            "minecart_furnace.png",       // BACK
            "minecart_hopper.png",        // NEXT
            "barrier.png",                // LOCKED
            "slimeball.png",              // READY
            "fireworks.png",              // COMPLETED
            "ender_eye.png",              // PREMIUM
            "gold_nugget.png",            // COINS
            "nether_star.png",            // STAR
            "map_empty.png",              // BATTLE PASS
            "book_writable.png",          // QUESTS
            "minecart_chest.png",         // KITS
            "minecart_command_block.png", // CRATES
            "repeater.png",               // JACKPOT
            "hopper.png",                 // SHOP
            "name_tag.png",               // TRADE
            "book_written.png",           // CLAN
            "shears.png",                 // DUEL
            "magma_cream.png"              // EVENT
    };

    private ResourcePackAtlasBuilder() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ResourcePackAtlasBuilder <atlas.rgba.gz> <stage-root>");
        }

        File sourceFile = new File(args[0]);
        File stageRoot = new File(args[1]);
        BufferedImage atlas = readRawAtlas(sourceFile);

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

        BufferedImage logo = crop(atlas, 19);
        BufferedImage packIcon = resizeNearest(logo, 128, 128);
        File packPng = new File(stageRoot, "pack.png");
        if (!ImageIO.write(packIcon, "png", packPng)) {
            throw new IOException("Could not write pack.png");
        }

        System.out.println("[OK] Generated " + ITEM_TEXTURES.length + " SkyKings item textures + pack.png");
    }

    private static BufferedImage readRawAtlas(File file) throws IOException {
        if (!file.isFile()) throw new IOException("Atlas source missing: " + file);

        ByteArrayOutputStream rawOut = new ByteArrayOutputStream(RAW_BYTES);
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzip.read(buffer)) != -1) rawOut.write(buffer, 0, read);
        } finally {
            gzip.close();
        }

        byte[] raw = rawOut.toByteArray();
        if (raw.length != RAW_BYTES) {
            throw new IOException("Unexpected raw atlas byte count " + raw.length + "; expected " + RAW_BYTES);
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int p = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int r = raw[p++] & 0xFF;
                int g = raw[p++] & 0xFF;
                int b = raw[p++] & 0xFF;
                int a = raw[p++] & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
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
