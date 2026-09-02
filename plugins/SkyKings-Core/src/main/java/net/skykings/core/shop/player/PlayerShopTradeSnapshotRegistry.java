package net.skykings.core.shop.player;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kurzlebiger serverseitiger Snapshot der Angebote, die ein Spieler im echten
 * Villager-Handelsfenster gesehen hat. Verhindert, dass ein alter Client-Preview
 * nach einer Preis-/Stock-Aenderung ein inzwischen anderes Angebot kauft.
 *
 * Bukkit-Events und PlayerShopService laufen auf dem Main-Thread; synchronized
 * haelt den Zustand trotzdem eindeutig und testbar.
 */
final class PlayerShopTradeSnapshotRegistry {
    private static final Map<UUID, Snapshot> ACTIVE = new HashMap<UUID, Snapshot>();

    private PlayerShopTradeSnapshotRegistry() {}

    static synchronized void open(UUID playerId, PlayerShop shop) {
        if (playerId == null || shop == null) return;
        Map<Integer, String> signatures = new HashMap<Integer, String>();
        for (int i = 0; i < PlayerShop.MAX_OFFERS; i++) {
            PlayerShopOffer offer = shop.getOffer(i);
            if (offer != null && offer.isConfigured()) signatures.put(i, signature(offer));
        }
        ACTIVE.put(playerId, new Snapshot(shop.getId(), signatures));
    }

    static synchronized void close(UUID playerId) {
        if (playerId != null) ACTIVE.remove(playerId);
    }

    /**
     * Kein Snapshot bedeutet Legacy-/Command-Kaufpfad und wird nicht blockiert.
     * Existiert ein Merchant-Snapshot, muss Shop + Spalte exakt dem gesehenen
     * Zustand entsprechen.
     */
    static synchronized boolean matchesIfPresent(UUID playerId, UUID shopId, int offerIndex, PlayerShopOffer current) {
        Snapshot snapshot = ACTIVE.get(playerId);
        if (snapshot == null) return true;
        if (shopId == null || !shopId.equals(snapshot.shopId)) return false;
        String expected = snapshot.offerSignatures.get(offerIndex);
        return expected != null && current != null && expected.equals(signature(current));
    }

    static String signature(PlayerShopOffer offer) {
        if (offer == null || !offer.isConfigured()) return "EMPTY";
        Material material = offer.getMaterial();
        return (material == null ? "AIR" : material.name())
                + ':' + offer.getData()
                + ':' + offer.getAmountTop()
                + ':' + offer.getAmountMiddle()
                + ':' + offer.getPriceCoins();
    }

    private static final class Snapshot {
        private final UUID shopId;
        private final Map<Integer, String> offerSignatures;

        private Snapshot(UUID shopId, Map<Integer, String> offerSignatures) {
            this.shopId = shopId;
            this.offerSignatures = offerSignatures;
        }
    }
}
