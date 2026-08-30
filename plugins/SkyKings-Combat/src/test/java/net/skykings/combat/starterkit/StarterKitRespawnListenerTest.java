package net.skykings.combat.starterkit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StarterKitRespawnListenerTest {

    @Test
    public void respawnAppliesStarterKitToThePlayer() {
        DeathStarterKitService starterKitService = mock(DeathStarterKitService.class);
        StarterKitRespawnListener listener = new StarterKitRespawnListener(starterKitService);

        Player player = mock(Player.class);
        Location location = mock(Location.class);
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, location, false);

        listener.onRespawn(event);

        verify(starterKitService).apply(player);
    }
}
