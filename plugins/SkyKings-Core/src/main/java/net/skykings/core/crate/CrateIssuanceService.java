package net.skykings.core.crate;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/** Modulneutrale Bridge: andere SkyKings-Module koennen echte server-issued Crates anfordern. */
public interface CrateIssuanceService {
    ItemStack create(String crateId, int amount);
    String displayName(String crateId);
    Collection<String> ids();
}
