package net.skykings.core.trade;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** In-Memory-Angebot einer Handelsseite. */
public final class TradeOffer {
    private final UUID player;
    private final List<ItemStack> items = new ArrayList<ItemStack>();
    private long coins;
    private boolean accepted;

    public TradeOffer(UUID player) { this.player = player; }
    public UUID getPlayer() { return player; }
    public List<ItemStack> getItems() { return Collections.unmodifiableList(items); }
    public long getCoins() { return coins; }
    public void setCoins(long coins) { this.coins = Math.max(0L, coins); this.accepted = false; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public void setItems(List<ItemStack> source) {
        items.clear();
        if (source != null) {
            for (ItemStack item : source) if (item != null) items.add(item.clone());
        }
        accepted = false;
    }
}
