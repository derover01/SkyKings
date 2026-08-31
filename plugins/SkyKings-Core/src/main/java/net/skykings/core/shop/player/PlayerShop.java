package net.skykings.core.shop.player;

import org.bukkit.Material;

import java.util.UUID;

/** Persistente Definition eines spielereigenen Shops. */
public final class PlayerShop {
    private final UUID id;
    private final UUID owner;
    private UUID villagerUuid;
    private Material material;
    private short data;
    private int amountPerSale;
    private long priceCoins;
    private int stock;
    private long pendingRevenue;
    private String world;
    private double x;
    private double y;
    private double z;

    public PlayerShop(UUID id, UUID owner) {
        this.id = id;
        this.owner = owner;
    }

    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public UUID getVillagerUuid() { return villagerUuid; }
    public void setVillagerUuid(UUID villagerUuid) { this.villagerUuid = villagerUuid; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public short getData() { return data; }
    public void setData(short data) { this.data = data; }
    public int getAmountPerSale() { return amountPerSale; }
    public void setAmountPerSale(int amountPerSale) { this.amountPerSale = amountPerSale; }
    public long getPriceCoins() { return priceCoins; }
    public void setPriceCoins(long priceCoins) { this.priceCoins = priceCoins; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
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
        return material != null && amountPerSale > 0 && priceCoins > 0;
    }
}
