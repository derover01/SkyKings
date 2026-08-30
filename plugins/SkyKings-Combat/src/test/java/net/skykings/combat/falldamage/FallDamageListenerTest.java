package net.skykings.combat.falldamage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FallDamageListenerTest {

    private final FallDamageListener listener = new FallDamageListener();

    @Test
    public void fallDamageForPlayerIsCancelled() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        when(event.getEntity()).thenReturn(mock(Player.class));

        listener.onEntityDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void fallDamageForNonPlayerEntityIsUntouched() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        when(event.getEntity()).thenReturn(mock(Zombie.class));

        listener.onEntityDamage(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    public void otherDamageCausesForPlayersAreNotCancelled() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.LAVA);
        Entity player = mock(Player.class);
        when(event.getEntity()).thenReturn(player);

        listener.onEntityDamage(event);

        verify(event, never()).setCancelled(anyBoolean());
    }
}
