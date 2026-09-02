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
import java.util.Map;
import java.util.UUID;

/** 3x9 PlayerShop: jede Spalte ist ein Trade aus bis zu zwei Item-Stacks plus Coin-Preis. */
public final class PlayerShopTradeController implements Listener, CommandExecutor {
    private static final long EGG_PRICE = 2_500_000L;
    private static final String TRADE_TITLE = ChatColor.DARK_GRAY + "SkyKings | PlayerShop";
    private static final String SETUP_TITLE = ChatColor.DARK_GRAY + "SkyKings | Shop Setup";
    private final PlayerShopService service;
    private final PlayerShopStore store;
    private final PlayerShopEgg shopEgg = new PlayerShopEgg();
    private final Map<UUID, UUID> openShops = new HashMap<UUID, UUID>();

    public PlayerShopTradeController(PlayerShopService service) { this.service = service; this.store = service.getStore(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar."); return true; }
        Player player = (Player) sender;
        if (args.length == 0) { usage(player); return true; }
        String sub = args[0].toLowerCase();
        if ("kaufen".equals(sub) || "egg".equals(sub) || "buyegg".equals(sub)) return buyEgg(player);
        PlayerShop shop = nearestOwned(player, 7D);
        if (shop == null) { player.sendMessage(ChatColor.RED + "Kein eigener PlayerShop-Villager in der Naehe."); return true; }
        if ("menu".equals(sub) || "gui".equals(sub) || "setup".equals(sub) || "set".equals(sub) || "stock".equals(sub)) {
            openSetup(player, shop); return true;
        }
        if ("claim".equals(sub)) { claimRevenue(player, shop); return true; }
        if ("remove".equals(sub)) { removeOwnedShop(player, shop); return true; }
        usage(player); return true;
    }

    private boolean buyEgg(Player player) {
        SkyKingsCoreAPI core = core(); if (core == null) { player.sendMessage(ChatColor.RED + "Economy ist noch nicht bereit."); return true; }
        ItemStack egg = shopEgg.create();
        if (!canFit(player, egg)) { player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer das Haendler-Ei."); return true; }
        if (!core.getEconomyService().withdraw(player.getUniqueId(), EGG_PRICE, player.getName(), "PlayerShop Haendler-Ei")) {
            player.sendMessage(ChatColor.RED + "Dir fehlen Coins. Preis: " + ChatColor.YELLOW + format(EGG_PRICE) + " Coins"); return true;
        }
        if (!player.getInventory().addItem(egg).isEmpty()) {
            core.getEconomyService().deposit(player.getUniqueId(), EGG_PRICE, "PLAYER_SHOP_EGG_ROLLBACK", "Ei-Zustellung fehlgeschlagen");
            player.sendMessage(ChatColor.RED + "Ei konnte nicht zugestellt werden. Coins wurden erstattet."); return true;
        }
        player.updateInventory(); player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.4F);
        player.sendMessage(ChatColor.GREEN + "Haendler-Ei gekauft. Rechtsklick auf einen Block zum Platzieren."); return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || !shopEgg.isShopEgg(event.getItem())) return;
        event.setCancelled(true); Player player = event.getPlayer();
        Location location = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5D, 0D, 0.5D);
        PlayerShop shop = service.create(player, location);
        if (shop == null) { player.sendMessage(ChatColor.RED + "Das Haendler-Ei kann nur auf deiner eigenen Insel/deinem Plot platziert werden."); return; }
        Villager villager;
        try { villager = player.getWorld().spawn(location, Villager.class); }
        catch (RuntimeException ex) { store.deleteChecked(shop.getId()); player.sendMessage(ChatColor.RED + "Shop konnte nicht erstellt werden; Ei bleibt erhalten."); return; }
        villager.setCustomName(ChatColor.GOLD.toString() + ChatColor.BOLD + "Shop " + ChatColor.YELLOW + "von " + player.getName()); villager.setCustomNameVisible(true);
        shop.setVillagerUuid(villager.getUniqueId());
        if (!store.saveChecked()) { shop.setVillagerUuid(null); villager.remove(); store.deleteChecked(shop.getId()); player.sendMessage(ChatColor.RED + "Shop konnte nicht gespeichert werden; Ei bleibt erhalten."); return; }
        consumeHand(player); player.playSound(location, Sound.LEVEL_UP, 0.8F, 1.35F);
        player.sendMessage(ChatColor.GREEN + "PlayerShop platziert. " + ChatColor.YELLOW + "Shift + Rechtsklick" + ChatColor.GRAY + " zum Befuellen.");
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        PlayerShop shop = store.getByVillager(event.getRightClicked().getUniqueId()); if (shop == null) return;
        event.setCancelled(true); Player player = event.getPlayer();
        if (!isNearStoredLocation(shop, event.getRightClicked().getLocation())) { player.sendMessage(ChatColor.RED + "Dieser PlayerShop wurde verschoben und ist deaktiviert."); return; }
        if (player.getUniqueId().equals(shop.getOwner()) && player.isSneaking()) openSetup(player, shop); else openTrade(player, shop);
    }

    private void openTrade(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(null, 27, TRADE_TITLE);
        for (int i = 0; i < PlayerShop.MAX_OFFERS; i++) {
            PlayerShopOffer offer = shop.getOffer(i);
            ItemStack top = offer.topStack(), middle = offer.middleStack();
            if (top != null) inv.setItem(i, top); else inv.setItem(i, emptyPane(i + 1));
            if (middle != null) inv.setItem(9 + i, middle); else inv.setItem(9 + i, emptyPane(i + 1));
            inv.setItem(18 + i, priceItem(offer, i, false));
        }
        openShops.put(player.getUniqueId(), shop.getId()); player.openInventory(inv); player.playSound(player.getLocation(), Sound.VILLAGER_YES, 0.55F, 1.2F);
    }

    private void openSetup(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(null, 27, SETUP_TITLE);
        for (int i = 0; i < PlayerShop.MAX_OFFERS; i++) {
            PlayerShopOffer offer = shop.getOffer(i);
            ItemStack top = offer.topStack(), middle = offer.middleStack();
            inv.setItem(i, top == null ? setupEmpty(i + 1, "oben") : top);
            inv.setItem(9 + i, middle == null ? setupEmpty(i + 1, "mitte") : middle);
            inv.setItem(18 + i, priceItem(offer, i, true));
        }
        openShops.put(player.getUniqueId(), shop.getId()); player.openInventory(inv); player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5F, 1.2F);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView() == null || !(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle(); if (!TRADE_TITLE.equals(title) && !SETUP_TITLE.equals(title)) return;
        event.setCancelled(true); Player player = (Player) event.getWhoClicked(); UUID shopId = openShops.get(player.getUniqueId()); PlayerShop shop = shopId == null ? null : store.get(shopId);
        if (shop == null) { player.closeInventory(); return; }
        int raw = event.getRawSlot(); if (raw < 0 || raw >= 27) return;
        int column = raw % 9;
        if (TRADE_TITLE.equals(title)) {
            if (raw < 18) return;
            if (player.getUniqueId().equals(shop.getOwner())) { player.sendMessage(ChatColor.YELLOW + "Du kannst deinen eigenen Shop nicht kaufen. Shift + Rechtsklick zum Bearbeiten."); return; }
            PlayerShopOffer offer = shop.getOffer(column); long price = offer == null ? 0L : offer.getPriceCoins(); int amount = offer == null ? 0 : offer.getTotalAmount(); Material material = offer == null ? null : offer.getMaterial();
            PlayerShopService.Result result = service.purchase(player, shop.getId(), column);
            if (result == PlayerShopService.Result.SUCCESS) { player.sendMessage(ChatColor.GREEN + "Gekauft: " + amount + "x " + material.name() + " fuer " + format(price) + " Coins."); player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.25F); }
            else { player.sendMessage(ChatColor.RED + "Kauf nicht moeglich: " + readable(result)); player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.8F); }
            openTrade(player, shop); return;
        }
        if (!player.getUniqueId().equals(shop.getOwner())) { player.closeInventory(); return; }
        if (raw < 18) handleOfferSlot(player, shop, column, raw >= 9, event); else handlePrice(player, shop, column, event);
    }

    private void handleOfferSlot(Player player, PlayerShop shop, int column, boolean middle, InventoryClickEvent event) {
        PlayerShopOffer offer = shop.getOffer(column); int stored = middle ? offer.getAmountMiddle() : offer.getAmountTop(); ItemStack cursor = event.getCursor();
        if (stored > 0) {
            if (cursor != null && cursor.getType() != Material.AIR) { player.sendMessage(ChatColor.YELLOW + "Nimm den vorhandenen Stack zuerst heraus."); return; }
            ItemStack returned = service.takeOfferStack(player, shop.getId(), column, middle);
            if (returned == null) { player.sendMessage(ChatColor.RED + "Stack konnte nicht sicher entnommen werden."); return; }
            Map<Integer, ItemStack> left = player.getInventory().addItem(returned); if (!left.isEmpty()) for (ItemStack item : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.updateInventory(); player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 0.5F, 1.2F); openSetup(player, shop); return;
        }
        if (cursor == null || cursor.getType() == Material.AIR) { player.sendMessage(ChatColor.GRAY + "Nimm einen normalen Item-Stack auf den Mauszeiger und klicke hier."); return; }
        ItemStack deposit = cursor.clone();
        if (!service.putOfferStack(player, shop.getId(), column, middle, deposit)) { player.sendMessage(ChatColor.RED + "Nur gleiche, normale Items ohne Custom-Meta/Enchantments koennen gemeinsam in eine Trade-Spalte."); return; }
        event.setCursor(new ItemStack(Material.AIR));
        if (shop.getOffer(column).getPriceCoins() <= 0L) service.setOfferPrice(player, shop.getId(), column, 10_000L);
        player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.35F); openSetup(player, shop);
    }

    private void handlePrice(Player player, PlayerShop shop, int column, InventoryClickEvent event) {
        PlayerShopOffer offer = shop.getOffer(column); if (offer == null || offer.getTotalAmount() <= 0) { player.sendMessage(ChatColor.RED + "Lege zuerst Items in diese Spalte."); return; }
        long current = Math.max(1L, offer.getPriceCoins()); long step = event.isShiftClick() ? 100_000L : 10_000L; long next;
        if (event.isRightClick()) next = current <= step ? 1L : current - step; else next = current > Long.MAX_VALUE - step ? Long.MAX_VALUE : current + step;
        if (!service.setOfferPrice(player, shop.getId(), column, next)) { player.sendMessage(ChatColor.RED + "Preis konnte nicht gespeichert werden."); return; }
        player.playSound(player.getLocation(), Sound.CLICK, 0.4F, event.isRightClick() ? 0.9F : 1.4F); openSetup(player, shop);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView() == null) return; String title = event.getView().getTitle();
        if (TRADE_TITLE.equals(title) || SETUP_TITLE.equals(title)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) { if (event.getEntity() instanceof Villager && store.getByVillager(event.getEntity().getUniqueId()) != null) event.setCancelled(true); }

    private ItemStack priceItem(PlayerShopOffer offer, int index, boolean setup) {
        boolean active = offer != null && offer.getTotalAmount() > 0;
        String action = setup ? ChatColor.YELLOW + "Linksklick +10k | Rechtsklick -10k" : ChatColor.GREEN + "Klicken zum Kaufen";
        String shift = setup ? ChatColor.DARK_GRAY + "Shift: +/-100k" : ChatColor.DARK_GRAY + "Angebot " + (index + 1);
        return item(Material.NETHER_STAR, (short) 0,
                active ? ChatColor.GOLD + format(Math.max(1L, offer.getPriceCoins())) + " Coins" : ChatColor.DARK_GRAY + "Angebot " + (index + 1) + " leer",
                active ? ChatColor.GRAY + "Menge gesamt: " + ChatColor.WHITE + offer.getTotalAmount() : ChatColor.GRAY + "Oben/Mitte Items einstellen",
                action, shift);
    }

    private ItemStack emptyPane(int index) { return item(Material.STAINED_GLASS_PANE, (short) 15, ChatColor.DARK_GRAY + "Angebot " + index + " leer"); }
    private ItemStack setupEmpty(int index, String row) { return item(Material.STAINED_GLASS_PANE, (short) 7, ChatColor.GRAY + "Angebot " + index + " - " + row, ChatColor.YELLOW + "Item-Stack auf Cursor nehmen", ChatColor.YELLOW + "und hier klicken"); }

    private void claimRevenue(Player player, PlayerShop shop) {
        try { long amount = service.claimRevenue(player, shop.getId()); player.sendMessage(amount > 0 ? ChatColor.GREEN + "Du hast " + format(amount) + " Coins Einnahmen abgeholt." : ChatColor.YELLOW + "Keine Einnahmen vorhanden."); }
        catch (PlayerShopService.RevenueClaimOverflowException ex) { player.sendMessage(ChatColor.RED + "Dein Coin-Kontostand ist zu hoch fuer diese Auszahlung."); }
    }

    private void removeOwnedShop(Player player, PlayerShop shop) {
        if (!offersEmpty(shop) || shop.getPendingRevenue() > 0L) { player.sendMessage(ChatColor.RED + "Nimm zuerst alle angebotenen Items heraus und hole deine Einnahmen ab."); return; }
        ItemStack egg = shopEgg.create(); if (!canFit(player, egg)) { player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer das Haendler-Ei."); return; }
        if (!store.deleteChecked(shop.getId())) { player.sendMessage(ChatColor.RED + "Shop konnte nicht sicher entfernt werden."); return; }
        removeVillager(shop.getVillagerUuid()); openShops.remove(player.getUniqueId());
        if ((player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR)) player.setItemInHand(egg); else player.getInventory().addItem(egg);
        player.updateInventory(); player.sendMessage(ChatColor.GREEN + "PlayerShop entfernt. Dein Haendler-Ei wurde zurueckgegeben.");
    }

    private boolean offersEmpty(PlayerShop shop) { for (PlayerShopOffer offer : shop.getOffers()) if (offer.getTotalAmount() > 0) return false; return true; }
    private ItemStack item(Material material, short data, String name, String... lore) { ItemStack stack = new ItemStack(material, 1, data); ItemMeta meta = stack.getItemMeta(); if (meta != null) { meta.setDisplayName(name); if (lore != null) meta.setLore(Arrays.asList(lore)); stack.setItemMeta(meta); } return stack; }
    private boolean canFit(Player player, ItemStack item) { Inventory temp = Bukkit.createInventory(null, 36); for (int i = 0; i < 36; i++) { ItemStack current = player.getInventory().getItem(i); if (current != null) temp.setItem(i, current.clone()); } return temp.addItem(item.clone()).isEmpty(); }
    private void consumeHand(Player player) { ItemStack hand = player.getItemInHand(); if (hand == null) return; if (hand.getAmount() <= 1) player.setItemInHand(new ItemStack(Material.AIR)); else hand.setAmount(hand.getAmount() - 1); player.updateInventory(); }
    private SkyKingsCoreAPI core() { RegisteredServiceProvider<SkyKingsCoreAPI> reg = Bukkit.getServicesManager().getRegistration(SkyKingsCoreAPI.class); return reg == null ? null : reg.getProvider(); }
    private boolean isNearStoredLocation(PlayerShop shop, Location current) { if (shop.getWorld() == null || current == null || current.getWorld() == null || !shop.getWorld().equals(current.getWorld().getName())) return false; double dx=current.getX()-shop.getX(),dy=current.getY()-shop.getY(),dz=current.getZ()-shop.getZ(); return dx*dx+dy*dy+dz*dz<=16D; }
    private PlayerShop nearestOwned(Player player, double radius) { double best=radius*radius; PlayerShop result=null; for (Entity entity:player.getNearbyEntities(radius,radius,radius)) { if (!(entity instanceof Villager)) continue; PlayerShop shop=store.getByVillager(entity.getUniqueId()); if (shop==null||!player.getUniqueId().equals(shop.getOwner())) continue; double d=entity.getLocation().distanceSquared(player.getLocation()); if(d<best){best=d;result=shop;} } return result; }
    private void removeVillager(UUID uuid) { if (uuid==null)return; for(World world:Bukkit.getWorlds()) for(Entity entity:world.getEntities()) if(uuid.equals(entity.getUniqueId())){entity.remove();return;} }
    private String readable(PlayerShopService.Result result) { switch(result){case NOT_ENOUGH_MONEY:return "zu wenig Coins";case INVENTORY_FULL:return "Inventar voll";case NOT_ALLOWED:return "Shop aktuell nicht aktiv";case INVALID_SHOP:return "Angebot leer/ungueltig";default:return "Transaktion fehlgeschlagen";} }
    private String format(long value) { return String.format("%,d", value).replace(',', '.'); }
    private void usage(Player player) { player.sendMessage(ChatColor.GOLD.toString()+ChatColor.BOLD+"SKYKINGS PLAYERSHOP"); player.sendMessage(ChatColor.YELLOW+"/playershop kaufen"+ChatColor.GRAY+" - Haendler-Ei kaufen"); player.sendMessage(ChatColor.YELLOW+"Shift + Rechtsklick"+ChatColor.GRAY+" - 3x9 Shop Setup"); player.sendMessage(ChatColor.YELLOW+"Rechtsklick"+ChatColor.GRAY+" - Handelsfenster"); player.sendMessage(ChatColor.YELLOW+"/playershop claim | remove"); }
}
