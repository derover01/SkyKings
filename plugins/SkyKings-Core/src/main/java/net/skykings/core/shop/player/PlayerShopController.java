package net.skykings.core.shop.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Locale;
import java.util.UUID;

/** Bedienung fuer Villager-PlayerShops auf privaten Islands. */
public final class PlayerShopController implements Listener, CommandExecutor {
    private final PlayerShopService service;
    private final PlayerShopStore store;

    public PlayerShopController(PlayerShopService service) {
        this.service = service;
        this.store = service.getStore();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) { usage(player); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("create".equals(sub)) {
            PlayerShop shop = service.create(player, player.getLocation());
            if (shop == null) {
                player.sendMessage(ChatColor.RED + "PlayerShops kannst du nur auf deiner eigenen Insel oder deinem eigenen Plot erstellen.");
                return true;
            }
            Villager villager = player.getWorld().spawn(player.getLocation(), Villager.class);
            villager.setCustomName(ChatColor.GOLD + "Shop von " + player.getName());
            villager.setCustomNameVisible(true);
            shop.setVillagerUuid(villager.getUniqueId());
            store.save();
            player.sendMessage(ChatColor.GREEN + "PlayerShop erstellt. Jetzt /playershop set <Menge> <Preis> und /playershop stock <Menge>.");
            return true;
        }

        PlayerShop shop = nearestOwned(player, 7D);
        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Kein eigener PlayerShop-Villager in der Naehe.");
            return true;
        }

        if ("set".equals(sub) && args.length >= 3) {
            try {
                int amount = Integer.parseInt(args[1]);
                long price = Long.parseLong(args[2]);
                if (!service.configure(player, shop.getId(), amount, price)) throw new IllegalArgumentException();
                player.sendMessage(ChatColor.GREEN + "Angebot: " + amount + " Item(s) fuer " + price + " Coins.");
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Nutze /playershop set <1-64> <Coins>.");
            }
            return true;
        }
        if ("stock".equals(sub) && args.length >= 2) {
            try {
                int amount = Integer.parseInt(args[1]);
                if (!service.addStock(player, shop.getId(), amount)) {
                    player.sendMessage(ChatColor.RED + "Stock konnte nicht hinzugefuegt werden. Halte das passende normale Item in der Hand.");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Stock hinzugefuegt. Gesamt: " + shop.getStock());
            } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungueltige Menge."); }
            return true;
        }
        if (("withdraw".equals(sub) || "take".equals(sub)) && args.length >= 2) {
            try {
                int amount = Integer.parseInt(args[1]);
                if (!service.withdrawStock(player, shop.getId(), amount)) {
                    player.sendMessage(ChatColor.RED + "Stock konnte nicht entnommen werden. Pruefe Menge und Inventarplatz.");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN.toString() + amount + " Item(s) aus dem Shop entnommen. Rest: " + shop.getStock());
            } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungueltige Menge."); }
            return true;
        }
        if ("claim".equals(sub)) {
            long amount = service.claimRevenue(player, shop.getId());
            player.sendMessage(amount > 0 ? ChatColor.GREEN + "Du hast " + amount + " Coins Shop-Einnahmen abgeholt."
                    : ChatColor.YELLOW + "Keine Einnahmen zum Abholen.");
            return true;
        }
        if ("info".equals(sub)) {
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "PLAYERSHOP");
            player.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + shop.getId().toString().substring(0, 8));
            player.sendMessage(ChatColor.GRAY + "Item: " + ChatColor.WHITE + (shop.getMaterial() == null ? "noch nicht gesetzt" : shop.getMaterial().name()));
            player.sendMessage(ChatColor.GRAY + "Menge/Kauf: " + ChatColor.WHITE + shop.getAmountPerSale());
            player.sendMessage(ChatColor.GRAY + "Preis: " + ChatColor.WHITE + shop.getPriceCoins());
            player.sendMessage(ChatColor.GRAY + "Stock: " + ChatColor.WHITE + shop.getStock());
            player.sendMessage(ChatColor.GRAY + "Einnahmen: " + ChatColor.WHITE + shop.getPendingRevenue());
            return true;
        }
        if ("remove".equals(sub)) {
            if (shop.getStock() > 0 || shop.getPendingRevenue() > 0) {
                player.sendMessage(ChatColor.RED + "Leere zuerst den Stock mit /playershop withdraw und hole Einnahmen ab.");
                return true;
            }
            removeVillager(shop.getVillagerUuid());
            store.delete(shop.getId());
            player.sendMessage(ChatColor.YELLOW + "PlayerShop entfernt.");
            return true;
        }
        usage(player);
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        PlayerShop shop = store.getByVillager(event.getRightClicked().getUniqueId());
        if (shop == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!isNearStoredLocation(shop, event.getRightClicked().getLocation())) {
            player.sendMessage(ChatColor.RED + "Dieser PlayerShop wurde verschoben und ist aus Sicherheitsgruenden deaktiviert.");
            return;
        }
        if (player.getUniqueId().equals(shop.getOwner())) {
            player.sendMessage(ChatColor.GOLD + "Dein PlayerShop: " + ChatColor.GRAY + shop.getStock() + " Stock, "
                    + shop.getPendingRevenue() + " Coins Einnahmen. /playershop info");
            return;
        }
        PlayerShopService.Result result = service.purchase(player, shop.getId());
        if (result == PlayerShopService.Result.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Kauf erfolgreich: " + shop.getAmountPerSale() + "x " + shop.getMaterial().name()
                    + " fuer " + shop.getPriceCoins() + " Coins.");
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.65F, 1.25F);
        } else {
            player.sendMessage(ChatColor.RED + "Kauf nicht moeglich: " + readable(result));
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.45F, 0.8F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;
        if (store.getByVillager(event.getEntity().getUniqueId()) != null) event.setCancelled(true);
    }

    private boolean isNearStoredLocation(PlayerShop shop, Location current) {
        if (shop.getWorld() == null || current == null || current.getWorld() == null || !shop.getWorld().equals(current.getWorld().getName())) return false;
        double dx = current.getX() - shop.getX();
        double dy = current.getY() - shop.getY();
        double dz = current.getZ() - shop.getZ();
        return dx * dx + dy * dy + dz * dz <= 16D;
    }

    private PlayerShop nearestOwned(Player player, double radius) {
        double best = radius * radius;
        PlayerShop result = null;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Villager)) continue;
            PlayerShop shop = store.getByVillager(entity.getUniqueId());
            if (shop == null || !player.getUniqueId().equals(shop.getOwner())) continue;
            double distance = entity.getLocation().distanceSquared(player.getLocation());
            if (distance < best) { best = distance; result = shop; }
        }
        return result;
    }

    private void removeVillager(UUID uuid) {
        if (uuid == null) return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) if (uuid.equals(entity.getUniqueId())) { entity.remove(); return; }
        }
    }

    private String readable(PlayerShopService.Result result) {
        switch (result) {
            case OUT_OF_STOCK: return "ausverkauft";
            case NOT_ENOUGH_MONEY: return "zu wenig Coins";
            case INVENTORY_FULL: return "Inventar voll";
            case INVALID_SHOP: return "Shop nicht konfiguriert";
            default: return "Transaktion fehlgeschlagen";
        }
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.GOLD + "PlayerShops (eigene Insel / eigener Plot)");
        player.sendMessage(ChatColor.YELLOW + "/playershop create");
        player.sendMessage(ChatColor.YELLOW + "/playershop set <Menge> <Coins>");
        player.sendMessage(ChatColor.YELLOW + "/playershop stock <Menge>" + ChatColor.GRAY + " - Item in Hand");
        player.sendMessage(ChatColor.YELLOW + "/playershop withdraw <Menge>");
        player.sendMessage(ChatColor.YELLOW + "/playershop claim");
        player.sendMessage(ChatColor.YELLOW + "/playershop info");
        player.sendMessage(ChatColor.YELLOW + "/playershop remove");
    }
}
