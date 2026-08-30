package net.skykings.combat.tag;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CombatFlyCommandListenerTest {

    @Test
    public void blocksFlyWhileTagged() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn("/fly");

        CombatTagService service = mock(CombatTagService.class);
        when(service.isTagged(uuid)).thenReturn(true);
        when(service.getRemainingMillis(uuid)).thenReturn(4500L);

        new CombatFlyCommandListener(service).onCommand(event);

        verify(event).setCancelled(true);
        verify(player).sendMessage(contains("Combat-Tag"));
    }

    @Test
    public void allowsFlyOutsideCombat() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn("/fly");

        CombatTagService service = mock(CombatTagService.class);
        when(service.isTagged(uuid)).thenReturn(false);

        new CombatFlyCommandListener(service).onCommand(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void ignoresOtherCommandsWhileTagged() {
        PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class);
        when(event.getMessage()).thenReturn("/kit king");

        CombatTagService service = mock(CombatTagService.class);
        new CombatFlyCommandListener(service).onCommand(event);

        verify(event, never()).setCancelled(true);
    }
}
