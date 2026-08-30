package net.skykings.combat.tag;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CombatFlyCommandListenerTest {

    @Test
    public void blocksFlyWhileTagged() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        CombatTagService service = mock(CombatTagService.class);
        when(service.isTagged(uuid)).thenReturn(true);
        when(service.getRemainingMillis(uuid)).thenReturn(4500L);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/fly");
        new CombatFlyCommandListener(service).onCommand(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(contains("Combat-Tag"));
    }

    @Test
    public void allowsFlyOutsideCombat() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        CombatTagService service = mock(CombatTagService.class);
        when(service.isTagged(uuid)).thenReturn(false);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/fly");
        new CombatFlyCommandListener(service).onCommand(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void ignoresOtherCommandsWhileTagged() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        CombatTagService service = mock(CombatTagService.class);
        when(service.isTagged(uuid)).thenReturn(true);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/kit king");
        new CombatFlyCommandListener(service).onCommand(event);

        assertFalse(event.isCancelled());
    }
}
