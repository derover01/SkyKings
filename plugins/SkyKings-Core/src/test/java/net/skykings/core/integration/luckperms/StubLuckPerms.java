package net.skykings.core.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.actionlog.ActionLogger;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.metastacking.MetaStackFactory;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.node.matcher.NodeMatcherFactory;
import net.luckperms.api.platform.Platform;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.platform.PluginMetadata;
import net.luckperms.api.query.QueryOptionsRegistry;
import net.luckperms.api.track.TrackManager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Handgeschriebenes Test-Double fuer {@link LuckPerms}, statt es per Mockito zu mocken.
 *
 * <p>Grund: {@code mock(LuckPerms.class)} schlaegt auf diesem JDK 8 Build zuverlaessig mit
 * einer {@code NullPointerException} in {@code sun.reflect.annotation.TypeAnnotationParser}
 * fehl - ein bekannter JDK-8-Reflection-Bug, den ByteBuddy beim Erzeugen der Mock-Unterklasse
 * ausloest, sobald es die Typ-Annotationen ALLER Interface-Methoden einliest (LuckPerms hat
 * mehrere generische, typannotierte Methoden). Nur die beiden von {@link
 * LuckPermsPermissionBridge} tatsaechlich genutzten Methoden sind hier sinnvoll implementiert;
 * alles andere wirft bewusst {@link UnsupportedOperationException}, da es in den Tests nicht
 * gebraucht wird.
 */
final class StubLuckPerms implements LuckPerms {

    private final UserManager userManager;
    private final GroupManager groupManager;

    StubLuckPerms(UserManager userManager, GroupManager groupManager) {
        this.userManager = userManager;
        this.groupManager = groupManager;
    }

    @Override
    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public GroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    public String getServerName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TrackManager getTrackManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> PlayerAdapter<T> getPlayerAdapter(Class<T> playerClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Platform getPlatform() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PluginMetadata getPluginMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventBus getEventBus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MessagingService> getMessagingService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActionLogger getActionLogger() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContextManager getContextManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetaStackFactory getMetaStackFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> runUpdateTask() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerMessengerProvider(MessengerProvider messengerProvider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeBuilderRegistry getNodeBuilderRegistry() {
        return new StubNodeBuilderRegistry();
    }

    @Override
    public QueryOptionsRegistry getQueryOptionsRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeMatcherFactory getNodeMatcherFactory() {
        throw new UnsupportedOperationException();
    }
}
