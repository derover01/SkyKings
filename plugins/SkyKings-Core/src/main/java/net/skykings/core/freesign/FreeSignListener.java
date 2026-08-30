package net.skykings.core.freesign;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

/** Erstellt, nutzt und schuetzt persistente SkyKings-Free-Signs. */
public final class FreeSignListener implements Listener {

    private final FreeSignStore store;

    public FreeSignListener(FreeSignStore store) {
        this.store = store;
    }

    @EventHandler
    public void onCreate(SignChangeEvent event) {
        if (!"[FREE]".equalsIgnoreCase(ChatColor.stripColor(event.getLine(0)).trim())) return;

        Player player = event.getPlayer();
        if (!player.isOp() && !player.hasPermission("skykings.admin.freesign")) {
            player.sendMessage(ChatColor.RED + "Du darfst keine Free Signs erstellen.");
            event.setCancelled(true);
            return;
        }

        ParsedItem parsed = parseItem(event.getLine(1));
        if (parsed == null) {
            player.sendMessage(ChatColor.RED + "Ungueltige Item-ID. Beispiel: 276 oder 5:2");
            event.setCancelled(true);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(event.getLine(2).trim());
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Ungueltige Menge. Beispiel: 1 oder 64");
            event.setCancelled(true);
            return;
        }
        if (amount < 1 || amount > parsed.material.getMaxStackSize()) {
            player.sendMessage(ChatColor.RED + "Menge muss zwischen 1 und " + parsed.material.getMaxStackSize() + " liegen.");
            event.setCancelled(true);
            return;
        }

        store.put(event.getBlock().getLocation(), new FreeSignStore.FreeItem(parsed.material, parsed.data, amount));
        event.setLine(0, ChatColor.GOLD.toString() + ChatColor.BOLD + "SkyKings");
        event.setLine(1, ChatColor.GREEN.toString() + ChatColor.BOLD + "[ FREE ]");
        event.setLine(2, ChatColor.WHITE + prettyName(parsed.material, parsed.data));
        event.setLine(3, ChatColor.YELLOW + "x" + amount + " | KLICK");
        player.sendMessage(ChatColor.GREEN + "Free Sign erstellt: " + amount + "x " + prettyName(parsed.material, parsed.data));
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) return;

        FreeSignStore.FreeItem freeItem = store.get(block.getLocation());
        if (freeItem == null) return;
        event.setCancelled(true);

        ItemStack reward = new ItemStack(freeItem.getMaterial(), freeItem.getAmount(), freeItem.getData());
        if (!event.getPlayer().getInventory().addItem(reward).isEmpty()) {
            event.getPlayer().sendMessage(ChatColor.RED + "Du brauchst mehr freien Inventarplatz.");
            return;
        }
        event.getPlayer().updateInventory();
        event.getPlayer().sendMessage(ChatColor.GREEN + "Du hast " + freeItem.getAmount() + "x "
                + prettyName(freeItem.getMaterial(), freeItem.getData()) + ChatColor.GREEN + " erhalten.");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!store.contains(event.getBlock().getLocation())) return;
        Player player = event.getPlayer();
        if (!player.isOp() && !player.hasPermission("skykings.admin.freesign")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Dieses Free Sign ist geschuetzt.");
            return;
        }
        store.remove(event.getBlock().getLocation());
        player.sendMessage(ChatColor.YELLOW + "Free Sign entfernt.");
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        removeProtectedSigns(event.blockList().iterator());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        removeProtectedSigns(event.blockList().iterator());
    }

    private void removeProtectedSigns(Iterator<Block> iterator) {
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (store.contains(block.getLocation())) iterator.remove();
        }
    }

    private ParsedItem parseItem(String raw) {
        if (raw == null) return null;
        String input = raw.trim();
        if (input.isEmpty()) return null;

        short data = 0;
        String materialPart = input;
        int colon = input.indexOf(':');
        if (colon >= 0) {
            materialPart = input.substring(0, colon).trim();
            try {
                data = Short.parseShort(input.substring(colon + 1).trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        Material material;
        try {
            material = Material.getMaterial(Integer.parseInt(materialPart));
        } catch (NumberFormatException ignored) {
            material = Material.matchMaterial(materialPart.toUpperCase(java.util.Locale.ROOT));
        }
        if (material == null || material == Material.AIR) return null;
        return new ParsedItem(material, data);
    }

    private String prettyName(Material material, short data) {
        String name = material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String part : name.split(" ")) {
            if (out.length() > 0) out.append(' ');
            if (!part.isEmpty()) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        if (data != 0) out.append(":").append(data);
        String result = out.toString();
        return result.length() <= 15 ? result : result.substring(0, 15);
    }

    private static final class ParsedItem {
        final Material material;
        final short data;

        ParsedItem(Material material, short data) {
            this.material = material;
            this.data = data;
        }
    }
}
