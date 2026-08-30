package net.skykings.combat.pearl;

import net.skykings.combat.util.MessageCooldownTracker;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Wichtig: {@link PlayerInteractEvent#isCancelled()} spiegelt fuer RIGHT_CLICK_AIR (kein
 * angeklickter Block) bereits VOR jeder Listener-Ausfuehrung "cancelled" wider (Bukkit setzt
 * {@code useInteractedBlock()} beim Konstruieren ohne Block auf DENY). Ob ein Item tatsaechlich
 * benutzt/geworfen wird, entscheidet stattdessen {@link PlayerInteractEvent#useItemInHand()} -
 * darauf pruefen diese Tests (siehe auch die Begruendung in {@code EnderpearlCooldownListener}).
 */
public class EnderpearlCooldownListenerTest {

    private static final long COOLDOWN_MILLIS = 150L;

    private FakeCooldownService cooldownService;
    private EnderpearlCooldownListener listener;
    private Player player;
    private UUID uuid;

    @Before
    public void setUp() {
        cooldownService = new FakeCooldownService();
        listener = new EnderpearlCooldownListener(cooldownService, COOLDOWN_MILLIS, new MessageCooldownTracker(0L));
        uuid = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
    }

    private PlayerInteractEvent pearlThrow() {
        ItemStack pearl = new ItemStack(Material.ENDER_PEARL, 1);
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, pearl, null, null);
    }

    @Test
    public void firstThrowIsAllowedAndStartsCooldown() {
        PlayerInteractEvent event = pearlThrow();

        listener.onInteract(event);

        assertNotEquals("Itembenutzung darf nicht verweigert werden", Event.Result.DENY, event.useItemInHand());
        assertTrue(cooldownService.isActive(uuid, EnderpearlCooldownListener.COOLDOWN_KEY));
    }

    @Test
    public void secondThrowWithinCooldownIsBlockedWithoutLosingThePearl() {
        listener.onInteract(pearlThrow());

        PlayerInteractEvent secondEvent = pearlThrow();
        ItemStack itemBeforeAndAfter = secondEvent.getItem();
        listener.onInteract(secondEvent);

        assertEquals("Der zweite Wurf muss die Itembenutzung verweigern", Event.Result.DENY, secondEvent.useItemInHand());
        // Cancelling PlayerInteractEvent verhindert den Item-Verbrauch komplett - das Item auf
        // dem Event bleibt unveraendert (kein manueller Abzug durch unseren Code). Bewusst ohne
        // ItemStack#isSimilar(...), da das ItemMeta-Vergleiche nutzt und einen echten Server
        // braucht (siehe ItemBuilderTest in SkyKings-Core fuer dieselbe Einschraenkung).
        assertEquals(Material.ENDER_PEARL, itemBeforeAndAfter.getType());
        assertEquals(1, itemBeforeAndAfter.getAmount());
    }

    @Test
    public void throwAfterCooldownExpiryIsAllowedAgain() throws InterruptedException {
        listener.onInteract(pearlThrow());
        Thread.sleep(200L);

        PlayerInteractEvent event = pearlThrow();
        listener.onInteract(event);

        assertNotEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    public void nonPearlItemIsIgnored() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD, 1);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, sword, null, null);

        listener.onInteract(event);

        assertNotEquals(Event.Result.DENY, event.useItemInHand());
        assertFalse(cooldownService.isActive(uuid, EnderpearlCooldownListener.COOLDOWN_KEY));
    }

    @Test
    public void leftClickIsIgnoredEvenWithPearlInHand() {
        ItemStack pearl = new ItemStack(Material.ENDER_PEARL, 1);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, pearl, null, null);

        listener.onInteract(event);

        assertFalse(cooldownService.isActive(uuid, EnderpearlCooldownListener.COOLDOWN_KEY));
    }
}
