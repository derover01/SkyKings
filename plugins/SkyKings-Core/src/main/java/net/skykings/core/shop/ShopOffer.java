package net.skykings.core.shop;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** Unveränderliches Kaufangebot für System-, Villager- und spätere Player-Shops. */
public final class ShopOffer {
    private final String id;
    private final ItemStack item;
    private final ShopCurrency currency;
    private final long price;

    public ShopOffer(String id, ItemStack item, ShopCurrency currency, long price) {
        this.id = Objects.requireNonNull(id, "id");
        this.item = Objects.requireNonNull(item, "item").clone();
        this.currency = Objects.requireNonNull(currency, "currency");
        if (price <= 0L) throw new IllegalArgumentException("price muss > 0 sein");
        this.price = price;
    }

    public String getId() { return id; }
    public ItemStack getItem() { return item.clone(); }
    public ShopCurrency getCurrency() { return currency; }
    public long getPrice() { return price; }
}
