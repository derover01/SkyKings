package net.skykings.combat.cosmetic;

import net.skykings.combat.cosmetic.KillCosmeticService.KillEffect;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Auswahl-GUI für rein kosmetische Kill-Effects. */
public final class KillEffectGui {

    private final GuiManager guiManager;
    private final KillCosmeticService service;

    public KillEffectGui(GuiManager guiManager, KillCosmeticService service) {
        this.guiManager = guiManager;
        this.service = service;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Kill Effects", 27);
        add(gui, player, 10, Material.BARRIER, KillEffect.NONE, ChatColor.GRAY);
        add(gui, player, 12, Material.BLAZE_POWDER, KillEffect.FLAME, ChatColor.GOLD);
        add(gui, player, 13, Material.RED_ROSE, KillEffect.HEART, ChatColor.LIGHT_PURPLE);
        add(gui, player, 14, Material.ENDER_PEARL, KillEffect.ENDER, ChatColor.DARK_PURPLE);
        add(gui, player, 16, Material.NETHER_STAR, KillEffect.LIGHTNING, ChatColor.AQUA);
        guiManager.open(gui);
    }

    private void add(GuiSession gui, Player player, int slot, Material material, KillEffect effect, ChatColor color) {
        boolean unlocked = service.canUse(player, effect);
        boolean selected = service.getSelected(player.getUniqueId()) == effect;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + effect.getDisplayName());
        meta.setLore(Arrays.asList(
                selected ? ChatColor.GREEN + "Ausgewählt" : unlocked ? ChatColor.YELLOW + "Klicken zum Auswählen" : ChatColor.RED + "Noch nicht freigeschaltet",
                effect == KillEffect.NONE ? ChatColor.GRAY + "Deaktiviert deinen Kill-Effect." : ChatColor.DARK_GRAY + "Nur kosmetisch • kein Vorteil"
        ));
        item.setItemMeta(meta);
        gui.setItem(slot, item, (p, e, s) -> {
            if (!service.select(p, effect)) {
                p.sendMessage(ChatColor.RED + "Diesen Kill-Effect hast du noch nicht freigeschaltet.");
                return;
            }
            p.sendMessage(ChatColor.GOLD + "Kill-Effect: " + ChatColor.WHITE + effect.getDisplayName());
            open(p);
        });
    }
}
