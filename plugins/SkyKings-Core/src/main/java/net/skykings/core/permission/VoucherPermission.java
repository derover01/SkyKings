package net.skykings.core.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Unveraenderliche Beschreibung eines per Gutschein/Admin freigegebenen Rechts. */
public final class VoucherPermission {

    private final String id;
    private final String node;
    private final String displayName;
    private final Set<String> aliases;

    public VoucherPermission(String id, String node, String displayName, Set<String> aliases) {
        this.id = id;
        this.node = node;
        this.displayName = displayName;
        Set<String> normalized = new HashSet<String>();
        normalized.add(id.toLowerCase(Locale.ROOT));
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null && !alias.trim().isEmpty()) {
                    normalized.add(alias.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.aliases = Collections.unmodifiableSet(normalized);
    }

    public String getId() { return id; }
    public String getNode() { return node; }
    public String getDisplayName() { return displayName; }
    public Set<String> getAliases() { return aliases; }

    public boolean matches(String input) {
        return input != null && aliases.contains(input.trim().toLowerCase(Locale.ROOT));
    }
}
