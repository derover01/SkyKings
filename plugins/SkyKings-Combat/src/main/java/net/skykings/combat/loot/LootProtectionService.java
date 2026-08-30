package net.skykings.combat.loot;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Schuetzt Death-Drops fuer den legitimen Killer (siehe Auftrag Phase 2, Abschnitt 13). Nur
 * tatsaechliche Death-Drops, kein globaler Pickup-Block.
 */
public interface LootProtectionService {

    /** Schuetzt alle Item-Entities, die (naechster Tick) an {@code deathLocation} entstehen, fuer den Killer. */
    void protectDeathDrops(Location deathLocation, UUID killerUuid);

    /** {@code true}, wenn {@code player} das Item aktuell aufheben darf (nicht geschuetzt oder Besitzer oder abgelaufen). */
    boolean canPickup(Item item, Player player);

    /** Entfernt ein Item vollstaendig aus dem Tracking (z. B. bei Despawn/Removal - Memory-Leak-Schutz). */
    void forget(Item item);
}
