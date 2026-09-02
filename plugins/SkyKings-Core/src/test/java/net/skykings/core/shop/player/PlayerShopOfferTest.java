package net.skykings.core.shop.player;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerShopOfferTest {

    @Test
    public void stackRowsAreBoundedToVanillaStackSize() {
        PlayerShopOffer offer = new PlayerShopOffer();
        offer.setAmountTop(999);
        offer.setAmountMiddle(65);

        assertEquals(64, offer.getAmountTop());
        assertEquals(64, offer.getAmountMiddle());
        assertEquals(128, offer.getTotalAmount());
    }

    @Test
    public void negativeAmountsAndPriceAreClampedToZero() {
        PlayerShopOffer offer = new PlayerShopOffer();
        offer.setMaterial(Material.DIAMOND);
        offer.setAmountTop(-5);
        offer.setAmountMiddle(-1);
        offer.setPriceCoins(-100L);

        assertEquals(0, offer.getAmountTop());
        assertEquals(0, offer.getAmountMiddle());
        assertEquals(0L, offer.getPriceCoins());
        assertFalse(offer.isConfigured());
    }

    @Test
    public void configuredOfferRequiresItemAmountAndPositivePrice() {
        PlayerShopOffer offer = new PlayerShopOffer();
        offer.setMaterial(Material.GOLDEN_APPLE);
        offer.setAmountTop(64);
        offer.setAmountMiddle(64);
        offer.setPriceCoins(2_000_000L);

        assertTrue(offer.isConfigured());
        assertEquals(128, offer.getTotalAmount());
    }
}
