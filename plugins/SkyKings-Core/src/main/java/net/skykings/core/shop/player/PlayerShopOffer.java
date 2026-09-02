package net.skykings.core.shop.player;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Ein einzelnes Angebot eines PlayerShop-Villagers (maximal zwei sichtbare Stacks + Coin-Preis). */
public final class PlayerShopOffer {
    private Material material;
    private short data;
    private int amountTop;
    private int amountMiddle;
    private long priceCoins;

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public short getData() { return data; }
    public void setData(short data) { this.data = data; }
    public int getAmountTop() { return amountTop; }
    public void setAmountTop(int amountTop) { this.amountTop = clampStack(amountTop); }
    public int getAmountMiddle() { return amountMiddle; }
    public void setAmountMiddle(int amountMiddle) { this.amountMiddle = clampStack(amountMiddle); }
    public long getPriceCoins() { return priceCoins; }
    public void setPriceCoins(long priceCoins) { this.priceCoins = Math.max(0L, priceCoins); }

    public int getTotalAmount() {
        return amountTop + amountMiddle;
    }

    public boolean isConfigured() {
        return material != null && getTotalAmount() > 0 && priceCoins > 0L;
    }

    public ItemStack topStack() {
        return material == null || amountTop <= 0 ? null : new ItemStack(material, amountTop, data);
    }

    public ItemStack middleStack() {
        return material == null || amountMiddle <= 0 ? null : new ItemStack(material, amountMiddle, data);
    }

    public void clear() {
        material = null;
        data = 0;
        amountTop = 0;
        amountMiddle = 0;
        priceCoins = 0L;
    }

    private int clampStack(int amount) {
        return Math.max(0, Math.min(64, amount));
    }
}
