package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.retention.JackpotService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kompatibilitaets-Adapter fuer bestehende Jackpot-NPC-Bindungen.
 * Command und Villager verwenden bewusst denselben autoritativen JackpotService.
 */
public final class JackpotGui {
    private final JackpotService jackpotService;

    public JackpotGui(JavaPlugin plugin, GuiManager guiManager, EconomyService economyService) {
        JackpotService live = JackpotService.liveInstance();
        if (live == null) {
            throw new IllegalStateException("JackpotService muss vor dem Jackpot-NPC initialisiert werden.");
        }
        this.jackpotService = live;
    }

    public void open(Player player) {
        jackpotService.open(player);
    }
}
