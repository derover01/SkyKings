package net.skykings.core.integration.luckperms;

import net.skykings.core.model.Rank;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Exaktes SkyKings-Rang -&gt; LuckPerms-Gruppenname-Mapping (siehe Auftrag Phase 1B).
 *
 * <p>Bewusst in einer eigenen Klasse ohne jeden Bezug zur LuckPerms-API: dieses Mapping ist
 * reine Konfiguration und muss unabhaengig davon testbar sein, ob LuckPerms auf dem
 * Klassenpfad verfuegbar ist. Enthaelt bewusst nur Free-/Paid-Raenge, keine Teamraenge und
 * keine automatisch erfundenen Zusatzgruppen.
 */
public final class RankGroupMapping {

    private static final Map<Rank, String> RANK_TO_GROUP = createMapping();

    private RankGroupMapping() {
    }

    /** LuckPerms-Gruppenname fuer einen Rang, oder {@code null} falls kein Mapping existiert. */
    public static String groupNameFor(Rank rank) {
        return RANK_TO_GROUP.get(rank);
    }

    /** Alle von SkyKings verwalteten LuckPerms-Gruppennamen (fuer das Aufraeumen alter Rang-Gruppen). */
    public static Collection<String> managedGroupNames() {
        return RANK_TO_GROUP.values();
    }

    private static Map<Rank, String> createMapping() {
        Map<Rank, String> map = new EnumMap<>(Rank.class);
        map.put(Rank.SPIELER, "spieler");
        map.put(Rank.IRON, "iron");
        map.put(Rank.GOLD, "gold");
        map.put(Rank.EPIC, "epic");
        map.put(Rank.DIAMOND, "diamond");
        map.put(Rank.KNIGHT, "knight");
        map.put(Rank.PHOENIX, "phoenix");
        map.put(Rank.ETERNAL, "eternal");
        map.put(Rank.EXILE, "exile");
        map.put(Rank.ENDLING, "endling");
        map.put(Rank.KING, "king");
        return Collections.unmodifiableMap(map);
    }
}
