package net.skykings.core.ui;

import org.bukkit.Material;

/**
 * Verbindliche Material-Slots fuer den optionalen Minecraft-1.8.9-SkyKings-Pack.
 *
 * 1.8.x kennt kein CustomModelData. Deshalb werden bewusst wenig relevante Vanilla-Items
 * fuer reine UI-Symbole reserviert. PvP-Kernitems (Schwerter, Bow, Rod, Armor, Pearls,
 * Golden Apples) bleiben unangetastet. Ohne Resource Pack bleiben alle Items als klare
 * Vanilla-Fallbacks mit Name/Lore bedienbar.
 */
public enum ResourcePackIcon {
    HOME(Material.MINECART),
    BACK(Material.POWERED_MINECART),
    NEXT(Material.HOPPER_MINECART),
    LOCKED(Material.BARRIER),
    READY(Material.SLIME_BALL),
    COMPLETED(Material.FIREWORK),
    PREMIUM(Material.EYE_OF_ENDER),
    COINS(Material.GOLD_NUGGET),
    STAR(Material.NETHER_STAR),
    BATTLE_PASS(Material.EMPTY_MAP),
    QUESTS(Material.BOOK_AND_QUILL),
    KITS(Material.STORAGE_MINECART),
    CRATES(Material.COMMAND_MINECART),
    JACKPOT(Material.DIODE),
    SHOP(Material.HOPPER),
    TRADE(Material.NAME_TAG),
    CLAN(Material.WRITTEN_BOOK),
    DUEL(Material.SHEARS),
    EVENT(Material.MAGMA_CREAM);

    private final Material material;

    ResourcePackIcon(Material material) {
        this.material = material;
    }

    public Material material() {
        return material;
    }
}
