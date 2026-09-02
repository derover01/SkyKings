package net.skykings.core.shop.player;

import org.bukkit.Material;

import java.util.UUID;

/** Persistente Definition eines spielereigenen Shops mit bis zu neun Angeboten. */
public final class PlayerShop {
    public static final int MAX_OFFERS = 9;

    private final UUID id;
    private final UUID owner;
    private UUID villagerUuid;
    private final PlayerShopOffer[] offers = new PlayerShopOffer[MAX_OFFERS];
    private long pendingRevenue;
    private String world;
    private double x;
    private double y;
    private double z;

    public PlayerShop(UUID id, UUID owner) {
        this.id = id;
        this.owner = owner;
        for (int i = 0; i < offers.length; i++) offers[i] = new PlayerShopOffer();
    }

    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public UUID getVillagerUuid() { return villagerUuid; }
    public void setVillagerUuid(UUID villagerUuid) { this.villagerUuid = villagerUuid; }
    public PlayerShopOffer getOffer(int index) { return index < 0 || index >= offers.length ? null : offers[index]; }
    public PlayerShopOffer[] getOffers() { return offers; }

    /* Legacy-Kompatibilitaet: bisherige API zeigt auf Angebot 1. */
    public Material getMaterial() { return offers[0].getMaterial(); }
    public void setMaterial(Material material) { offers[0].setMaterial(material); }
    public short getData() { return offers[0].getData(); }
    public void setData(short data) { offers[0].setData(data); }
    public int getAmountPerSale() { return offers[0].getTotalAmount(); }
    public void setAmountPerSale(int amountPerSale) {
        offers[0].setAmountTop(Math.max(0, Math.min(64, amountPerSale)));
        offers[0].setAmountMiddle(Math.max(0, amountPerSale - 64));
    }
    public long getPriceCoins() { return offers[0].getPriceCoins(); }
    public void setPriceCoins(long priceCoins) { offers[0].setPriceCoins(priceCoins); }
    public int getStock() { return offers[0].getTotalAmount(); }
    public void setStock(int stock) {
        offers[0].setAmountTop(Math.max(0, Math.min(64, stock)));
        offers[0].setAmountMiddle(Math.max(0, Math.min(64, stock - 64)));
    }

    public long getPendingRevenue() { return pendingRevenue; }
    public void setPendingRevenue(long pendingRevenue) { this.pendingRevenue = pendingRevenue; }
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public boolean isConfigured() {
        for (PlayerShopOffer offer : offers) if (offer.isConfigured()) return true;
        return false;
    }
}
