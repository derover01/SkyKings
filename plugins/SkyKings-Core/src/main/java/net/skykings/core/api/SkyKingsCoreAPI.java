package net.skykings.core.api;

import net.skykings.core.clan.ClanService;
import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.kit.KitGrantService;
import net.skykings.core.kit.KitRegistry;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.netherstar.NetherstarService;
import net.skykings.core.permission.VoucherPermissionService;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.rank.RankService;
import net.skykings.core.shop.ShopTransactionService;

/** Zugriffspunkt fuer andere SkyKings-Module (Combat, Crates, Admin). */
public interface SkyKingsCoreAPI {

    PlayerProfileService getPlayerProfileService();

    RankService getRankService();

    EconomyService getEconomyService();

    NetherstarService getNetherstarService();

    CooldownService getCooldownService();

    LoggingService getLoggingService();

    KitRegistry getKitRegistry();

    KitGrantService getKitGrantService();

    GuiManager getGuiManager();

    VoucherPermissionService getVoucherPermissionService();

    ShopTransactionService getShopTransactionService();

    ClanService getClanService();
}
