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
    private static final String CONFIG_TITLE = ChatColor.DARK_GRAY + "SkyKings | Angebot";
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
                player.sendMessage(ChatColor.GREEN + "Angebot: " + amount + " Item(s) fuer " + format(price) + " Coins.");
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
            shop.setVillagerUuid(null);
            villager.remove();
            store.deleteChecked(shop.getId());
            player.sendMessage(ChatColor.RED + "Der PlayerShop konnte nicht sicher gespeichert werden. Dein Haendler-Ei wurde nicht verbraucht.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
            return;
        }
        consumeHand(player);
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLAYERSHOP PLATZIERT! " + ChatColor.GRAY + "Sneak + Rechtsklick stellt Menge und Preis ein.");
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
            if (player.isSneaking()) openConfigMenu(player, shop);
            else openOwnerMenu(player, shop);
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
        if (event.getView() == null) return;
        String title = event.getView().getTitle();
        if (!OWNER_TITLE.equals(title) && !CONFIG_TITLE.equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID shopId = openOwnerShops.get(player.getUniqueId());
        PlayerShop shop = shopId == null ? null : store.get(shopId);
        if (shop == null || !player.getUniqueId().equals(shop.getOwner())) {
            player.closeInventory();
            return;
        }

        if (CONFIG_TITLE.equals(title)) {
            handleConfigClick(player, shop, event.getRawSlot());
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 11) {
            openConfigMenu(player, shop);
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
        if (event.getView() == null) return;
        String title = event.getView().getTitle();
        if (OWNER_TITLE.equals(title) || CONFIG_TITLE.equals(title)) event.setCancelled(true);
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
                ChatColor.YELLOW + "Klicken zum Einstellen",
                ChatColor.DARK_GRAY + "Tipp: Sneak + Rechtsklick auf Villager"));
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
                ChatColor.GRAY + "Einnahmen leer sind.",
                ChatColor.GRAY + "Das Haendler-Ei bekommst du zurueck."));
        openOwnerShops.put(player.getUniqueId(), shop.getId());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.VILLAGER_YES, 0.55F, 1.25F);
    }

    private void openConfigMenu(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(null, 45, CONFIG_TITLE);
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        inv.setItem(4, item(Material.EMERALD, (short) 0,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "ANGEBOT EINSTELLEN",
                ChatColor.GRAY + "Verkaufsmenge und Coin-Preis",
                ChatColor.DARK_GRAY + "werden sofort gespeichert."));

        inv.setItem(10, adjustItem(Material.REDSTONE, ChatColor.RED + "-16 Menge", "Aktuell: " + shop.getAmountPerSale()));
        inv.setItem(11, adjustItem(Material.REDSTONE, ChatColor.RED + "-1 Menge", "Aktuell: " + shop.getAmountPerSale()));
        inv.setItem(13, item(shop.getMaterial() == null ? Material.BARRIER : shop.getMaterial(), shop.getData(),
                ChatColor.AQUA + "Verkaufsmenge: " + ChatColor.WHITE + shop.getAmountPerSale(),
                ChatColor.GRAY + "Erlaubt: 1 bis 64"));
        inv.setItem(15, adjustItem(Material.EMERALD, ChatColor.GREEN + "+1 Menge", "Aktuell: " + shop.getAmountPerSale()));
        inv.setItem(16, adjustItem(Material.EMERALD, ChatColor.GREEN + "+16 Menge", "Aktuell: " + shop.getAmountPerSale()));

        inv.setItem(27, adjustItem(Material.REDSTONE, ChatColor.RED + "-1.000.000 Coins", "Preis: " + format(shop.getPriceCoins())));
        inv.setItem(28, adjustItem(Material.REDSTONE, ChatColor.RED + "-100.000 Coins", "Preis: " + format(shop.getPriceCoins())));
        inv.setItem(29, adjustItem(Material.REDSTONE, ChatColor.RED + "-10.000 Coins", "Preis: " + format(shop.getPriceCoins())));
        inv.setItem(31, item(Material.GOLD_INGOT, (short) 0,
                ChatColor.GOLD + "Preis: " + ChatColor.YELLOW + format(shop.getPriceCoins()) + " Coins",
                ChatColor.GRAY + "Mindestens 1 Coin",
                "",
                ChatColor.DARK_GRAY + "Exakter Wert weiterhin via",
                ChatColor.YELLOW + "/playershop set <Menge> <Coins>"));
        inv.setItem(33, adjustItem(Material.EMERALD, ChatColor.GREEN + "+10.000 Coins", "Preis: " + format(shop.getPriceCoins())));
        inv.setItem(34, adjustItem(Material.EMERALD, ChatColor.GREEN + "+100.000 Coins", "Preis: " + format(shop.getPriceCoins())));
        inv.setItem(35, adjustItem(Material.EMERALD, ChatColor.GREEN + "+1.000.000 Coins", "Preis: " + format(shop.getPriceCoins())));

        inv.setItem(40, item(Material.ARROW, (short) 0, ChatColor.YELLOW + "Zurueck", ChatColor.GRAY + "Zum Shop-Menue"));
        openOwnerShops.put(player.getUniqueId(), shop.getId());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.CLICK, 0.6F, 1.25F);
    }

    private ItemStack adjustItem(Material material, String name, String current) {
        return item(material, (short) 0, name, ChatColor.GRAY + current, ChatColor.YELLOW + "Klicken");
    }

    private void handleConfigClick(Player player, PlayerShop shop, int slot) {
        if (slot == 40) {
            openOwnerMenu(player, shop);
            return;
        }

        int amount = shop.getAmountPerSale();
        long price = shop.getPriceCoins();
        int newAmount = amount;
        long newPrice = price;

        switch (slot) {
            case 10: newAmount = Math.max(1, amount - 16); break;
            case 11: newAmount = Math.max(1, amount - 1); break;
            case 15: newAmount = Math.min(64, amount + 1); break;
            case 16: newAmount = Math.min(64, amount + 16); break;
            case 27: newPrice = subtractFloor(price, 1_000_000L); break;
            case 28: newPrice = subtractFloor(price, 100_000L); break;
            case 29: newPrice = subtractFloor(price, 10_000L); break;
            case 33: newPrice = addCeiling(price, 10_000L); break;
            case 34: newPrice = addCeiling(price, 100_000L); break;
            case 35: newPrice = addCeiling(price, 1_000_000L); break;
            default: return;
        }

        if (newAmount == amount && newPrice == price) {
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.4F, 0.9F);
            return;
        }
        if (!service.configure(player, shop.getId(), newAmount, newPrice)) {
            player.sendMessage(ChatColor.RED + "Die Shop-Einstellung konnte nicht sicher gespeichert werden.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.6F, 0.9F);
            return;
        }
        player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.4F);
        openConfigMenu(player, shop);
    }

    private long subtractFloor(long value, long delta) {
        if (value <= 1L) return 1L;
        return value <= delta ? 1L : value - delta;
    }

    private long addCeiling(long value, long delta) {
        if (value > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        return value + delta;
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

        ItemStack returnedEgg = shopEgg.create();
        if (!canFit(player, returnedEgg)) {
            player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer dein zurueckgegebenes Haendler-Ei.");
            player.sendMessage(ChatColor.GRAY + "Der Shop wurde nicht entfernt.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.6F, 0.9F);
            return;
        }

        if (!store.deleteChecked(shop.getId())) {
            player.sendMessage(ChatColor.RED + "Der PlayerShop konnte nicht sicher entfernt werden. Es wurde nichts veraendert.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.6F, 0.9F);
            return;
        }

        removeVillager(shop.getVillagerUuid());
        openOwnerShops.remove(player.getUniqueId());
        giveReturnedEgg(player, returnedEgg);
        player.sendMessage(ChatColor.YELLOW + "PlayerShop entfernt. " + ChatColor.GREEN + "Dein Haendler-Ei wurde zurueckgegeben.");
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.25F);
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

    private void giveReturnedEgg(Player player, ItemStack egg) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.setItemInHand(egg);
        } else {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(egg);
            if (!leftovers.isEmpty()) {
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
        player.updateInventory();
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
        player.sendMessage(ChatColor.YELLOW + "Sneak + Rechtsklick Villager" + ChatColor.GRAY + " - Menge & Preis einstellen");
        player.sendMessage(ChatColor.YELLOW + "/playershop menu" + ChatColor.GRAY + " - Verwaltung des naechsten eigenen Shops");
        player.sendMessage(ChatColor.YELLOW + "/playershop set <Menge> <Coins>" + ChatColor.GRAY + " - exakten Wert setzen");
        player.sendMessage(ChatColor.YELLOW + "/playershop stock <Menge>" + ChatColor.GRAY + " - Item in Hand");
        player.sendMessage(ChatColor.YELLOW + "/playershop withdraw <Menge>");
        player.sendMessage(ChatColor.YELLOW + "/playershop claim | remove");
    }
}