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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** PlayerShop mit Besitzer-Dashboard, 3x9 Editor und echtem 1.8-Villager-Handelsfenster. */
public final class PlayerShopTradeController implements Listener, CommandExecutor {
    private static final long EGG_PRICE = 2_500_000L;
    private static final String OWNER_TITLE = ChatColor.DARK_GRAY + "SkyKings | Mein Shop";
    private static final String SETUP_TITLE = ChatColor.DARK_GRAY + "SkyKings | Angebote";

    private final PlayerShopService service;
    private final PlayerShopStore store;
    private final PlayerShopEgg shopEgg = new PlayerShopEgg();
    private final LegacyVillagerTradeBridge merchantBridge = new LegacyVillagerTradeBridge();
    private final Map<UUID, UUID> openShops = new HashMap<UUID, UUID>();
    private final Map<UUID, UUID> merchantShops = new HashMap<UUID, UUID>();
    private final Map<UUID, UUID> merchantVillagers = new HashMap<UUID, UUID>();

    public PlayerShopTradeController(PlayerShopService service) {
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
        if (args.length == 0) {
            usage(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("kaufen".equals(sub) || "egg".equals(sub) || "buyegg".equals(sub)) return buyEgg(player);

        PlayerShop shop = nearestOwned(player, 7D);
        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Kein eigener PlayerShop-Villager in der Naehe.");
            return true;
        }
        if ("menu".equals(sub) || "gui".equals(sub)) {
            openOwnerMenu(player, shop);
            return true;
        }
        if ("setup".equals(sub) || "set".equals(sub) || "stock".equals(sub)) {
            openSetup(player, shop);
            return true;
        }
        if ("claim".equals(sub)) {
            claimRevenue(player, shop);
            return true;
        }
        if ("remove".equals(sub)) {
            removeOwnedShop(player, shop);
            return true;
        }
        usage(player);
        return true;
    }

    private boolean buyEgg(Player player) {
        SkyKingsCoreAPI core = core();
        if (core == null) {
            player.sendMessage(ChatColor.RED + "Economy ist noch nicht bereit.");
            return true;
        }
        ItemStack egg = shopEgg.create();
        if (!canFit(player, egg)) {
            player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer das Haendler-Ei.");
            return true;
        }
        if (!core.getEconomyService().withdraw(player.getUniqueId(), EGG_PRICE, player.getName(), "PlayerShop Haendler-Ei")) {
            player.sendMessage(ChatColor.RED + "Dir fehlen Coins. Preis: " + ChatColor.YELLOW + format(EGG_PRICE) + " Coins");
            return true;
        }
        if (!player.getInventory().addItem(egg).isEmpty()) {
            core.getEconomyService().deposit(player.getUniqueId(), EGG_PRICE, "PLAYER_SHOP_EGG_ROLLBACK", "Ei-Zustellung fehlgeschlagen");
            player.sendMessage(ChatColor.RED + "Ei konnte nicht zugestellt werden. Coins wurden erstattet.");
            return true;
        }
        player.updateInventory();
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.4F);
        player.sendMessage(ChatColor.GREEN + "Haendler-Ei gekauft. Rechtsklick auf einen Block zum Platzieren.");
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || !shopEgg.isShopEgg(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5D, 0D, 0.5D);
        PlayerShop shop = service.create(player, location);
        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Das Haendler-Ei kann nur auf deiner eigenen Insel/deinem Plot platziert werden.");
            return;
        }
        Villager villager;
        try {
            villager = player.getWorld().spawn(location, Villager.class);
        } catch (RuntimeException ex) {
            store.deleteChecked(shop.getId());
            player.sendMessage(ChatColor.RED + "Shop konnte nicht erstellt werden; Ei bleibt erhalten.");
            return;
        }
        villager.setCustomName(ChatColor.GOLD.toString() + ChatColor.BOLD + "Shop " + ChatColor.YELLOW + "von " + player.getName());
        villager.setCustomNameVisible(true);
        shop.setVillagerUuid(villager.getUniqueId());
        if (!store.saveChecked()) {
            shop.setVillagerUuid(null);
            villager.remove();
            store.deleteChecked(shop.getId());
            player.sendMessage(ChatColor.RED + "Shop konnte nicht gespeichert werden; Ei bleibt erhalten.");
            return;
        }
        consumeHand(player);
        player.playSound(location, Sound.LEVEL_UP, 0.8F, 1.35F);
        player.sendMessage(ChatColor.GREEN + "PlayerShop platziert. " + ChatColor.YELLOW + "Shift + Rechtsklick" + ChatColor.GRAY + " fuer dein Shop-Menue.");
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Villager villager = (Villager) event.getRightClicked();
        PlayerShop shop = store.getByVillager(villager.getUniqueId());
        if (shop == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!isNearStoredLocation(shop, villager.getLocation())) {
            player.sendMessage(ChatColor.RED + "Dieser PlayerShop wurde verschoben und ist deaktiviert.");
            return;
        }
        if (player.getUniqueId().equals(shop.getOwner()) && player.isSneaking()) {
            openOwnerMenu(player, shop);
            return;
        }
        openTrade(player, villager, shop);
    }

    private void openOwnerMenu(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(null, 27, OWNER_TITLE);
        fill(inv, Material.STAINED_GLASS_PANE, (short) 15, " ");
        inv.setItem(11, item(Material.CHEST, (short) 0,
                ChatColor.GOLD + "Angebote bearbeiten",
                ChatColor.GRAY + "Bis zu 9 Angebote gleichzeitig",
                ChatColor.YELLOW + "Klicken: 3x9 Editor oeffnen"));
        inv.setItem(13, item(Material.NETHER_STAR, (short) 0,
                ChatColor.AQUA + "Erloes-Lager",
                ChatColor.GRAY + "Gespeichert: " + ChatColor.WHITE + format(shop.getPendingRevenue()),
                ChatColor.YELLOW + "Klicken: Erloes herausnehmen"));
        inv.setItem(15, item(Material.MONSTER_EGG, (short) 120,
                ChatColor.RED + "Shop entfernen",
                ChatColor.GRAY + "Nur wenn alle Angebote leer sind",
                ChatColor.GRAY + "und das Erloes-Lager geleert wurde."));
        openShops.put(player.getUniqueId(), shop.getId());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.55F, 1.15F);
    }

    private void openTrade(final Player player, final Villager villager, final PlayerShop shop) {
        if (!shop.isConfigured()) {
            player.sendMessage(ChatColor.YELLOW + "Dieser PlayerShop hat aktuell keine Angebote.");
            return;
        }
        if (!merchantBridge.configureAndOpen(player, villager, shop)) {
            player.sendMessage(ChatColor.RED + "Das Villager-Handelsfenster konnte auf dieser 1.8-Runtime nicht geoeffnet werden.");
            return;
        }
        merchantShops.put(player.getUniqueId(), shop.getId());
        merchantVillagers.put(player.getUniqueId(), villager.getUniqueId());
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (plugin == null) return;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (!player.isOnline() || !merchantShops.containsKey(player.getUniqueId())) return;
                Inventory top = player.getOpenInventory().getTopInventory();
                if (top == null || top.getType() != InventoryType.MERCHANT || top.getSize() < 3) return;
                top.setItem(0, merchantBridge.virtualCoinToken());
                top.setItem(1, new ItemStack(Material.AIR));
                player.updateInventory();
            }
        });
    }

    /** Besitzer-Editor: zwei echte Itemreihen + eine Preisreihe. */
    private void openSetup(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(null, 27, SETUP_TITLE);
        for (int i = 0; i < PlayerShop.MAX_OFFERS; i++) {
            PlayerShopOffer offer = shop.getOffer(i);
            ItemStack top = offer.topStack();
            ItemStack middle = offer.middleStack();
            if (top != null) inv.setItem(i, top);
            if (middle != null) inv.setItem(9 + i, middle);
            inv.setItem(18 + i, priceItem(offer, i));
        }
        openShops.put(player.getUniqueId(), shop.getId());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5F, 1.25F);
        player.sendMessage(ChatColor.GRAY + "Items direkt oben/mittig hineinlegen. " + ChatColor.YELLOW + "Shift-Klick" + ChatColor.GRAY + " aus deinem Inventar funktioniert ebenfalls.");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView() == null || !(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory() != null && event.getInventory().getType() == InventoryType.MERCHANT && merchantShops.containsKey(player.getUniqueId())) {
            handleMerchantClick(player, event);
            return;
        }

        String title = event.getView().getTitle();
        if (!OWNER_TITLE.equals(title) && !SETUP_TITLE.equals(title)) return;

        UUID shopId = openShops.get(player.getUniqueId());
        PlayerShop shop = shopId == null ? null : store.get(shopId);
        if (shop == null) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (OWNER_TITLE.equals(title)) {
            event.setCancelled(true);
            if (!player.getUniqueId().equals(shop.getOwner())) {
                player.closeInventory();
                return;
            }
            int raw = event.getRawSlot();
            if (raw == 11) openSetup(player, shop);
            else if (raw == 13) {
                claimRevenue(player, shop);
                openOwnerMenu(player, shop);
            } else if (raw == 15) removeOwnedShop(player, shop);
            return;
        }

        event.setCancelled(true);
        if (!player.getUniqueId().equals(shop.getOwner())) {
            player.closeInventory();
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= 0 && raw < 18) {
            handleOfferSlot(player, shop, raw % 9, raw >= 9, event);
            return;
        }
        if (raw >= 18 && raw < 27) {
            handlePrice(player, shop, raw % 9, event);
            return;
        }
        if (raw >= 27 && event.isShiftClick()) shiftDeposit(player, shop, event);
    }

    private void handleMerchantClick(final Player player, InventoryClickEvent event) {
        UUID shopId = merchantShops.get(player.getUniqueId());
        final PlayerShop shop = shopId == null ? null : store.get(shopId);
        if (shop == null) {
            event.setCancelled(true);
            clearMerchantSession(player, event.getInventory());
            player.closeInventory();
            return;
        }

        int raw = event.getRawSlot();
        if (raw >= 0 && raw < event.getInventory().getSize()) {
            event.setCancelled(true);
            if (raw != 2) return;

            ItemStack preview = event.getCurrentItem();
            int offerIndex = merchantBridge.offerIndex(preview);
            if (offerIndex < 0 || offerIndex >= PlayerShop.MAX_OFFERS) {
                player.sendMessage(ChatColor.RED + "Dieses Angebot konnte nicht eindeutig erkannt werden.");
                return;
            }
            PlayerShopOffer offer = shop.getOffer(offerIndex);
            long price = offer == null ? 0L : offer.getPriceCoins();
            int amount = offer == null ? 0 : offer.getTotalAmount();
            Material material = offer == null ? null : offer.getMaterial();
            PlayerShopService.Result result = service.purchase(player, shop.getId(), offerIndex);
            if (result == PlayerShopService.Result.SUCCESS) {
                player.sendMessage(ChatColor.GREEN + "Gekauft: " + amount + "x " + (material == null ? "Item" : material.name()) + " fuer " + format(price) + " Coins.");
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.25F);
            } else {
                player.sendMessage(ChatColor.RED + "Kauf nicht moeglich: " + readable(result));
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.8F);
            }

            final UUID villagerId = merchantVillagers.get(player.getUniqueId());
            clearMerchantSession(player, event.getInventory());
            player.closeInventory();
            Plugin plugin = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
            if (plugin == null) return;
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    if (!player.isOnline()) return;
                    Villager villager = findVillager(villagerId);
                    PlayerShop latest = store.get(shop.getId());
                    if (villager != null && latest != null && latest.isConfigured()) openTrade(player, villager, latest);
                }
            });
            return;
        }
        if (event.isShiftClick()) event.setCancelled(true);
    }

    private void handleOfferSlot(Player player, PlayerShop shop, int column, boolean middle, InventoryClickEvent event) {
        PlayerShopOffer offer = shop.getOffer(column);
        if (offer == null) return;
        int stored = middle ? offer.getAmountMiddle() : offer.getAmountTop();
        ItemStack cursor = event.getCursor();
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR && cursor.getAmount() > 0;

        if (stored > 0 && !hasCursor) {
            ItemStack returned = service.takeOfferStack(player, shop.getId(), column, middle);
            if (returned == null) {
                player.sendMessage(ChatColor.RED + "Stack konnte nicht sicher entnommen werden.");
                return;
            }
            event.setCursor(returned);
            player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 0.5F, 1.2F);
            openSetupNextTick(player, shop);
            return;
        }
        if (stored > 0) {
            player.sendMessage(ChatColor.YELLOW + "Nimm den vorhandenen Stack erst heraus, bevor du ihn ersetzt.");
            return;
        }
        if (!hasCursor) return;

        ItemStack deposit = cursor.clone();
        if (!service.putOfferStack(player, shop.getId(), column, middle, deposit)) {
            player.sendMessage(ChatColor.RED + "Die zweite Reihe einer Spalte muss dasselbe Item/Data wie die erste enthalten. Custom-Meta/Enchantments sind hier nicht erlaubt.");
            return;
        }
        event.setCursor(new ItemStack(Material.AIR));
        if (shop.getOffer(column).getPriceCoins() <= 0L) service.setOfferPrice(player, shop.getId(), column, 1L);
        player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.35F);
        openSetupNextTick(player, shop);
    }

    private void shiftDeposit(Player player, PlayerShop shop, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getAmount() <= 0) return;
        ItemStack deposit = clicked.clone();
        for (int column = 0; column < PlayerShop.MAX_OFFERS; column++) {
            PlayerShopOffer offer = shop.getOffer(column);
            if (offer == null) continue;
            if (offer.getAmountTop() <= 0) {
                if (service.putOfferStack(player, shop.getId(), column, false, deposit)) {
                    event.setCurrentItem(new ItemStack(Material.AIR));
                    if (offer.getPriceCoins() <= 0L) service.setOfferPrice(player, shop.getId(), column, 1L);
                    player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.35F);
                    openSetupNextTick(player, shop);
                    return;
                }
            } else if (offer.getAmountMiddle() <= 0 && offer.getMaterial() == deposit.getType() && offer.getData() == deposit.getDurability()) {
                if (service.putOfferStack(player, shop.getId(), column, true, deposit)) {
                    event.setCurrentItem(new ItemStack(Material.AIR));
                    player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.35F);
                    openSetupNextTick(player, shop);
                    return;
                }
            }
        }
        player.sendMessage(ChatColor.RED + "Kein passender freier Angebots-Slot vorhanden.");
    }

    private void handlePrice(Player player, PlayerShop shop, int column, InventoryClickEvent event) {
        PlayerShopOffer offer = shop.getOffer(column);
        if (offer == null || offer.getTotalAmount() <= 0) {
            player.sendMessage(ChatColor.RED + "Lege zuerst Items in diese Spalte.");
            return;
        }
        long current = Math.max(1L, offer.getPriceCoins());
        long next = current;
        ClickType click = event.getClick();
        if (click == ClickType.MIDDLE) next = safeAdd(current, 100L);
        else if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) next = 1L;
        else if (event.isShiftClick() && event.isLeftClick()) next = safeAdd(current, 10L);
        else if (event.isShiftClick() && event.isRightClick()) next = Math.max(1L, current - 10L);
        else if (event.isLeftClick()) next = safeAdd(current, 1L);
        else if (event.isRightClick()) next = Math.max(1L, current - 1L);
        else return;

        if (!service.setOfferPrice(player, shop.getId(), column, next)) {
            player.sendMessage(ChatColor.RED + "Preis konnte nicht gespeichert werden.");
            return;
        }
        player.playSound(player.getLocation(), Sound.CLICK, 0.4F, next >= current ? 1.4F : 0.9F);
        openSetupNextTick(player, shop);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView() == null) return;
        if (event.getInventory() != null && event.getInventory().getType() == InventoryType.MERCHANT && merchantShops.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (OWNER_TITLE.equals(event.getView().getTitle()) || SETUP_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (merchantShops.containsKey(playerId) && event.getInventory() != null && event.getInventory().getType() == InventoryType.MERCHANT) {
            clearMerchantSession(playerId, event.getInventory());
        }
        openShops.remove(playerId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager && store.getByVillager(event.getEntity().getUniqueId()) != null) event.setCancelled(true);
    }

    private ItemStack priceItem(PlayerShopOffer offer, int index) {
        boolean active = offer != null && offer.getTotalAmount() > 0;
        return item(Material.NETHER_STAR, (short) 0,
                active ? ChatColor.AQUA + "Preis: " + ChatColor.WHITE + format(Math.max(1L, offer.getPriceCoins())) : ChatColor.DARK_GRAY + "Angebot " + (index + 1),
                active ? ChatColor.GRAY + "Gesamtmenge: " + ChatColor.WHITE + offer.getTotalAmount() : ChatColor.GRAY + "Erst Items oben/mittig einlegen",
                ChatColor.GREEN + "Linksklick: +1",
                ChatColor.RED + "Rechtsklick: -1",
                ChatColor.GREEN + "Shift + Links: +10",
                ChatColor.RED + "Shift + Rechts: -10",
                ChatColor.AQUA + "Mittelklick: +100",
                ChatColor.YELLOW + "Q: auf 1 zuruecksetzen");
    }

    private void claimRevenue(Player player, PlayerShop shop) {
        try {
            long amount = service.claimRevenue(player, shop.getId());
            player.sendMessage(amount > 0
                    ? ChatColor.GREEN + "Du hast " + format(amount) + " aus dem Erloes-Lager genommen."
                    : ChatColor.YELLOW + "Das Erloes-Lager ist leer.");
            if (amount > 0) player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.4F);
        } catch (PlayerShopService.RevenueClaimOverflowException ex) {
            player.sendMessage(ChatColor.RED + "Dein Kontostand ist zu hoch fuer diese Auszahlung.");
        }
    }

    private void removeOwnedShop(Player player, PlayerShop shop) {
        if (!offersEmpty(shop) || shop.getPendingRevenue() > 0L) {
            player.sendMessage(ChatColor.RED + "Nimm zuerst alle angebotenen Items heraus und leere dein Erloes-Lager.");
            return;
        }
        ItemStack egg = shopEgg.create();
        if (!canFit(player, egg)) {
            player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer das Haendler-Ei.");
            return;
        }
        if (!store.deleteChecked(shop.getId())) {
            player.sendMessage(ChatColor.RED + "Shop konnte nicht sicher entfernt werden.");
            return;
        }
        removeVillager(shop.getVillagerUuid());
        openShops.remove(player.getUniqueId());
        if (player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR) player.setItemInHand(egg);
        else player.getInventory().addItem(egg);
        player.updateInventory();
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "PlayerShop entfernt. Dein Haendler-Ei wurde zurueckgegeben.");
    }

    private boolean offersEmpty(PlayerShop shop) {
        for (PlayerShopOffer offer : shop.getOffers()) if (offer != null && offer.getTotalAmount() > 0) return false;
        return true;
    }

    private PlayerShop nearestOwned(Player player, double radius) {
        PlayerShop best = null;
        double bestDistance = radius * radius;
        for (PlayerShop shop : store.all()) {
            if (!player.getUniqueId().equals(shop.getOwner()) || shop.getWorld() == null || !shop.getWorld().equals(player.getWorld().getName())) continue;
            double dx = shop.getX() - player.getLocation().getX();
            double dy = shop.getY() - player.getLocation().getY();
            double dz = shop.getZ() - player.getLocation().getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = shop;
            }
        }
        return best;
    }

    private boolean isNearStoredLocation(PlayerShop shop, Location location) {
        if (shop.getWorld() == null || location.getWorld() == null || !shop.getWorld().equals(location.getWorld().getName())) return false;
        double dx = shop.getX() - location.getX();
        double dy = shop.getY() - location.getY();
        double dz = shop.getZ() - location.getZ();
        return dx * dx + dy * dy + dz * dz <= 4.0D;
    }

    private Villager findVillager(UUID uuid) {
        if (uuid == null) return null;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (uuid.equals(entity.getUniqueId()) && entity instanceof Villager) return (Villager) entity;
            }
        }
        return null;
    }

    private void removeVillager(UUID uuid) {
        Villager villager = findVillager(uuid);
        if (villager != null) villager.remove();
    }

    private void clearMerchantSession(Player player, Inventory inventory) {
        clearMerchantSession(player.getUniqueId(), inventory);
    }

    private void clearMerchantSession(UUID playerId, Inventory inventory) {
        if (inventory != null && inventory.getType() == InventoryType.MERCHANT) {
            if (inventory.getSize() > 0) inventory.setItem(0, new ItemStack(Material.AIR));
            if (inventory.getSize() > 1) inventory.setItem(1, new ItemStack(Material.AIR));
        }
        merchantShops.remove(playerId);
        merchantVillagers.remove(playerId);
    }

    private void consumeHand(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        if (hand.getAmount() <= 1) player.setItemInHand(new ItemStack(Material.AIR));
        else {
            hand.setAmount(hand.getAmount() - 1);
            player.setItemInHand(hand);
        }
        player.updateInventory();
    }

    private boolean canFit(Player player, ItemStack item) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(item.clone()).isEmpty();
    }

    private ItemStack item(Material material, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inv, Material material, short data, String name) {
        ItemStack filler = item(material, data, name);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private long safeAdd(long current, long step) {
        return current > Long.MAX_VALUE - step ? Long.MAX_VALUE : current + step;
    }

    private void openSetupNextTick(final Player player, final PlayerShop shop) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (plugin == null) return;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (player.isOnline() && store.get(shop.getId()) != null) openSetup(player, shop);
            }
        });
    }

    private SkyKingsCoreAPI core() {
        RegisteredServiceProvider<SkyKingsCoreAPI> provider = Bukkit.getServicesManager().getRegistration(SkyKingsCoreAPI.class);
        return provider == null ? null : provider.getProvider();
    }

    private String readable(PlayerShopService.Result result) {
        if (result == null) return "Unbekannter Fehler";
        switch (result) {
            case NOT_ALLOWED: return "Shop ist aktuell nicht aktiv";
            case INVALID_SHOP: return "Angebot ist leer";
            case OUT_OF_STOCK: return "Angebot ist ausverkauft";
            case NOT_ENOUGH_MONEY: return "Nicht genug Coins";
            case INVENTORY_FULL: return "Nicht genug Inventarplatz";
            case FAILED: return "Transaktion konnte nicht sicher gespeichert werden";
            default: return result.name();
        }
    }

    private String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS PLAYERSHOP");
        player.sendMessage(ChatColor.YELLOW + "/playershop kaufen" + ChatColor.GRAY + " - Haendler-Ei kaufen");
        player.sendMessage(ChatColor.YELLOW + "Shift + Rechtsklick" + ChatColor.GRAY + " - Besitzer-Menue");
        player.sendMessage(ChatColor.YELLOW + "Rechtsklick" + ChatColor.GRAY + " - echtes Villager-Handelsfenster");
        player.sendMessage(ChatColor.YELLOW + "/playershop claim | remove");
    }
}
