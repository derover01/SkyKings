package net.skykings.core.shop.player;

import net.skykings.core.api.SkyKingsCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** PlayerShops werden ausschliesslich ueber das SkyKings Haendler-Ei platziert. */
public final class PlayerShopController implements Listener, CommandExecutor {
    private static final long EGG_PRICE = 2_500_000L;
    private static final String OWNER_TITLE = ChatColor.DARK_GRAY + "SkyKings | Mein Shop";
    private final PlayerShopService service;
    private final PlayerShopStore store;
    private final PlayerShopEgg shopEgg = new PlayerShopEgg();
    private final Map<UUID, UUID> openOwnerShops = new HashMap<UUID, UUID>();

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
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "PLAYERSHOP " + ChatColor.YELLOW + "Shops werden mit einem Haendler-Ei platziert.");
            player.sendMessage(ChatColor.GRAY + "Kaufe eins mit " + ChatColor.AQUA + "/playershop kaufen" + ChatColor.GRAY + " oder gewinne es spaeter aus Events/Crates.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 1.2F);
            return true;
        }
        if ("kaufen".equals(sub) || "buyegg".equals(sub) || "egg".equals(sub)) {
            SkyKingsCoreAPI core = core();
            if (core == null) { player.sendMessage(ChatColor.RED + "Economy ist noch nicht bereit."); return true; }
            ItemStack egg = shopEgg.create();
            if (!canFit(player, egg)) { player.sendMessage(ChatColor.RED + "Du brauchst genug Inventarplatz fuer das Haendler-Ei."); return true; }
            if (!core.getEconomyService().withdraw(player.getUniqueId(), EGG_PRICE, player.getName(), "PlayerShop Haendler-Ei")) {
                player.sendMessage(ChatColor.RED + "Dir fehlen Coins. Preis: " + ChatColor.YELLOW + format(EGG_PRICE) + " Coins");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
                return true;
            }
            // canFit() ist nur ein Preflight. Falls sich das Inventar trotzdem zwischenzeitlich
            // geaendert hat, darf der Spieler weder Coins verlieren noch ein Item gedroppt bekommen.
            if (!player.getInventory().addItem(egg).isEmpty()) {
                core.getEconomyService().deposit(player.getUniqueId(), EGG_PRICE, "PLAYER_SHOP_EGG_ROLLBACK",
                        "Haendler-Ei konnte nach Abbuchung nicht zugestellt werden");
                player.updateInventory();
                player.sendMessage(ChatColor.RED + "Das Haendler-Ei konnte nicht sicher zugestellt werden. Deine Coins wurden erstattet.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
                return true;
            }
            player.updateInventory();
            player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "HAENDLER-EI GEKAUFT! " + ChatColor.GRAY + "Rechtsklick auf deiner Insel/deinem Plot zum Platzieren.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.4F);
            return true;
        }

        PlayerShop shop = nearestOwned(player, 7D);
        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Kein eigener PlayerShop-Villager in der Naehe.");
            return true;
        }

        if ("menu".equals(sub) || "gui".equals(sub)) {
            openOwnerMenu(player, shop);
            return true;
        }
        if ("set".equals(sub) && args.length >= 3) {
            try {
                int amount = Integer.parseInt(args[1]); long price = Long.parseLong(args[2]);
                if (!service.configure(player, shop.getId(), amount, price)) throw new IllegalArgumentException();
                player.sendMessage(ChatColor.GREEN + "Angebot: " + amount + " Item(s) fuer " + price + " Coins.");
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.6F, 1.3F);
            } catch (IllegalArgumentException ex) { player.sendMessage(ChatColor.RED + "Nutze /playershop set <1-64> <Coins>."); }
            return true;
        }
        if ("stock".equals(sub) && args.length >= 2) {
            try {
                int amount = Integer.parseInt(args[1]);
                if (!service.addStock(player, shop.getId(), amount)) {
                    player.sendMessage(ChatColor.RED + "Stock konnte nicht hinzugefuegt werden. Halte das passende normale Item in der Hand."); return true;
                }
                player.sendMessage(ChatColor.GREEN + "Stock hinzugefuegt. Gesamt: " + shop.getStock());
                player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5F, 1.3F);
            } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungueltige Menge."); }
            return true;
        }
        if (("withdraw".equals(sub) || "take".equals(sub)) && args.length >= 2) {
            try {
                int amount = Integer.parseInt(args[1]);
                if (!service.withdrawStock(player, shop.getId(), amount)) {
                    player.sendMessage(ChatColor.RED + "Stock konnte nicht entnommen werden. Pruefe Menge und Inventarplatz."); return true;
                }
                player.sendMessage(ChatColor.GREEN.toString() + amount + " Item(s) entnommen. Rest: " + shop.getStock());
                player.playSound(player.getLocation(), Sound.CHEST_CLOSE, 0.5F, 1.1F);
            } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungueltige Menge."); }
            return true;
        }
        if ("claim".equals(sub)) {
            claimRevenue(player, shop);
            return true;
        }
        if ("info".equals(sub)) {
            openOwnerMenu(player, shop);
            return true;
        }
        if ("remove".equals(sub)) {
            removeOwnedShop(player, shop);
            return true;
        }
        usage(player); return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !shopEgg.isShopEgg(event.getItem()) || event.getClickedBlock() == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5D, 0D, 0.5D);
        PlayerShop shop = service.create(player, location);
        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Das Haendler-Ei kann nur auf deiner eigenen Insel oder deinem eigenen Plot platziert werden.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return;
        }

        final Villager villager;
        try {
            villager = player.getWorld().spawn(location, Villager.class);
        } catch (RuntimeException ex) {
            store.deleteChecked(shop.getId());
            player.sendMessage(ChatColor.RED + "Der PlayerShop konnte nicht sicher erstellt werden. Dein Haendler-Ei wurde nicht verbraucht.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }
        villager.setCustomName(ChatColor.GOLD.toString() + ChatColor.BOLD + "Shop " + ChatColor.YELLOW + "von " + player.getName());
        villager.setCustomNameVisible(true);
        shop.setVillagerUuid(villager.getUniqueId());
        if (!store.saveChecked()) {
            // Der initiale Shop-Datensatz wurde bereits persistiert. Wenn die Villager-Verknuepfung
            // nicht sicher gespeichert werden kann, weder Ei verbrauchen noch einen halben Shop stehen lassen.
            shop.setVillagerUuid(null);
            villager.remove();
            store.deleteChecked(shop.getId());
            player.sendMessage(ChatColor.RED + "Der PlayerShop konnte nicht sicher gespeichert werden. Dein Haendler-Ei wurde nicht verbraucht.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }
        consumeHand(player);
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLAYERSHOP PLATZIERT! " + ChatColor.GRAY + "Rechtsklick auf den Haendler fuer die Verwaltung.");
        player.playSound(location, Sound.LEVEL_UP, 0.8F, 1.35F);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        PlayerShop shop = store.getByVillager(event.getRightClicked().getUniqueId()); if (shop == null) return;
        event.setCancelled(true); Player player = event.getPlayer();
        if (!isNearStoredLocation(shop, event.getRightClicked().getLocation())) {
            player.sendMessage(ChatColor.RED + "Dieser PlayerShop wurde verschoben und ist deaktiviert."); return;
        }
        if (player.getUniqueId().equals(shop.getOwner())) {
            openOwnerMenu(player, shop);
            return;
        }
        PlayerShopService.Result result = service.purchase(player, shop.getId());
        if (result == PlayerShopService.Result.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Kauf erfolgreich: " + shop.getAmountPerSale() + "x " + shop.getMaterial().name() + " fuer " + shop.getPriceCoins() + " Coins.");
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.65F, 1.25F);
        } else {
            player.sendMessage(ChatColor.RED + "Kauf nicht moeglich: " + readable(result));
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.45F, 0.8F);
        }
    }

    @EventHandler
    public void onOwnerMenuClick(InventoryClickEvent event) {
        if (event.getView() == null || !OWNER_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID shopId = openOwnerShops.get(player.getUniqueId());
        PlayerShop shop = shopId == null ? null : store.get(shopId);
        if (shop == null || !player.getUniqueId().equals(shop.getOwner())) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 11) {
            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "Angebot setzen: " + ChatColor.YELLOW + "/playershop set <Menge> <Coins>" + ChatColor.GRAY + " und dabei das Verkaufsitem in der Hand halten.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.5F, 1.15F);
        } else if (slot == 13) {
            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "Lager: " + ChatColor.YELLOW + "/playershop stock <Menge>" + ChatColor.GRAY + " bzw. /playershop withdraw <Menge>.");
            player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5F, 1.2F);
        } else if (slot == 15) {
            claimRevenue(player, shop);
            openOwnerMenu(player, shop);
        } else if (slot == 22) {
            player.closeInventory();
            removeOwnedShop(player, shop);
        }
    }

    @EventHandler
    public void onOwnerMenuDrag(InventoryDragEvent event) {
        if (event.getView() != null && OWNER_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager && store.getByVillager(event.getEntity().getUniqueId()) != null) event.setCancelled(true);
    }

    private void openOwnerMenu(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(null, 27, OWNER_TITLE);
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        Material offerMaterial = shop.getMaterial() == null ? Material.BARRIER : shop.getMaterial();
        inv.setItem(11, item(offerMaterial, (short) 0,
                ChatColor.GOLD + "Angebot",
                ChatColor.GRAY + "Item: " + ChatColor.WHITE + (shop.getMaterial() == null ? "noch nicht gesetzt" : shop.getMaterial().name()),
                ChatColor.GRAY + "Menge: " + ChatColor.WHITE + shop.getAmountPerSale(),
                ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + format(shop.getPriceCoins()) + " Coins",
                "",
                ChatColor.YELLOW + "Klicken fuer Set-Anleitung"));
        inv.setItem(13, item(Material.CHEST, (short) 0,
                ChatColor.AQUA + "Lager",
                ChatColor.GRAY + "Aktueller Stock: " + ChatColor.WHITE + shop.getStock(),
                "",
                ChatColor.YELLOW + "Klicken fuer Lager-Befehle"));
        inv.setItem(15, item(Material.GOLD_INGOT, (short) 0,
                ChatColor.GREEN + "Einnahmen",
                ChatColor.GRAY + "Bereit: " + ChatColor.GOLD + format(shop.getPendingRevenue()) + " Coins",
                "",
                shop.getPendingRevenue() > 0 ? ChatColor.GREEN + "Klicken zum Abholen" : ChatColor.DARK_GRAY + "Noch keine Einnahmen"));
        inv.setItem(22, item(Material.REDSTONE_BLOCK, (short) 0,
                ChatColor.RED + "Shop entfernen",
                ChatColor.GRAY + "Nur moeglich wenn Stock und",
                ChatColor.GRAY + "Einnahmen leer sind."));
        openOwnerShops.put(player.getUniqueId(), shop.getId());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.VILLAGER_YES, 0.55F, 1.25F);
    }

    private void claimRevenue(Player player, PlayerShop shop) {
        try {
            long amount = service.claimRevenue(player, shop.getId());
            player.sendMessage(amount > 0 ? ChatColor.GREEN + "Du hast " + format(amount) + " Coins Shop-Einnahmen abgeholt."
                    : ChatColor.YELLOW + "Keine Einnahmen zum Abholen.");
            player.playSound(player.getLocation(), amount > 0 ? Sound.LEVEL_UP : Sound.NOTE_BASS, 0.6F, amount > 0 ? 1.5F : 0.8F);
        } catch (PlayerShopService.RevenueClaimOverflowException ex) {
            player.sendMessage(ChatColor.RED + "Dein Coin-Kontostand ist zu hoch, um diese Shop-Einnahmen sicher auszuzahlen.");
            player.sendMessage(ChatColor.GRAY + "Die Einnahmen bleiben im Shop gespeichert. Gib zuerst Coins aus und versuche es erneut.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 0.9F);
        }
    }

    private void removeOwnedShop(Player player, PlayerShop shop) {
        if (shop.getStock() > 0 || shop.getPendingRevenue() > 0) {
            player.sendMessage(ChatColor.RED + "Leere zuerst den Stock und hole Einnahmen ab.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.6F, 0.9F);
            return;
        }
        // Erst die persistente Shop-Definition entfernen. Wenn das fehlschlaegt, muss der Villager
        // stehen bleiben, damit kein gueltiger Shop-Datensatz ohne zugehoerige Entity entsteht.
        if (!store.deleteChecked(shop.getId())) {
            player.sendMessage(ChatColor.RED + "Der PlayerShop konnte nicht sicher entfernt werden. Es wurde nichts veraendert.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.6F, 0.9F);
            return;
        }
        removeVillager(shop.getVillagerUuid());
        openOwnerShops.remove(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "PlayerShop entfernt.");
        player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 0.7F);
    }

    private ItemStack item(Material material, short durability, String name, String... lore) {
        ItemStack stack = new ItemStack(material, 1, durability);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean canFit(Player player, ItemStack item) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(item.clone()).isEmpty();
    }

    private void consumeHand(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null) return;
        if (hand.getAmount() <= 1) player.setItemInHand(null); else hand.setAmount(hand.getAmount() - 1);
        player.updateInventory();
    }
    private SkyKingsCoreAPI core() {
        RegisteredServiceProvider<SkyKingsCoreAPI> registration = Bukkit.getServicesManager().getRegistration(SkyKingsCoreAPI.class);
        return registration == null ? null : registration.getProvider();
    }
    private boolean isNearStoredLocation(PlayerShop shop, Location current) {
        if (shop.getWorld() == null || current == null || current.getWorld() == null || !shop.getWorld().equals(current.getWorld().getName())) return false;
        double dx = current.getX() - shop.getX(), dy = current.getY() - shop.getY(), dz = current.getZ() - shop.getZ();
        return dx * dx + dy * dy + dz * dz <= 16D;
    }
    private PlayerShop nearestOwned(Player player, double radius) {
        double best = radius * radius; PlayerShop result = null;
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
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) if (uuid.equals(entity.getUniqueId())) { entity.remove(); return; }
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
    private String format(long value) { return String.format("%,d", value).replace(',', '.'); }
    private void usage(Player player) {
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS PLAYERSHOPS");
        player.sendMessage(ChatColor.YELLOW + "/playershop kaufen" + ChatColor.GRAY + " - Haendler-Ei fuer " + format(EGG_PRICE) + " Coins");
        player.sendMessage(ChatColor.YELLOW + "/playershop menu" + ChatColor.GRAY + " - Verwaltung des naechsten eigenen Shops");
        player.sendMessage(ChatColor.YELLOW + "/playershop set <Menge> <Coins>");
        player.sendMessage(ChatColor.YELLOW + "/playershop stock <Menge>" + ChatColor.GRAY + " - Item in Hand");
        player.sendMessage(ChatColor.YELLOW + "/playershop withdraw <Menge>");
        player.sendMessage(ChatColor.YELLOW + "/playershop claim | remove");
    }
}
