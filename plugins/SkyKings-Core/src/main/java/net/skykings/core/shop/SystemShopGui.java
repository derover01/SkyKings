package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.permission.VoucherPermission;
import net.skykings.core.permission.VoucherPermissionService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Premium System-Shop: Kategorien -> Progressionsseiten statt einer flachen Item-Wand. */
public final class SystemShopGui {
    private final GuiManager guiManager;
    private final ShopTransactionService transactions;
    private final EconomyService economy;
    private final VoucherPermissionService rights;

    public SystemShopGui(GuiManager guiManager, ShopTransactionService transactions,
                         EconomyService economy, VoucherPermissionService rights) {
        this.guiManager = guiManager;
        this.transactions = transactions;
        this.economy = economy;
        this.rights = rights;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Shop | Kategorien"), 54);
        gui.setItem(4, UiItems.item(Material.EMERALD,
                UiTheme.PRIMARY + "SKYKINGS MARKET",
                UiTheme.MUTED + "Erschaffe dir deine Ausruestung.",
                UiTheme.WARNING + "Starke Items sind bewusst teuer.",
                UiTheme.MUTED + "Erspielen soll wertvoll bleiben."));

        category(gui, 10, Material.DIAMOND, ChatColor.GOLD + "Raenge", "Free-Rang-Progression", p -> p.performCommand("raenge"));
        category(gui, 12, Material.SKULL_ITEM, ChatColor.LIGHT_PURPLE + "Crates", "Crate & Reward Center", p -> p.performCommand("craterewards"));
        category(gui, 14, Material.PAPER, ChatColor.YELLOW + "Rechte", "Permanente Komfort-Rechte", this::openRights);
        category(gui, 16, Material.DIAMOND_SWORD, ChatColor.RED + "Schwerter", "Normal bis Sharpness XIII", this::openSwords);

        category(gui, 28, Material.BOW, ChatColor.GOLD + "Boegen", "Power Progression", this::openBows);
        category(gui, 30, Material.DIAMOND_CHESTPLATE, ChatColor.AQUA + "Ruestung", "PvP-Ruestung & OP-Tiers", this::openArmor);
        category(gui, 32, Material.ENDER_PEARL, ChatColor.DARK_AQUA + "Enderperlen", "Kleine bis grosse Stacks", this::openPearls);
        category(gui, 34, Material.COOKED_BEEF, ChatColor.GREEN + "Essen", "Food & Golden Apples", this::openFood);

        category(gui, 38, Material.POTION, ChatColor.LIGHT_PURPLE + "Potions", "PvP-Verbrauchsgueter", this::openPotions);
        category(gui, 40, Material.EXP_BOTTLE, ChatColor.WHITE + "Utility", "XP, Pfeile, Obsidian", this::openUtility);

        gui.setItem(49, UiItems.item(Material.NETHER_STAR,
                UiTheme.TEXT + "Dein Guthaben",
                UiTheme.MUTED + "Coins " + UiTheme.TEXT + UiFormat.number(transactions.getCoinBalance(player.getUniqueId())),
                UiTheme.MUTED + "Physische Sterne " + UiTheme.TEXT + transactions.countNetherstars(player.getInventory())));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openSwords(Player p) {
        GuiSession gui = page(p, "Schwerter", Material.DIAMOND_SWORD, "Von Einstieg bis Prestige-Waffe");
        add(gui,p,10,offer("sword-iron", enchanted(Material.IRON_SWORD,"&fStarter Sword",1,1,0),150000L),"SYSTEM_SWORDS");
        add(gui,p,11,offer("sword-d1", enchanted(Material.DIAMOND_SWORD,"&aPvP Sword",1,2,0),450000L),"SYSTEM_SWORDS");
        add(gui,p,12,offer("sword-d3", enchanted(Material.DIAMOND_SWORD,"&bElite Sword",3,3,1),1750000L),"SYSTEM_SWORDS");
        add(gui,p,13,offer("sword-d5", enchanted(Material.DIAMOND_SWORD,"&dTitan Sword",5,3,2),5500000L),"SYSTEM_SWORDS");
        add(gui,p,14,offer("sword-d7", enchanted(Material.DIAMOND_SWORD,"&6Legend Blade",7,5,2),15000000L),"SYSTEM_SWORDS");
        add(gui,p,15,offer("sword-d10", enchanted(Material.DIAMOND_SWORD,"&cOverlord Blade",10,7,2),45000000L),"SYSTEM_SWORDS");
        add(gui,p,16,offer("sword-d13", enchanted(Material.DIAMOND_SWORD,"&5&lMYTHIC SHARPNESS XIII",13,10,3),125000000L),"SYSTEM_SWORDS");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openBows(Player p) {
        GuiSession gui = page(p,"Boegen",Material.BOW,"Power steigt - Preis explodiert");
        add(gui,p,10,offer("bow-p1", bow("&fStarter Bow",1,0,0),200000L),"SYSTEM_BOWS");
        add(gui,p,11,offer("bow-p3", bow("&aHunter Bow",3,1,0),900000L),"SYSTEM_BOWS");
        add(gui,p,12,offer("bow-p5", bow("&bElite Bow",5,1,1),3500000L),"SYSTEM_BOWS");
        add(gui,p,13,offer("bow-p7", bow("&dTitan Bow",7,2,1),12000000L),"SYSTEM_BOWS");
        add(gui,p,14,offer("bow-p10", bow("&6&lRoyal Bow",10,3,1),40000000L),"SYSTEM_BOWS");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openArmor(Player p) {
        GuiSession gui = page(p,"Ruestung",Material.DIAMOND_CHESTPLATE,"Einzelteile - keine billigen Komplettsets");
        add(gui,p,10,offer("armor-helm-p2", armor(Material.DIAMOND_HELMET,"&aGuardian Helm",2,2),650000L),"SYSTEM_ARMOR");
        add(gui,p,11,offer("armor-chest-p2", armor(Material.DIAMOND_CHESTPLATE,"&aGuardian Chest",2,2),950000L),"SYSTEM_ARMOR");
        add(gui,p,12,offer("armor-legs-p2", armor(Material.DIAMOND_LEGGINGS,"&aGuardian Legs",2,2),850000L),"SYSTEM_ARMOR");
        add(gui,p,13,offer("armor-boots-p2", armor(Material.DIAMOND_BOOTS,"&aGuardian Boots",2,2),600000L),"SYSTEM_ARMOR");
        add(gui,p,28,offer("armor-helm-p4", armor(Material.DIAMOND_HELMET,"&dTitan Helm",4,5),5500000L),"SYSTEM_ARMOR");
        add(gui,p,29,offer("armor-chest-p4", armor(Material.DIAMOND_CHESTPLATE,"&dTitan Chest",4,5),8500000L),"SYSTEM_ARMOR");
        add(gui,p,30,offer("armor-legs-p4", armor(Material.DIAMOND_LEGGINGS,"&dTitan Legs",4,5),7500000L),"SYSTEM_ARMOR");
        add(gui,p,31,offer("armor-boots-p4", armor(Material.DIAMOND_BOOTS,"&dTitan Boots",4,5),5000000L),"SYSTEM_ARMOR");
        add(gui,p,34,offer("armor-chest-p8", armor(Material.DIAMOND_CHESTPLATE,"&5&lMYTHIC CHEST",8,10),75000000L),"SYSTEM_ARMOR");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openPearls(Player p) {
        GuiSession gui = page(p,"Enderperlen",Material.ENDER_PEARL,"Mobilitaet bleibt ein echter Coin-Sink");
        add(gui,p,11,new ShopOffer("pearls-8",new ItemStack(Material.ENDER_PEARL,8),ShopCurrency.COINS,140000L),"SYSTEM_PEARLS");
        add(gui,p,13,new ShopOffer("pearls-16",new ItemStack(Material.ENDER_PEARL,16),ShopCurrency.COINS,275000L),"SYSTEM_PEARLS");
        add(gui,p,15,new ShopOffer("pearls-32",new ItemStack(Material.ENDER_PEARL,32),ShopCurrency.COINS,600000L),"SYSTEM_PEARLS");
        add(gui,p,31,new ShopOffer("pearls-64",new ItemStack(Material.ENDER_PEARL,64),ShopCurrency.COINS,1350000L),"SYSTEM_PEARLS");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openFood(Player p) {
        GuiSession gui = page(p,"Essen",Material.COOKED_BEEF,"Grundversorgung bis PvP-Luxus");
        add(gui,p,11,new ShopOffer("food-steak-16",new ItemStack(Material.COOKED_BEEF,16),ShopCurrency.COINS,35000L),"SYSTEM_FOOD");
        add(gui,p,12,new ShopOffer("food-steak-64",new ItemStack(Material.COOKED_BEEF,64),ShopCurrency.COINS,120000L),"SYSTEM_FOOD");
        add(gui,p,14,new ShopOffer("food-gapples-8",new ItemStack(Material.GOLDEN_APPLE,8),ShopCurrency.COINS,350000L),"SYSTEM_FOOD");
        add(gui,p,15,new ShopOffer("food-opgaps-2",new ItemStack(Material.GOLDEN_APPLE,2,(short)1),ShopCurrency.COINS,900000L),"SYSTEM_FOOD");
        add(gui,p,31,new ShopOffer("food-opgaps-8",new ItemStack(Material.GOLDEN_APPLE,8,(short)1),ShopCurrency.COINS,4200000L),"SYSTEM_FOOD");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openPotions(Player p) {
        GuiSession gui = page(p,"Potions",Material.POTION,"Verbrauchsgueter sind absichtlich nicht billig");
        add(gui,p,11,new ShopOffer("pot-heal2-4",new ItemStack(Material.POTION,4,(short)16421),ShopCurrency.COINS,400000L),"SYSTEM_POTIONS");
        add(gui,p,13,new ShopOffer("pot-speed2-4",new ItemStack(Material.POTION,4,(short)8226),ShopCurrency.COINS,450000L),"SYSTEM_POTIONS");
        add(gui,p,15,new ShopOffer("pot-strength2-4",new ItemStack(Material.POTION,4,(short)8233),ShopCurrency.COINS,650000L),"SYSTEM_POTIONS");
        add(gui,p,29,new ShopOffer("pot-heal2-12",new ItemStack(Material.POTION,12,(short)16421),ShopCurrency.COINS,1150000L),"SYSTEM_POTIONS");
        add(gui,p,31,new ShopOffer("pot-speed2-12",new ItemStack(Material.POTION,12,(short)8226),ShopCurrency.COINS,1300000L),"SYSTEM_POTIONS");
        add(gui,p,33,new ShopOffer("pot-strength2-12",new ItemStack(Material.POTION,12,(short)8233),ShopCurrency.COINS,1850000L),"SYSTEM_POTIONS");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openUtility(Player p) {
        GuiSession gui = page(p,"Utility",Material.EXP_BOTTLE,"Restock ohne Progression zu entwerten");
        add(gui,p,10,new ShopOffer("util-arrows",new ItemStack(Material.ARROW,64),ShopCurrency.COINS,85000L),"SYSTEM_UTILITY");
        add(gui,p,12,new ShopOffer("util-xp",new ItemStack(Material.EXP_BOTTLE,32),ShopCurrency.COINS,250000L),"SYSTEM_UTILITY");
        add(gui,p,14,new ShopOffer("util-obsidian",new ItemStack(Material.OBSIDIAN,32),ShopCurrency.COINS,300000L),"SYSTEM_UTILITY");
        add(gui,p,16,new ShopOffer("util-anvil",new ItemStack(Material.ANVIL,1),ShopCurrency.COINS,750000L),"SYSTEM_UTILITY");
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void openRights(Player p) {
        GuiSession gui = page(p,"Rechte",Material.PAPER,"Permanente Rechte - einmal kaufen");
        int[] slots = {10,12,14,16,28,30,32,34};
        int index = 0;
        for (VoucherPermission right : rights.getAll()) {
            if (index >= slots.length) break;
            final VoucherPermission selected = right;
            final long price = rightPrice(right.getId());
            boolean owned = p.hasPermission(right.getNode());
            List<String> lore = new ArrayList<String>();
            lore.add(UiTheme.MUTED + "Permanent fuer diesen Account");
            lore.add(UiTheme.MUTED + "Preis " + UiTheme.TEXT + UiFormat.number(price) + " Coins");
            lore.add(owned ? UiTheme.SUCCESS + "BEREITS FREIGESCHALTET" : UiItems.action("Klicken zum Kaufen"));
            gui.setItem(slots[index++], UiItems.item(Material.PAPER,
                    (owned ? UiTheme.SUCCESS : UiTheme.TEXT) + right.getDisplayName(), lore.toArray(new String[lore.size()])),
                    owned ? null : (player,event,slot) -> confirmRight(player, selected, price));
        }
        nav(gui); guiManager.open(gui); SoundFeedback.menuOpen(p);
    }

    private void confirmRight(final Player p, final VoucherPermission right, final long price) {
        GuiSession gui = GuiSession.create(p, UiTheme.title("Recht kaufen?"),27);
        gui.setItem(13,UiItems.item(Material.PAPER,UiTheme.TEXT + right.getDisplayName(),
                UiTheme.MUTED + "Permanent",UiTheme.MUTED + "Preis " + UiTheme.TEXT + UiFormat.number(price) + " Coins"));
        gui.setItem(11,UiItems.item(Material.EMERALD_BLOCK,UiTheme.SUCCESS + "KAUFEN",UiItems.action("Bestaetigen")),(player,e,s)->{
            if (player.hasPermission(right.getNode())) { player.closeInventory(); openRights(player); return; }
            if (!economy.withdraw(player.getUniqueId(),price,"SHOP_RIGHT","Recht " + right.getId())) {
                player.sendMessage(UiTheme.DANGER + "Nicht genug Coins."); SoundFeedback.error(player); return;
            }
            VoucherPermissionService.GrantStatus status = rights.grant(player.getUniqueId(),right.getId(),"SHOP_RIGHT");
            if (status != VoucherPermissionService.GrantStatus.GRANTED) {
                economy.deposit(player.getUniqueId(),price,"SHOP_RIGHT_ROLLBACK","Rollback Recht " + right.getId());
                player.sendMessage(UiTheme.DANGER + "Recht konnte nicht vergeben werden. Coins wurden erstattet.");
                SoundFeedback.error(player); return;
            }
            player.closeInventory(); player.sendMessage(UiTheme.SUCCESS + "Recht permanent freigeschaltet: " + right.getDisplayName());
            SoundFeedback.reward(player); openRights(player);
        });
        gui.setItem(15,UiItems.item(Material.REDSTONE_BLOCK,UiTheme.DANGER + "ABBRECHEN",UiTheme.MUTED + "Keine Coins werden verwendet."),(player,e,s)->openRights(player));
        guiManager.open(gui); SoundFeedback.confirm(p);
    }

    private long rightPrice(String id) {
        if (id == null) return 10000000L;
        String key = id.toLowerCase();
        if (key.contains("fly")) return 25000000L;
        if (key.contains("repair")) return 18000000L;
        if (key.contains("stack")) return 12000000L;
        if (key.contains("bloecke")) return 15000000L;
        return 10000000L;
    }

    private GuiSession page(Player p,String title,Material icon,String subtitle) {
        GuiSession gui = GuiSession.create(p,UiTheme.title("Shop | " + title),54);
        gui.setItem(4,UiItems.item(icon,UiTheme.PRIMARY + title,UiTheme.MUTED + subtitle,
                UiTheme.MUTED + "Coins " + UiTheme.TEXT + UiFormat.number(transactions.getCoinBalance(p.getUniqueId()))));
        return gui;
    }

    private void nav(GuiSession gui) {
        gui.setItem(45,UiItems.item(Material.ARROW,UiTheme.TEXT + "Zurueck",UiItems.action("Shop-Kategorien")),(p,e,s)->open(p));
        gui.setItem(49,UiItems.item(Material.EMERALD,UiTheme.PRIMARY + "Market Hub",UiItems.action("Kategorien")),(p,e,s)->open(p));
    }

    private void category(GuiSession gui,int slot,Material material,String title,String description,OpenAction action) {
        gui.setItem(slot,UiItems.item(material,title,UiTheme.MUTED + description,UiItems.action("Kategorie oeffnen")),(p,e,s)->action.open(p));
    }

    private ShopOffer offer(String id,ItemStack item,long price) { return new ShopOffer(id,item,ShopCurrency.COINS,price); }

    private ItemStack enchanted(Material material,String name,int sharpness,int unbreaking,int fire) {
        ItemStack item = new ItemStack(material);
        if (sharpness > 0) item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL,sharpness);
        if (unbreaking > 0) item.addUnsafeEnchantment(Enchantment.DURABILITY,unbreaking);
        if (fire > 0) item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT,fire);
        name(item,name); return item;
    }

    private ItemStack bow(String name,int power,int punch,int flame) {
        ItemStack item = new ItemStack(Material.BOW);
        if (power > 0) item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE,power);
        if (punch > 0) item.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK,punch);
        if (flame > 0) item.addUnsafeEnchantment(Enchantment.ARROW_FIRE,flame);
        item.addUnsafeEnchantment(Enchantment.DURABILITY,Math.max(1,power/2)); name(item,name); return item;
    }

    private ItemStack armor(Material material,String name,int protection,int unbreaking) {
        ItemStack item = new ItemStack(material);
        item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,protection);
        item.addUnsafeEnchantment(Enchantment.DURABILITY,unbreaking);
        name(item,name); return item;
    }

    private void name(ItemStack item,String raw) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',raw));
        item.setItemMeta(meta);
    }

    private void add(GuiSession gui, Player player, int slot, ShopOffer offer, String shopId) {
        ItemStack icon = offer.getItem();
        ItemMeta meta = icon.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        long balance = offer.getCurrency() == ShopCurrency.COINS ? transactions.getCoinBalance(player.getUniqueId())
                : transactions.countNetherstars(player.getInventory());
        String unit = offer.getCurrency() == ShopCurrency.COINS ? " Coins" : " Sterne";
        lore.add(UiTheme.MUTED + "Preis " + UiTheme.TEXT + UiFormat.number(offer.getPrice()) + unit);
        lore.add(UiTheme.MUTED + "Dein Guthaben " + UiTheme.TEXT + UiFormat.number(balance) + unit);
        if (balance < offer.getPrice()) lore.add(UiTheme.DANGER + "Dir fehlen " + UiFormat.number(offer.getPrice()-balance) + unit + ".");
        else lore.add(UiItems.action("Klicken zum Kaufen"));
        meta.setLore(lore); icon.setItemMeta(meta);
        gui.setItem(slot,icon,(p,event,clicked)->{
            ShopPurchaseResult result = transactions.purchase(p,offer,shopId);
            sendResult(p,result,offer);
            if (result == ShopPurchaseResult.SUCCESS) reopenCategory(p,shopId);
        });
    }

    private void reopenCategory(Player p,String shopId) {
        if (shopId.endsWith("SWORDS")) openSwords(p);
        else if (shopId.endsWith("BOWS")) openBows(p);
        else if (shopId.endsWith("ARMOR")) openArmor(p);
        else if (shopId.endsWith("PEARLS")) openPearls(p);
        else if (shopId.endsWith("FOOD")) openFood(p);
        else if (shopId.endsWith("POTIONS")) openPotions(p);
        else openUtility(p);
    }

    private void sendResult(Player player, ShopPurchaseResult result, ShopOffer offer) {
        switch (result) {
            case SUCCESS:
                player.sendMessage(UiTheme.SUCCESS + "Kauf erfolgreich: " + UiFormat.number(offer.getPrice())
                        + (offer.getCurrency()==ShopCurrency.COINS ? " Coins" : " Sterne")); SoundFeedback.success(player); break;
            case NOT_ENOUGH_MONEY: player.sendMessage(UiTheme.DANGER + "Nicht genug Coins."); SoundFeedback.error(player); break;
            case NOT_ENOUGH_NETHERSTARS: player.sendMessage(UiTheme.DANGER + "Nicht genug SkyKings Sterne."); SoundFeedback.error(player); break;
            case INVENTORY_FULL: player.sendMessage(UiTheme.DANGER + "Inventar voll."); SoundFeedback.error(player); break;
            default: player.sendMessage(UiTheme.DANGER + "Kauf nicht moeglich."); SoundFeedback.error(player); break;
        }
    }

    private interface OpenAction { void open(Player player); }
}
